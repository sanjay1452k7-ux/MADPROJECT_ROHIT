import { Injectable, Logger } from '@nestjs/common';
import { NotificationType } from '@prisma/client';

/** A notification about to be delivered to a device. */
export interface PushPayload {
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  deviceTokens: string[];
}

/** Injection token for the push transport. */
export const PUSH_SENDER = 'PUSH_SENDER';

export interface PushSender {
  /** True when the transport can actually deliver. */
  readonly isConfigured: boolean;
  send(payload: PushPayload): Promise<void>;
}

/**
 * The transport in use while no Firebase project exists (Section 0, Section 32).
 *
 * It deliberately does not pretend to deliver anything. Registration, storage
 * and the in-app notification list all work today; only the final hop to a
 * device is absent. When google-services.json and a service account are
 * supplied, an FcmPushSender is bound to PUSH_SENDER in NotificationsModule and
 * nothing else in the codebase changes.
 */
@Injectable()
export class UnconfiguredPushSender implements PushSender {
  private readonly logger = new Logger(UnconfiguredPushSender.name);

  readonly isConfigured = false;

  async send(payload: PushPayload): Promise<void> {
    this.logger.debug(
      `Push not configured; ${payload.deviceTokens.length} device(s) would have ` +
        `received "${payload.title}" for user ${payload.userId}`,
    );
  }
}
