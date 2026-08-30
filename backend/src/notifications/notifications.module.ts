import { Global, Module } from '@nestjs/common';
import { NotificationsController } from './notifications.controller';
import { NotificationsService } from './notifications.service';
import { PUSH_SENDER, UnconfiguredPushSender } from './push-sender';

/**
 * Notifications (Section 32).
 *
 * PUSH_SENDER is the seam for Firebase: bind an FcmPushSender here once
 * google-services.json and a service account exist, and every notification the
 * product already raises starts reaching devices.
 */
@Global()
@Module({
  controllers: [NotificationsController],
  providers: [
    NotificationsService,
    UnconfiguredPushSender,
    { provide: PUSH_SENDER, useExisting: UnconfiguredPushSender },
  ],
  exports: [NotificationsService],
})
export class NotificationsModule {}
