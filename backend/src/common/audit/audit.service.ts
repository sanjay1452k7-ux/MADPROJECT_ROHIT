import { Injectable, Logger } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

/** The actions worth keeping a trail of (Section 35). */
export enum AuditAction {
  SIGN_IN = 'SIGN_IN',
  SIGN_OUT = 'SIGN_OUT',
  SESSION_STARTED = 'SESSION_STARTED',
  SESSION_CLOSED = 'SESSION_CLOSED',
  QR_MINTED = 'QR_MINTED',
  QR_SCANNED = 'QR_SCANNED',
  QR_REJECTED = 'QR_REJECTED',
  ATTENDANCE_MARKED = 'ATTENDANCE_MARKED',
  ATTENDANCE_MANUAL = 'ATTENDANCE_MANUAL',
  REPORT_GENERATED = 'REPORT_GENERATED',
}

/**
 * Append-only audit trail.
 *
 * Writing an audit row must never take down the operation being audited, so
 * failures here are logged and swallowed rather than propagated.
 */
@Injectable()
export class AuditService {
  private readonly logger = new Logger(AuditService.name);

  constructor(private readonly prisma: PrismaService) {}

  async record(params: {
    actorId: string | null;
    action: AuditAction;
    entityType: string;
    entityId?: string | null;
    metadata?: Prisma.InputJsonValue;
  }): Promise<void> {
    try {
      await this.prisma.auditLog.create({
        data: {
          actorId: params.actorId,
          action: params.action,
          entityType: params.entityType,
          entityId: params.entityId ?? null,
          metadata: params.metadata,
        },
      });
    } catch (error) {
      this.logger.error(`Failed to write audit log for ${params.action}`, error as Error);
    }
  }
}
