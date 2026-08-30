import { Controller, Get, UseGuards } from '@nestjs/common';
import { UserRole } from '@prisma/client';
import { ApiError } from '../common/api-error';
import {
  AuthenticatedPrincipal,
  CurrentUser,
} from '../common/decorators/current-user.decorator';
import { Roles } from '../common/decorators/roles.decorator';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { AnalyticsService } from './analytics.service';

/**
 * Analytics (Section 28).
 *
 * Each role sees only its own scope, resolved from the access token rather than
 * from a parameter, so a student cannot request the cohort's figures.
 */
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('analytics')
export class AnalyticsController {
  constructor(private readonly analytics: AnalyticsService) {}

  @Roles(UserRole.STUDENT)
  @Get('student')
  student(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.analytics.studentAnalytics(profileOf(user));
  }

  @Roles(UserRole.TEACHER)
  @Get('teacher')
  teacher(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.analytics.teacherAnalytics(profileOf(user));
  }

  @Roles(UserRole.ADMIN)
  @Get('admin')
  admin(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.analytics.adminAnalytics(user.institutionId);
  }
}

function profileOf(user: AuthenticatedPrincipal): string {
  if (!user.profileId) {
    throw ApiError.notAuthorised();
  }
  return user.profileId;
}
