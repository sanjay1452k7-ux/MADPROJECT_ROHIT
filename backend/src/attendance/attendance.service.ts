import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  AttendanceMethod,
  AttendanceStatus,
  NotificationType,
  Prisma,
  SessionStatus,
} from '@prisma/client';
import { createHash, randomBytes } from 'node:crypto';
import { ApiError } from '../common/api-error';
import { AuditAction, AuditService } from '../common/audit/audit.service';
import { PrismaService } from '../common/prisma/prisma.service';

/** What the student app renders as a QR code. */
export interface MintedTokenDto {
  token: string;
  expiresAt: string;
  validForSeconds: number;
}

/** The identity card the teacher checks against the person in front of them. */
export interface VerifiedStudentDto {
  verificationId: string;
  student: {
    id: string;
    name: string;
    registerNumber: string;
    department: string;
    year: number;
    section: string;
    avatarSeed: string;
    attendancePercentage: number;
  };
  session: {
    id: string;
    subjectName: string;
    subjectCode: string;
  };
}

@Injectable()
export class AttendanceService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
    private readonly audit: AuditService,
  ) {}

  private get tokenTtlSeconds(): number {
    return Number(this.config.get<string>('ATTENDANCE_TOKEN_TTL_SECONDS', '25'));
  }

  /**
   * How long the teacher has between scanning a code and confirming the
   * student. The QR itself is already consumed at scan time, so this window
   * only bounds how stale a pending confirmation may get.
   */
  private readonly verificationWindowMs = 3 * 60 * 1000;

  // ---------------------------------------------------------------- sessions

  async startSession(teacherProfileId: string, subjectId: string, classSectionId: string) {
    const teaches = await this.prisma.teacherSubject.findUnique({
      where: { teacherId_subjectId: { teacherId: teacherProfileId, subjectId } },
    });
    if (!teaches) {
      throw ApiError.notAuthorised();
    }

    // Reuse an already-open session rather than creating a second one for the
    // same class: two concurrent sessions would split the attendance record.
    const existing = await this.prisma.attendanceSession.findFirst({
      where: { classSectionId, subjectId, status: SessionStatus.ACTIVE },
    });
    if (existing) {
      return this.sessionSummary(existing.id);
    }

    const session = await this.prisma.attendanceSession.create({
      data: { teacherId: teacherProfileId, subjectId, classSectionId },
    });

    await this.audit.record({
      actorId: null,
      action: AuditAction.SESSION_STARTED,
      entityType: 'AttendanceSession',
      entityId: session.id,
      metadata: { subjectId, classSectionId, teacherId: teacherProfileId },
    });

    return this.sessionSummary(session.id);
  }

  async closeSession(teacherProfileId: string, sessionId: string) {
    const session = await this.prisma.attendanceSession.findUnique({ where: { id: sessionId } });
    if (!session) {
      throw ApiError.notFound('That attendance session does not exist.');
    }
    if (session.teacherId !== teacherProfileId) {
      throw ApiError.notAuthorised();
    }
    if (session.status === SessionStatus.CLOSED) {
      return this.sessionSummary(sessionId);
    }

    await this.prisma.attendanceSession.update({
      where: { id: sessionId },
      data: { status: SessionStatus.CLOSED, closedAt: new Date() },
    });

    await this.audit.record({
      actorId: null,
      action: AuditAction.SESSION_CLOSED,
      entityType: 'AttendanceSession',
      entityId: sessionId,
    });

    return this.sessionSummary(sessionId);
  }

  /** The open session for this student's cohort, if a teacher has started one. */
  async activeSessionForStudent(studentProfileId: string) {
    const student = await this.prisma.student.findUnique({
      where: { id: studentProfileId },
      include: { enrollments: true },
    });
    if (!student) {
      throw ApiError.notFound();
    }

    const enrolledSubjectIds = student.enrollments.map((e) => e.subjectId);

    const session = await this.prisma.attendanceSession.findFirst({
      where: {
        classSectionId: student.classSectionId,
        status: SessionStatus.ACTIVE,
        subjectId: { in: enrolledSubjectIds },
      },
      orderBy: { startedAt: 'desc' },
      include: { subject: true, teacher: true },
    });

    if (!session) {
      return null;
    }

    const alreadyMarked = await this.prisma.attendanceRecord.findUnique({
      where: { sessionId_studentId: { sessionId: session.id, studentId: student.id } },
    });

    return {
      id: session.id,
      subjectName: session.subject.name,
      subjectCode: session.subject.code,
      teacherName: session.teacher.name,
      startedAt: session.startedAt.toISOString(),
      alreadyMarked: alreadyMarked !== null,
      markedAt: alreadyMarked?.markedAt.toISOString() ?? null,
    };
  }

  // ------------------------------------------------------------------ tokens

  /**
   * Mints a short-lived QR credential (Section 19).
   *
   * The token is generated here and nowhere else: the client never holds a
   * signing key, so it cannot manufacture a code for a student who is not
   * present. Only the SHA-256 hash is persisted, so reading the table does not
   * yield a scannable code.
   */
  async mintToken(studentProfileId: string, sessionId: string): Promise<MintedTokenDto> {
    const session = await this.prisma.attendanceSession.findUnique({
      where: { id: sessionId },
      include: { classSection: true },
    });
    if (!session) {
      throw ApiError.notFound('That attendance session does not exist.');
    }
    if (session.status !== SessionStatus.ACTIVE) {
      throw ApiError.sessionClosed();
    }

    const student = await this.prisma.student.findUnique({ where: { id: studentProfileId } });
    if (!student) {
      throw ApiError.notFound();
    }
    if (student.classSectionId !== session.classSectionId) {
      throw ApiError.notEnrolled();
    }

    const enrolled = await this.prisma.enrollment.findUnique({
      where: {
        studentId_subjectId: { studentId: student.id, subjectId: session.subjectId },
      },
    });
    if (!enrolled) {
      throw ApiError.notEnrolled();
    }

    const already = await this.prisma.attendanceRecord.findUnique({
      where: { sessionId_studentId: { sessionId, studentId: student.id } },
    });
    if (already) {
      throw ApiError.alreadyMarked({
        markedAt: already.markedAt.toISOString(),
      });
    }

    const token = randomBytes(32).toString('base64url');
    const ttl = this.tokenTtlSeconds;
    const expiresAt = new Date(Date.now() + ttl * 1000);

    await this.prisma.attendanceToken.create({
      data: {
        sessionId,
        studentId: student.id,
        tokenHash: this.hash(token),
        expiresAt,
      },
    });

    await this.audit.record({
      actorId: null,
      action: AuditAction.QR_MINTED,
      entityType: 'AttendanceToken',
      entityId: sessionId,
      metadata: { studentId: student.id },
    });

    return {
      token,
      expiresAt: expiresAt.toISOString(),
      validForSeconds: ttl,
    };
  }

  /**
   * Validates a scanned code and returns the student's identity for visual
   * confirmation (Sections 20 and 22).
   *
   * Scanning consumes the code. Attendance is NOT recorded here - the teacher
   * still has to look at the student and confirm (Section 17).
   */
  async verifyToken(
    teacherProfileId: string,
    sessionId: string,
    rawToken: string,
  ): Promise<VerifiedStudentDto> {
    const session = await this.prisma.attendanceSession.findUnique({
      where: { id: sessionId },
      include: { subject: true },
    });
    if (!session) {
      throw ApiError.notFound('That attendance session does not exist.');
    }
    if (session.teacherId !== teacherProfileId) {
      throw ApiError.notAuthorised();
    }
    if (session.status !== SessionStatus.ACTIVE) {
      throw ApiError.sessionClosed();
    }

    const stored = await this.prisma.attendanceToken.findUnique({
      where: { tokenHash: this.hash(rawToken) },
      include: {
        student: { include: { classSection: { include: { department: true } } } },
      },
    });

    if (!stored) {
      await this.auditRejection(sessionId, 'QR_INVALID');
      throw ApiError.qrInvalid();
    }
    // A code minted for a different session must not work here, even if it is
    // otherwise valid.
    if (stored.sessionId !== sessionId) {
      await this.auditRejection(sessionId, 'WRONG_SESSION');
      throw ApiError.qrInvalid();
    }
    if (stored.usedAt !== null) {
      await this.auditRejection(sessionId, 'QR_ALREADY_USED');
      throw ApiError.qrAlreadyUsed();
    }
    if (stored.expiresAt <= new Date()) {
      await this.auditRejection(sessionId, 'QR_EXPIRED');
      throw ApiError.qrExpired();
    }

    const enrolled = await this.prisma.enrollment.findUnique({
      where: {
        studentId_subjectId: { studentId: stored.studentId, subjectId: session.subjectId },
      },
    });
    if (!enrolled) {
      await this.auditRejection(sessionId, 'NOT_ENROLLED');
      throw ApiError.notEnrolled();
    }

    const existingRecord = await this.prisma.attendanceRecord.findUnique({
      where: { sessionId_studentId: { sessionId, studentId: stored.studentId } },
    });
    if (existingRecord) {
      throw ApiError.alreadyMarked({
        studentName: stored.student.name,
        registerNumber: stored.student.registerNumber,
        markedAt: existingRecord.markedAt.toISOString(),
        subjectName: session.subject.name,
      });
    }

    // Consume the code now: a QR that has been shown to the scanner is spent,
    // whether or not the teacher goes on to confirm.
    await this.prisma.attendanceToken.update({
      where: { id: stored.id },
      data: { usedAt: new Date() },
    });

    await this.audit.record({
      actorId: null,
      action: AuditAction.QR_SCANNED,
      entityType: 'AttendanceToken',
      entityId: stored.id,
      metadata: { sessionId, studentId: stored.studentId },
    });

    const percentage = await this.attendancePercentage(stored.studentId);

    return {
      verificationId: stored.id,
      student: {
        id: stored.student.id,
        name: stored.student.name,
        registerNumber: stored.student.registerNumber,
        department: stored.student.classSection.department.code,
        year: stored.student.classSection.year,
        section: stored.student.classSection.section,
        avatarSeed: stored.student.avatarSeed,
        attendancePercentage: percentage,
      },
      session: {
        id: session.id,
        subjectName: session.subject.name,
        subjectCode: session.subject.code,
      },
    };
  }

  /**
   * Records attendance after the teacher confirms (Section 22).
   *
   * The unique constraint on (session, student) is what actually prevents a
   * duplicate under concurrent taps; the pre-check only produces a nicer
   * message in the common case.
   */
  async markAttendance(teacherProfileId: string, sessionId: string, verificationId: string) {
    const session = await this.prisma.attendanceSession.findUnique({
      where: { id: sessionId },
      include: { subject: true },
    });
    if (!session) {
      throw ApiError.notFound('That attendance session does not exist.');
    }
    if (session.teacherId !== teacherProfileId) {
      throw ApiError.notAuthorised();
    }
    if (session.status !== SessionStatus.ACTIVE) {
      throw ApiError.sessionClosed();
    }

    const ticket = await this.prisma.attendanceToken.findUnique({
      where: { id: verificationId },
      include: { student: true },
    });

    if (!ticket || ticket.sessionId !== sessionId || ticket.usedAt === null) {
      throw ApiError.qrInvalid();
    }
    if (Date.now() - ticket.usedAt.getTime() > this.verificationWindowMs) {
      throw ApiError.qrExpired();
    }

    try {
      const record = await this.prisma.attendanceRecord.create({
        data: {
          sessionId,
          studentId: ticket.studentId,
          subjectId: session.subjectId,
          status: AttendanceStatus.PRESENT,
          method: AttendanceMethod.QR_VERIFIED,
          markedBy: teacherProfileId,
        },
      });

      await this.notifyStudent(
        ticket.studentId,
        NotificationType.ATTENDANCE_MARKED,
        'Attendance marked',
        `You were marked present for ${session.subject.name}.`,
      );

      await this.audit.record({
        actorId: null,
        action: AuditAction.ATTENDANCE_MARKED,
        entityType: 'AttendanceRecord',
        entityId: record.id,
        metadata: { sessionId, studentId: ticket.studentId, method: 'QR_VERIFIED' },
      });

      return {
        recordId: record.id,
        studentName: ticket.student.name,
        registerNumber: ticket.student.registerNumber,
        subjectName: session.subject.name,
        markedAt: record.markedAt.toISOString(),
      };
    } catch (error) {
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === 'P2002'
      ) {
        const existing = await this.prisma.attendanceRecord.findUnique({
          where: { sessionId_studentId: { sessionId, studentId: ticket.studentId } },
        });
        throw ApiError.alreadyMarked({
          studentName: ticket.student.name,
          registerNumber: ticket.student.registerNumber,
          markedAt: existing?.markedAt.toISOString(),
          subjectName: session.subject.name,
        });
      }
      throw error;
    }
  }

  /** Manual fallback, recorded as such so an audit can tell it apart. */
  async markManually(
    teacherProfileId: string,
    sessionId: string,
    studentId: string,
    status: AttendanceStatus,
    reason?: string,
  ) {
    const session = await this.prisma.attendanceSession.findUnique({
      where: { id: sessionId },
      include: { subject: true },
    });
    if (!session) {
      throw ApiError.notFound('That attendance session does not exist.');
    }
    if (session.teacherId !== teacherProfileId) {
      throw ApiError.notAuthorised();
    }
    if (session.status !== SessionStatus.ACTIVE) {
      throw ApiError.sessionClosed();
    }

    const enrolled = await this.prisma.enrollment.findUnique({
      where: { studentId_subjectId: { studentId, subjectId: session.subjectId } },
    });
    if (!enrolled) {
      throw ApiError.notEnrolled();
    }

    try {
      const record = await this.prisma.attendanceRecord.create({
        data: {
          sessionId,
          studentId,
          subjectId: session.subjectId,
          status,
          method: AttendanceMethod.MANUAL,
          markedBy: teacherProfileId,
        },
      });

      await this.audit.record({
        actorId: null,
        action: AuditAction.ATTENDANCE_MANUAL,
        entityType: 'AttendanceRecord',
        entityId: record.id,
        metadata: { sessionId, studentId, status, reason: reason ?? null },
      });

      return { recordId: record.id, markedAt: record.markedAt.toISOString() };
    } catch (error) {
      if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
        throw ApiError.alreadyMarked({});
      }
      throw error;
    }
  }

  /** Roster plus who has been accounted for, for the live session screen. */
  async sessionSummary(sessionId: string) {
    const session = await this.prisma.attendanceSession.findUnique({
      where: { id: sessionId },
      include: {
        subject: true,
        teacher: true,
        classSection: { include: { department: true } },
        records: true,
      },
    });
    if (!session) {
      throw ApiError.notFound('That attendance session does not exist.');
    }

    const roster = await this.prisma.enrollment.findMany({
      where: { subjectId: session.subjectId, classSectionId: session.classSectionId },
      include: { student: true },
      orderBy: { student: { registerNumber: 'asc' } },
    });

    const markedByStudent = new Map(session.records.map((r) => [r.studentId, r]));

    return {
      id: session.id,
      status: session.status,
      subjectName: session.subject.name,
      subjectCode: session.subject.code,
      className: `${session.classSection.department.code} - ${session.classSection.year} Year - ${session.classSection.section}`,
      teacherName: session.teacher.name,
      startedAt: session.startedAt.toISOString(),
      closedAt: session.closedAt?.toISOString() ?? null,
      presentCount: session.records.filter((r) => r.status !== AttendanceStatus.ABSENT).length,
      totalStudents: roster.length,
      students: roster.map((enrollment) => {
        const record = markedByStudent.get(enrollment.studentId);
        return {
          id: enrollment.student.id,
          name: enrollment.student.name,
          registerNumber: enrollment.student.registerNumber,
          avatarSeed: enrollment.student.avatarSeed,
          status: record?.status ?? null,
          markedAt: record?.markedAt.toISOString() ?? null,
        };
      }),
    };
  }

  // ----------------------------------------------------------------- helpers

  /** Overall attendance across every subject the student is enrolled in. */
  async attendancePercentage(studentId: string): Promise<number> {
    const [held, attended] = await Promise.all([
      this.prisma.attendanceRecord.count({ where: { studentId } }),
      this.prisma.attendanceRecord.count({
        where: { studentId, status: { not: AttendanceStatus.ABSENT } },
      }),
    ]);
    if (held === 0) {
      return 0;
    }
    return Math.round((attended / held) * 1000) / 10;
  }

  private async notifyStudent(
    studentId: string,
    type: NotificationType,
    title: string,
    message: string,
  ): Promise<void> {
    const student = await this.prisma.student.findUnique({ where: { id: studentId } });
    if (!student) {
      return;
    }
    await this.prisma.notification.create({
      data: { userId: student.userId, type, title, message },
    });
  }

  private async auditRejection(sessionId: string, reason: string): Promise<void> {
    await this.audit.record({
      actorId: null,
      action: AuditAction.QR_REJECTED,
      entityType: 'AttendanceSession',
      entityId: sessionId,
      metadata: { reason },
    });
  }

  private hash(value: string): string {
    return createHash('sha256').update(value).digest('hex');
  }
}
