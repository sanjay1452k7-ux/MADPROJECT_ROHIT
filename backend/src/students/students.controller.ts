import { Controller, Get, UseGuards } from '@nestjs/common';
import { UserRole } from '@prisma/client';
import { AttendanceService } from '../attendance/attendance.service';
import { ApiError } from '../common/api-error';
import {
  AuthenticatedPrincipal,
  CurrentUser,
} from '../common/decorators/current-user.decorator';
import { Roles } from '../common/decorators/roles.decorator';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { StudentsService } from './students.service';

/**
 * Every route resolves the student from the access token rather than from a
 * path parameter, so a student cannot read another student's attendance by
 * changing an id (Section 34).
 */
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.STUDENT)
@Controller('students/me')
export class StudentsController {
  constructor(
    private readonly students: StudentsService,
    private readonly attendance: AttendanceService,
  ) {}

  @Get('dashboard')
  dashboard(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.students.dashboard(this.profileOf(user));
  }

  @Get('profile')
  profile(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.students.profile(this.profileOf(user));
  }

  @Get('attendance')
  history(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.students.history(this.profileOf(user));
  }

  /** Null when no teacher currently has a session open for this cohort. */
  @Get('active-session')
  activeSession(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.attendance.activeSessionForStudent(this.profileOf(user));
  }

  private profileOf(user: AuthenticatedPrincipal): string {
    if (!user.profileId) {
      throw ApiError.notAuthorised();
    }
    return user.profileId;
  }
}
