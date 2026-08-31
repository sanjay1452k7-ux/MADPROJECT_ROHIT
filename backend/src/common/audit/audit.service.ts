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

/** One audit row, resolved for display. */
export interface AuditEntry {
  id: string;
  action: string;
  actor: string;
  actorRole: string | null;
  entityType: string;
  entityId: string | null;
  metadata: unknown;
  createdAt: string;
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

  /**
   * The most recent entries, newest first.
   *
   * A trail nobody can read is not an accountability measure, so this exists to
   * be inspected - but only by an administrator (Section 35). The actor is
   * resolved to a name here rather than exposing the raw account id.
   */
  async recent(limit: number): Promise<AuditEntry[]> {
    const capped = Math.min(Math.max(limit, 1), MAX_AUDIT_PAGE);

    const rows = await this.prisma.auditLog.findMany({
      orderBy: { createdAt: 'desc' },
      take: capped,
      include: {
        actor: { select: { role: true, displayName: true } },
      },
    });

    return rows.map((row) => ({
      id: row.id,
      action: row.action,
      // A deleted account leaves its trail behind; the row stays readable.
      actor: row.actor?.displayName ?? 'Deleted account',
      actorRole: row.actor?.role ?? null,
      entityType: row.entityType,
      entityId: row.entityId,
      metadata: row.metadata,
      createdAt: row.createdAt.toISOString(),
    }));
  }
}

/** Bounds one request, so the trail cannot be drained in a single call. */
const MAX_AUDIT_PAGE = 200;
