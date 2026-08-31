import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { UserRole } from '@prisma/client';
import {
  AuthenticatedPrincipal,
  CurrentUser,
} from '../common/decorators/current-user.decorator';
import { Roles } from '../common/decorators/roles.decorator';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { AuditService } from '../common/audit/audit.service';
import { AdminService } from './admin.service';

@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.ADMIN)
@Controller('admin')
export class AdminController {
  constructor(
    private readonly admin: AdminService,
    private readonly audit: AuditService,
  ) {}

  /**
   * Scoped to the caller's own institution, so an administrator cannot read
   * another institution's figures (Section 34).
   */
  @Get('dashboard')
  dashboard(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.admin.dashboard(user.institutionId, user.profileId);
  }

  /**
   * The audit trail (Section 35). Administrator-only: it names who did what,
   * which is exactly the information a tampering attempt would want.
   */
  @Get('audit')
  async auditLog(@Query('limit') limit?: string) {
    const requested = Number(limit);
    const entries = await this.audit.recent(
      Number.isFinite(requested) && requested > 0 ? requested : DEFAULT_AUDIT_PAGE,
    );
    return { entries };
  }
}

/** Enough to review a day's activity without asking for a page size. */
const DEFAULT_AUDIT_PAGE = 50;
