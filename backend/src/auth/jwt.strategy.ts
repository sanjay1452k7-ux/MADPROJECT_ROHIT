import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { AccountStatus } from '@prisma/client';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { AuthenticatedPrincipal } from '../common/decorators/current-user.decorator';
import { PrismaService } from '../common/prisma/prisma.service';

interface AccessTokenPayload {
  sub: string;
}

/**
 * Verifies the access token and rebuilds the principal from the database.
 *
 * The role is deliberately re-read rather than trusted from the token body, so
 * a suspended account or a changed role takes effect immediately instead of
 * lingering until the token expires.
 */
@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy, 'jwt') {
  constructor(
    config: ConfigService,
    private readonly prisma: PrismaService,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: config.getOrThrow<string>('JWT_ACCESS_SECRET'),
    });
  }

  async validate(payload: AccessTokenPayload): Promise<AuthenticatedPrincipal> {
    const user = await this.prisma.user.findUnique({
      where: { id: payload.sub },
      include: { student: true, teacher: true, administrator: true },
    });

    if (!user || user.status !== AccountStatus.ACTIVE) {
      throw new UnauthorizedException();
    }

    return {
      userId: user.id,
      email: user.email,
      role: user.role,
      institutionId: user.institutionId,
      profileId: user.student?.id ?? user.teacher?.id ?? user.administrator?.id ?? null,
    };
  }
}
