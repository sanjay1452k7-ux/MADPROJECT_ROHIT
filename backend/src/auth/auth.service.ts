import { HttpStatus, Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { verify } from '@node-rs/argon2';
import { AccountStatus, UserRole } from '@prisma/client';
import { createHash, randomBytes } from 'node:crypto';
import { ApiError, ApiErrorCode } from '../common/api-error';
import { AuditAction, AuditService } from '../common/audit/audit.service';
import { PrismaService } from '../common/prisma/prisma.service';

export interface AuthenticatedUserDto {
  id: string;
  displayName: string;
  email: string;
  role: UserRole;
}

export interface AuthTokensDto {
  accessToken: string;
  refreshToken: string;
  user: AuthenticatedUserDto;
}

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwt: JwtService,
    private readonly config: ConfigService,
    private readonly audit: AuditService,
  ) {}

  /**
   * Verifies credentials and the requested role.
   *
   * An unknown email and a wrong password deliberately produce the same
   * failure, so the endpoint cannot be used to enumerate which accounts exist.
   */
  async signIn(email: string, password: string, requestedRole: UserRole): Promise<AuthTokensDto> {
    const user = await this.prisma.user.findUnique({
      where: { email: email.trim().toLowerCase() },
    });

    if (!user) {
      // Hash a dummy value anyway so a missing account and a wrong password
      // take a similar amount of time to reject.
      await this.wasteTime();
      throw ApiError.invalidCredentials();
    }

    const passwordMatches = await verify(user.passwordHash, password);
    if (!passwordMatches) {
      throw ApiError.invalidCredentials();
    }

    if (user.status !== AccountStatus.ACTIVE) {
      throw ApiError.accountInactive('This account is not active. Contact the academics office.');
    }

    if (user.role !== requestedRole) {
      throw ApiError.roleMismatch(user.role.toLowerCase());
    }

    const tokens = await this.issueTokens(user.id, {
      id: user.id,
      displayName: user.displayName,
      email: user.email,
      role: user.role,
    });

    await this.audit.record({
      actorId: user.id,
      action: AuditAction.SIGN_IN,
      entityType: 'User',
      entityId: user.id,
      metadata: { role: user.role },
    });

    return tokens;
  }

  /**
   * Exchanges a refresh token for a new pair.
   *
   * The presented token is revoked as it is consumed, so a stolen refresh token
   * stops working the moment the legitimate client rotates it.
   */
  async refresh(refreshToken: string): Promise<AuthTokensDto> {
    const tokenHash = this.hash(refreshToken);
    const stored = await this.prisma.refreshToken.findUnique({
      where: { tokenHash },
      include: { user: true },
    });

    if (!stored || stored.revokedAt !== null || stored.expiresAt <= new Date()) {
      throw new ApiError(
        ApiErrorCode.SESSION_EXPIRED,
        'Please sign in again to continue.',
        HttpStatus.UNAUTHORIZED,
      );
    }

    if (stored.user.status !== AccountStatus.ACTIVE) {
      throw ApiError.accountInactive('This account is not active.');
    }

    await this.prisma.refreshToken.update({
      where: { id: stored.id },
      data: { revokedAt: new Date() },
    });

    return this.issueTokens(stored.userId, {
      id: stored.user.id,
      displayName: stored.user.displayName,
      email: stored.user.email,
      role: stored.user.role,
    });
  }

  /** Revokes every refresh token the user holds. */
  async signOut(userId: string): Promise<void> {
    await this.prisma.refreshToken.updateMany({
      where: { userId, revokedAt: null },
      data: { revokedAt: new Date() },
    });

    await this.audit.record({
      actorId: userId,
      action: AuditAction.SIGN_OUT,
      entityType: 'User',
      entityId: userId,
    });
  }

  async currentUser(userId: string): Promise<AuthenticatedUserDto> {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) {
      throw ApiError.notFound();
    }
    return {
      id: user.id,
      displayName: user.displayName,
      email: user.email,
      role: user.role,
    };
  }

  private async issueTokens(userId: string, user: AuthenticatedUserDto): Promise<AuthTokensDto> {
    const accessToken = await this.jwt.signAsync(
      { sub: userId },
      {
        secret: this.config.getOrThrow<string>('JWT_ACCESS_SECRET'),
        expiresIn: parseDurationSeconds(this.config.get<string>('JWT_ACCESS_TTL', '15m')),
      },
    );

    // The refresh token is opaque randomness rather than a JWT: it has to be
    // revocable, and only a stored server-side record can do that.
    const refreshToken = randomBytes(48).toString('base64url');
    const refreshTtlSeconds = parseDurationSeconds(
      this.config.get<string>('JWT_REFRESH_TTL', '30d'),
    );

    await this.prisma.refreshToken.create({
      data: {
        userId,
        tokenHash: this.hash(refreshToken),
        expiresAt: new Date(Date.now() + refreshTtlSeconds * 1000),
      },
    });

    return { accessToken, refreshToken, user };
  }

  /** Only the hash is stored, so a database dump cannot be replayed as a login. */
  private hash(value: string): string {
    return createHash('sha256').update(value).digest('hex');
  }

  private async wasteTime(): Promise<void> {
    await verify(DUMMY_HASH, 'not-the-password').catch(() => false);
  }
}

/**
 * Turns "15m" / "30d" / "3600" into seconds.
 *
 * The JWT library types its own duration strings narrowly, so the value is
 * normalised to a plain number of seconds here rather than cast at the call
 * site. An unrecognised value is a configuration mistake and is refused loudly.
 */
export function parseDurationSeconds(value: string): number {
  const match = /^(\d+)([smhd])?$/.exec(value.trim());
  if (!match) {
    throw new Error('Invalid duration in configuration: ' + value);
  }
  const amount = Number(match[1]);
  const unit = match[2] ?? 's';
  const multipliers: Record<string, number> = { s: 1, m: 60, h: 3600, d: 86400 };
  return amount * multipliers[unit];
}

/**
 * A real Argon2id hash of a value nobody knows, used only to keep the timing of
 * a rejected unknown email close to that of a wrong password.
 */
const DUMMY_HASH =
  '$argon2id$v=19$m=19456,t=2,p=1$c29tZS1zdGF0aWMtc2FsdA$Qh0z6mQhKMRZDPBYVmVFYh8mE0lYqXFq0YoLKGVzMPA';
