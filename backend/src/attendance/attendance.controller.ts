import { Body, Controller, Get, Param, Post, UseGuards } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { UserRole } from '@prisma/client';
import { ApiError } from '../common/api-error';
import {
  AuthenticatedPrincipal,
  CurrentUser,
} from '../common/decorators/current-user.decorator';
import { Roles } from '../common/decorators/roles.decorator';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Actor, AttendanceService } from './attendance.service';
import {
  ManualAttendanceDto,
  MarkAttendanceDto,
  StartSessionDto,
  VerifyTokenDto,
} from './dto/attendance.dto';

/**
 * The attendance workflow (Section 17).
 *
 * Every route is guarded by role, and each handler additionally checks that the
 * caller owns the session or student it names - a role alone is not enough.
 */
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('attendance-sessions')
export class AttendanceController {
  constructor(private readonly attendance: AttendanceService) {}

  @Roles(UserRole.TEACHER)
  @Post()
  start(@CurrentUser() user: AuthenticatedPrincipal, @Body() dto: StartSessionDto) {
    return this.attendance.startSession(
      this.actorOf(user),
      dto.subjectId,
      dto.classSectionId,
    );
  }

  @Roles(UserRole.TEACHER)
  @Get(':id')
  detail(@CurrentUser() user: AuthenticatedPrincipal, @Param('id') id: string) {
    this.actorOf(user);
    return this.attendance.sessionSummary(id);
  }

  @Roles(UserRole.TEACHER)
  @Post(':id/close')
  close(@CurrentUser() user: AuthenticatedPrincipal, @Param('id') id: string) {
    return this.attendance.closeSession(this.actorOf(user), id);
  }

  /**
   * Mints the student's QR credential (Section 19). The student app calls this
   * on every refresh; it never constructs a token itself.
   */
  // A code lasts 25 seconds, so a student legitimately refreshes at most a few
  // times a minute. This bounds how fast codes can be harvested (Section 34).
  @Throttle({ default: { limit: 20, ttl: 60_000 } })
  @Roles(UserRole.STUDENT)
  @Post(':id/token')
  mint(@CurrentUser() user: AuthenticatedPrincipal, @Param('id') id: string) {
    return this.attendance.mintToken(this.actorOf(user), id);
  }

  /** Scan step: validate and show the student, but record nothing yet. */
  // Scanning is rapid by nature - a queue of students - but not unbounded.
  @Throttle({ default: { limit: 120, ttl: 60_000 } })
  @Roles(UserRole.TEACHER)
  @Post(':id/verify')
  verify(
    @CurrentUser() user: AuthenticatedPrincipal,
    @Param('id') id: string,
    @Body() dto: VerifyTokenDto,
  ) {
    return this.attendance.verifyToken(this.actorOf(user), id, dto.token);
  }

  /** Confirmation step: the teacher has looked at the student (Section 22). */
  @Roles(UserRole.TEACHER)
  @Post(':id/mark')
  mark(
    @CurrentUser() user: AuthenticatedPrincipal,
    @Param('id') id: string,
    @Body() dto: MarkAttendanceDto,
  ) {
    return this.attendance.markAttendance(this.actorOf(user), id, dto.verificationId);
  }

  @Roles(UserRole.TEACHER)
  @Post(':id/manual')
  manual(
    @CurrentUser() user: AuthenticatedPrincipal,
    @Param('id') id: string,
    @Body() dto: ManualAttendanceDto,
  ) {
    return this.attendance.markManually(
      this.actorOf(user),
      id,
      dto.studentId,
      dto.status,
      dto.reason,
    );
  }

  /**
   * A user with a role but no matching profile row cannot act on sessions.
   *
   * Returns both identities: the profile the action is scoped to, and the user
   * account the audit trail records as the actor (Section 35).
   */
  private actorOf(user: AuthenticatedPrincipal): Actor {
    if (!user.profileId) {
      throw ApiError.notAuthorised();
    }
    return { userId: user.userId, profileId: user.profileId };
  }
}
