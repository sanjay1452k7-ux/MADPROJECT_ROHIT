import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AttendanceStatus, SessionStatus } from '@prisma/client';
import { ApiError } from '../common/api-error';
import { PrismaService } from '../common/prisma/prisma.service';

@Injectable()
export class TeachersService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
  ) {}

  private get threshold(): number {
    return Number(this.config.get<string>('ATTENDANCE_THRESHOLD_PERCENT', '75'));
  }

  async dashboard(teacherId: string) {
    const teacher = await this.prisma.teacher.findUnique({
      where: { id: teacherId },
      include: {
        department: true,
        user: true,
        subjects: { include: { subject: true } },
      },
    });
    if (!teacher) {
      throw ApiError.notFound();
    }

    const subjectIds = teacher.subjects.map((s) => s.subjectId);

    // java.time.DayOfWeek numbering: Monday is 1, Sunday is 7.
    const jsDay = new Date().getDay();
    const dayOfWeek = jsDay === 0 ? 7 : jsDay;

    // Every cohort this teacher teaches, independent of today's timetable: the
    // app needs these to offer a class when starting an attendance session.
    const taughtSlots = await this.prisma.timetableSlot.findMany({
      where: { teacherId },
      include: { classSection: { include: { department: true } } },
    });
    const cohorts = new Map(
      taughtSlots.map((slot) => [
        slot.classSectionId,
        {
          id: slot.classSection.id,
          department: slot.classSection.department.code,
          year: slot.classSection.year,
          section: slot.classSection.section,
          name: [
            slot.classSection.department.code,
            slot.classSection.year + ' Year',
            slot.classSection.section,
          ].join(' - '),
        },
      ]),
    );

    const [slots, activeSessions] = await Promise.all([
      this.prisma.timetableSlot.findMany({
        where: { teacherId, dayOfWeek },
        include: { subject: true, classSection: { include: { department: true } } },
        orderBy: { startTime: 'asc' },
      }),
      this.prisma.attendanceSession.findMany({
        where: { teacherId, status: SessionStatus.ACTIVE },
        include: { subject: true },
      }),
    ]);

    const openSubjectIds = new Set(activeSessions.map((s) => s.subjectId));

    // Students this teacher actually teaches: enrolled in one of their subjects.
    const enrollments = await this.prisma.enrollment.findMany({
      where: { subjectId: { in: subjectIds } },
      include: { student: true },
    });
    const uniqueStudents = new Map(enrollments.map((e) => [e.studentId, e.student]));

    const percentages = await Promise.all(
      [...uniqueStudents.values()].map(async (student) => {
        const [held, attended] = await Promise.all([
          this.prisma.attendanceRecord.count({ where: { studentId: student.id } }),
          this.prisma.attendanceRecord.count({
            where: { studentId: student.id, status: { not: AttendanceStatus.ABSENT } },
          }),
        ]);
        return {
          student,
          held,
          percentage: held === 0 ? 0 : Math.round((attended / held) * 1000) / 10,
        };
      }),
    );

    // A student with no recorded classes is unmeasured, not "at risk".
    const measured = percentages.filter((p) => p.held > 0);
    const average =
      measured.length === 0
        ? 0
        : Math.round(
            (measured.reduce((sum, p) => sum + p.percentage, 0) / measured.length) * 10,
          ) / 10;

    const atRisk = measured
      .filter((p) => p.percentage < this.threshold)
      .sort((a, b) => a.percentage - b.percentage)
      .map((p) => ({
        studentId: p.student.id,
        name: p.student.name,
        registerNumber: p.student.registerNumber,
        avatarSeed: p.student.avatarSeed,
        percentage: p.percentage,
        standing: p.percentage >= this.threshold - 10 ? 'WARNING' : 'HIGH_RISK',
      }));

    return {
      teacher: {
        id: teacher.id,
        name: teacher.name,
        employeeCode: teacher.employeeCode,
        email: teacher.user.email,
        department: teacher.department.code,
        avatarSeed: teacher.avatarSeed,
        subjects: teacher.subjects.map((s) => ({
          id: s.subject.id,
          code: s.subject.code,
          name: s.subject.name,
        })),
      },
      todaysClasses: slots.map((slot) => ({
        id: slot.id,
        subjectId: slot.subject.id,
        subjectCode: slot.subject.code,
        subjectName: slot.subject.name,
        classSectionId: slot.classSectionId,
        className: [
          slot.classSection.department.code,
          slot.classSection.year + ' Year',
          slot.classSection.section,
        ].join(' - '),
        startTime: slot.startTime,
        endTime: slot.endTime,
        room: slot.room,
        hasActiveSession: openSubjectIds.has(slot.subjectId),
      })),
      activeSessions: activeSessions.map((s) => ({
        id: s.id,
        subjectId: s.subjectId,
        subjectName: s.subject.name,
      })),
      classSections: [...cohorts.values()],
      averageAttendance: average,
      studentsAtRisk: atRisk,
    };
  }
}
