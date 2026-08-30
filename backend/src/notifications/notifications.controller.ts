import { Body, Controller, Delete, Get, Param, Patch, Post, UseGuards } from '@nestjs/common';
import {
  AuthenticatedPrincipal,
  CurrentUser,
} from '../common/decorators/current-user.decorator';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RegisterDeviceDto } from './dto/device-token.dto';
import { NotificationsService } from './notifications.service';

@UseGuards(JwtAuthGuard)
@Controller('notifications')
export class NotificationsController {
  constructor(private readonly notifications: NotificationsService) {}

  @Get()
  list(@CurrentUser() user: AuthenticatedPrincipal) {
    return this.notifications.listFor(user.userId);
  }

  @Get('unread-count')
  async unreadCount(@CurrentUser() user: AuthenticatedPrincipal) {
    return { count: await this.notifications.unreadCount(user.userId) };
  }

  @Patch(':id/read')
  async markRead(
    @CurrentUser() user: AuthenticatedPrincipal,
    @Param('id') id: string,
  ) {
    await this.notifications.markRead(user.userId, id);
    return { ok: true };
  }

  @Patch('read-all')
  async markAllRead(@CurrentUser() user: AuthenticatedPrincipal) {
    await this.notifications.markAllRead(user.userId);
    return { ok: true };
  }

  /**
   * Registers this device for push (Section 32).
   *
   * Accepted and stored today; delivery begins the moment a Firebase project
   * is configured, with no client change.
   */
  @Post('device-token')
  async registerDevice(
    @CurrentUser() user: AuthenticatedPrincipal,
    @Body() dto: RegisterDeviceDto,
  ) {
    await this.notifications.registerDevice(user.userId, dto.token, dto.platform);
    return { pushConfigured: this.notifications.pushConfigured };
  }

  @Delete('device-token/:token')
  async unregisterDevice(@Param('token') token: string) {
    await this.notifications.unregisterDevice(token);
    return { ok: true };
  }
}
