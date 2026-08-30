import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AttendanceStatus } from '@prisma/client';
import { ApiError } from '../common/api-error';
import { PrismaService } from '../common/prisma/prisma.service';

/**
 * Student-facing reads.
 *
 * Percentages are computed from recorded attendance rather than stored, so the
 * dashboard can never disagree with the underlying records.
 */
@Injectable()
export class StudentsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
  ) {}

  private get threshold(): number {
    return Number(this.config.get<string>('ATTENDANCE_THRESHOLD_PERCENT', '75'));
  }

  async dashboard(studentId: string) {
    const student = await this.loadStudent(studentId);
    const breakdown = await this.subjectBreakdown(studentId);

    const held = breakdown.reduce((sum, s) => sum + s.held, 0);
    const attended = breakdown.reduce((sum, s) => sum + s.attended, 0);
    const overall = held === 0 ? 0 : Math.round((attended / held) * 1000) / 10;

    const [todaysClasses, recentRecords, unread] = await Promise.all([
      this.todaysClasses(student.classSectionId),
      this.recentRecords(studentId, 6),
      this.prisma.notification.count({ where: { userId: student.userId, isRead: false } }),
    ]);

    return {
      student: this.studentDto(student),
      summary: {
        overallPercentage: overall,
        standing: this.standingFor(overall),
        subjectBreakdown: breakdown,
      },
      todaysClasses,
      recentRecords,
      unreadNotifications: unread,
    };
  }

  async profile(studentId: string) {
    const student = await this.loadStudent(studentId);
    return this.studentDto(student);
  }

  async history(studentId: string) {
    return this.recentRecords(studentId, 100);
  }

  private async loadStudent(studentId: string) {
    const student = await this.prisma.student.findUnique({
      where: { id: studentId },
      include: { classSection: { include: { department: true } }, user: true },
    });
    if (!student) {
      throw ApiError.notFound();
    }
    return student;
  }

  private studentDto(student: Awaited<ReturnType<StudentsService['loadStudent']>>) {
    return {
      id: student.id,
      name: student.name,
      registerNumber: student.registerNumber,
      email: student.user.email,
      department: student.classSection.department.code,
      year: student.classSection.year,
      section: student.classSection.section,
      avatarSeed: student.avatarSeed,
    };
  }

  /** Attended and held counts per enrolled subject. */
  private async subjectBreakdown(studentId: string) {
    const enrollments = await this.prisma.enrollment.findMany({
      where: { studentId },
      include: { subject: true },
      orderBy: { subject: { code: 'asc' } },
    });

    return Promise.all(
      enrollments.map(async (enrollment) => {
        const [held, attended] = await Promise.all([
          this.prisma.attendanceRecord.count({
            where: { studentId, subjectId: enrollment.subjectId },
          }),
          this.prisma.attendanceRecord.count({
            where: {
              studentId,
              subjectId: enrollment.subjectId,
              status: { not: AttendanceStatus.ABSENT },
            },
          }),
        ]);

        return {
          subjectId: enrollment.subject.id,
          subjectCode: enrollment.subject.code,
          subjectName: enrollment.subject.name,
          attended,
          held,
          percentage: held === 0 ? 0 : Math.round((attended / held) * 1000) / 10,
        };
      }),
    );
  }

  private async todaysClasses(classSectionId: string) {
    // java.time.DayOfWeek numbering: Monday is 1, Sunday is 7.
    const jsDay = new Date().getDay();
    const dayOfWeek = jsDay === 0 ? 7 : jsDay;

    const slots = await this.prisma.timetableSlot.findMany({
      where: { classSectionId, dayOfWeek },
      include: { subject: true, teacher: true },
      orderBy: { startTime: 'asc' },
    });

    const openSessions = await this.prisma.attendanceSession.findMany({
      where: { classSectionId, status: 'ACTIVE' },
      select: { subjectId: true },
    });
    const openSubjectIds = new Set(openSessions.map((s) => s.subjectId));

    return slots.map((slot) => ({
      id: slot.id,
      subjectId: slot.subject.id,
      subjectCode: slot.subject.code,
      subjectName: slot.subject.name,
      startTime: slot.startTime,
      endTime: slot.endTime,
      room: slot.room,
      teacherName: slot.teacher.name,
      hasActiveSession: openSubjectIds.has(slot.subjectId),
    }));
  }

  private async recentRecords(studentId: string, take: number) {
    const records = await this.prisma.attendanceRecord.findMany({
      where: { studentId },
      include: { subject: true },
      orderBy: { markedAt: 'desc' },
      take,
    });

    return records.map((record) => ({
      id: record.id,
      date: record.markedAt.toISOString().slice(0, 10),
      subjectCode: record.subject.code,
      subjectName: record.subject.name,
      status: record.status,
      markedAt: record.status === AttendanceStatus.ABSENT ? null : record.markedAt.toISOString(),
    }));
  }

  private standingFor(percentage: number): 'GOOD' | 'WARNING' | 'HIGH_RISK' {
    if (percentage >= this.threshold) {
      return 'GOOD';
    }
    return percentage >= this.threshold - 10 ? 'WARNING' : 'HIGH_RISK';
  }
}
