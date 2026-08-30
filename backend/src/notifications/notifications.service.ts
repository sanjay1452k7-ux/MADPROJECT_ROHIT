import { Inject, Injectable } from '@nestjs/common';
import { NotificationType } from '@prisma/client';
import { PrismaService } from '../common/prisma/prisma.service';
import { PUSH_SENDER, PushSender } from './push-sender';

/**
 * The single place a notification is raised (Section 32).
 *
 * Every notification is written to the database first - that is what the
 * in-app list reads, and it works with or without push - and only then handed
 * to the push transport. Delivery failing must never lose the notification.
 */
@Injectable()
export class NotificationsService {
  constructor(
    private readonly prisma: PrismaService,
    @Inject(PUSH_SENDER) private readonly push: PushSender,
  ) {}

  async notify(params: {
    userId: string;
    type: NotificationType;
    title: string;
    message: string;
  }): Promise<void> {
    await this.prisma.notification.create({ data: params });
    await this.dispatch(params);
  }

  /** Raises the same notification for many users in one write. */
  async notifyMany(
    entries: Array<{
      userId: string;
      type: NotificationType;
      title: string;
      message: string;
    }>,
  ): Promise<void> {
    if (entries.length === 0) {
      return;
    }
    await this.prisma.notification.createMany({ data: entries });
    for (const entry of entries) {
      await this.dispatch(entry);
    }
  }

  async listFor(userId: string) {
    const items = await this.prisma.notification.findMany({
      where: { userId },
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

  /** Scoped by user as well as id, so one user cannot read another's item. */
  async markRead(userId: string, notificationId: string): Promise<void> {
    await this.prisma.notification.updateMany({
      where: { id: notificationId, userId },
      data: { isRead: true },
    });
  }

  async markAllRead(userId: string): Promise<void> {
    await this.prisma.notification.updateMany({
      where: { userId, isRead: false },
      data: { isRead: true },
    });
  }

  async unreadCount(userId: string): Promise<number> {
    return this.prisma.notification.count({ where: { userId, isRead: false } });
  }

  /** Registers a device for push, replacing any earlier owner of the token. */
  async registerDevice(userId: string, token: string, platform: string): Promise<void> {
    await this.prisma.deviceToken.upsert({
      where: { token },
      create: { userId, token, platform },
      update: { userId, platform },
    });
  }

  async unregisterDevice(token: string): Promise<void> {
    await this.prisma.deviceToken.deleteMany({ where: { token } });
  }

  /** Whether push delivery is actually available, for honest reporting. */
  get pushConfigured(): boolean {
    return this.push.isConfigured;
  }

  private async dispatch(entry: {
    userId: string;
    type: NotificationType;
    title: string;
    message: string;
  }): Promise<void> {
    const devices = await this.prisma.deviceToken.findMany({
      where: { userId: entry.userId },
      select: { token: true },
    });
    if (devices.length === 0) {
      return;
    }
    await this.push.send({ ...entry, deviceTokens: devices.map((d) => d.token) });
  }
}
