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
import { TeachersService } from './teachers.service';

@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.TEACHER)
@Controller('teachers/me')
export class TeachersController {
  constructor(private readonly teachers: TeachersService) {}

  @Get('dashboard')
  dashboard(@CurrentUser() user: AuthenticatedPrincipal) {
    if (!user.profileId) {
      throw ApiError.notAuthorised();
    }
    return this.teachers.dashboard(user.profileId);
  }
}
