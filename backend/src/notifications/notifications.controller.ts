import { Controller, Get, Param, Patch, UseGuards } from '@nestjs/common';
import {
  AuthenticatedPrincipal,
  CurrentUser,
} from '../common/decorators/current-user.decorator';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { PrismaService } from '../common/prisma/prisma.service';

@UseGuards(JwtAuthGuard)
@Controller('notifications')
export class NotificationsController {
  constructor(private readonly prisma: PrismaService) {}

  @Get()
  async list(@CurrentUser() user: AuthenticatedPrincipal) {
    const items = await this.prisma.notification.findMany({
      where: { userId: user.userId },
      orderBy: { createdAt: 'desc' },
      take: 50,
    });
    return items.map((n) => ({
      id: n.id,
      type: n.type,
      title: n.title,
      message: n.message,
      timestamp: n.createdAt.toISOString(),
      isRead: n.isRead,
    }));
  }

  /**
   * Scoped by userId as well as id, so one user cannot mark another user's
   * notification read by guessing an identifier.
   */
  @Patch(':id/read')
  async markRead(@CurrentUser() user: AuthenticatedPrincipal, @Param('id') id: string) {
    await this.prisma.notification.updateMany({
      where: { id, userId: user.userId },
      data: { isRead: true },
    });
    return { ok: true };
  }
}
