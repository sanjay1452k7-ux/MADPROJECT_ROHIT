import { UserRole } from '@prisma/client';
import { IsEmail, IsEnum, IsString, MinLength } from 'class-validator';

export class LoginDto {
  @IsEmail({}, { message: 'Enter a valid email address.' })
  email!: string;

  @IsString()
  @MinLength(1, { message: 'Enter your password.' })
  password!: string;

  /**
   * The role chosen on the role screen. The server confirms the account really
   * holds it; the client is never trusted to decide this (Section 34).
   */
  @IsEnum(UserRole)
  role!: UserRole;
}

export class RefreshDto {
  @IsString()
  @MinLength(1)
  refreshToken!: string;
}
