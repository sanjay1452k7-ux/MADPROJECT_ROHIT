import { AttendanceStatus } from '@prisma/client';
import { IsEnum, IsOptional, IsString, MinLength } from 'class-validator';

export class StartSessionDto {
  @IsString()
  @MinLength(1)
  subjectId!: string;

  @IsString()
  @MinLength(1)
  classSectionId!: string;
}

/** The payload the teacher's scanner read out of the QR code. */
export class VerifyTokenDto {
  @IsString()
  @MinLength(1)
  token!: string;
}

/**
 * Confirms attendance after the teacher has physically looked at the student
 * (Section 22). The ticket comes from the preceding verify call.
 */
export class MarkAttendanceDto {
  @IsString()
  @MinLength(1)
  verificationId!: string;
}

/** Manual fallback when a device or camera will not cooperate (Section 37). */
export class ManualAttendanceDto {
  @IsString()
  @MinLength(1)
  studentId!: string;

  @IsEnum(AttendanceStatus)
  status!: AttendanceStatus;

  @IsOptional()
  @IsString()
  reason?: string;
}
