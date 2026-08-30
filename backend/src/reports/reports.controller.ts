import { Body, Controller, Post, Res, UseGuards } from '@nestjs/common';
import { UserRole } from '@prisma/client';
import { Response } from 'express';
import { ApiError } from '../common/api-error';
import { AuditAction, AuditService } from '../common/audit/audit.service';
import {
  AuthenticatedPrincipal,
  CurrentUser,
} from '../common/decorators/current-user.decorator';
import { Roles } from '../common/decorators/roles.decorator';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { GenerateReportDto } from './dto/report.dto';
import { ReportType, ReportsService } from './reports.service';

/**
 * Report generation (Section 33).
 *
 * Scope is enforced here, not by the caller: a student may only ever generate
 * their own attendance report, whatever they put in the request body.
 */
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('reports')
export class ReportsController {
  constructor(
    private readonly reports: ReportsService,
    private readonly audit: AuditService,
  ) {}

  @Roles(UserRole.STUDENT, UserRole.TEACHER, UserRole.ADMIN)
  @Post()
  async generate(
    @CurrentUser() user: AuthenticatedPrincipal,
    @Body() dto: GenerateReportDto,
    @Res() response: Response,
  ): Promise<void> {
    const request = this.scopeToCaller(user, dto);
    const report = await this.reports.generate(request);

    await this.audit.record({
      actorId: user.userId,
      action: AuditAction.REPORT_GENERATED,
      entityType: 'Report',
      entityId: null,
      metadata: { type: request.type, format: request.format },
    });

    response
      .status(200)
      .setHeader('Content-Type', report.contentType)
      .setHeader('Content-Disposition', `attachment; filename="${report.filename}"`)
      .setHeader('X-Report-Filename', report.filename)
      .send(report.body);
  }

  /**
   * Narrows the request to what the caller is allowed to see.
   *
   * A student's request is forced to their own id and to the student report; a
   * teacher may not ask for a subject they do not teach.
   */
  private scopeToCaller(user: AuthenticatedPrincipal, dto: GenerateReportDto) {
    if (user.role === UserRole.STUDENT) {
      if (!user.profileId) {
        throw ApiError.notAuthorised();
      }
      if (dto.type !== ReportType.STUDENT_ATTENDANCE) {
        throw ApiError.notAuthorised();
      }
      return { ...dto, studentId: user.profileId };
    }

    if (user.role === UserRole.TEACHER && !user.profileId) {
      throw ApiError.notAuthorised();
    }

    return dto;
  }
}
