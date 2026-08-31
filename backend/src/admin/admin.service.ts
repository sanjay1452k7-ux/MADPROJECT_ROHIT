import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AttendanceStatus } from '@prisma/client';
import { AttendanceStatsService, percentageOf } from '../common/attendance-stats.service';
import { PrismaService } from '../common/prisma/prisma.service';

@Injectable()
export class AdminService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
    private readonly stats: AttendanceStatsService,
  ) {}

  private get threshold(): number {
    return Number(this.config.get<string>('ATTENDANCE_THRESHOLD_PERCENT', '75'));
  }

  async dashboard(institutionId: string, administratorId: string | null) {
    const administrator = administratorId
      ? await this.prisma.administrator.findUnique({
          where: { id: administratorId },
          include: { user: true },
        })
      : null;

    const departments = await this.prisma.department.findMany({
      where: { institutionId },
      include: { classSections: { include: { students: true } } },
    });

    const students = departments.flatMap((d) => d.classSections.flatMap((c) => c.students));

    // One grouped query for the whole institution.
    const totals = await this.stats.perStudent({
      studentId: { in: students.map((s) => s.id) },
    });
    const measurements = students.map((student) => {
      const entry = totals.get(student.id);
      return {
        studentId: student.id,
        classSectionId: student.classSectionId,
        held: entry?.held ?? 0,
        percentage: percentageOf(entry),
      };
    });

    const measured = measurements.filter((m) => m.held > 0);
    const average =
      measured.length === 0
        ? 0
        : Math.round(
            (measured.reduce((sum, m) => sum + m.percentage, 0) / measured.length) * 10,
          ) / 10;

    const totalFaculty = await this.prisma.teacher.count({
      where: { department: { institutionId } },
    });

    const startOfDay = new Date();
    startOfDay.setHours(0, 0, 0, 0);

    const presentToday = await this.prisma.attendanceRecord.count({
      where: {
        markedAt: { gte: startOfDay },
        status: { not: AttendanceStatus.ABSENT },
      },
    });

    const expectedToday = await this.prisma.attendanceSession.count({
      where: { startedAt: { gte: startOfDay } },
    });

    const byDepartment = departments.map((department) => {
      const sectionIds = new Set(department.classSections.map((c) => c.id));
      const inDept = measured.filter((m) => sectionIds.has(m.classSectionId));
      const deptAverage =
        inDept.length === 0
          ? 0
          : Math.round(
              (inDept.reduce((sum, m) => sum + m.percentage, 0) / inDept.length) * 10,
            ) / 10;
      return {
        department: department.code,
        studentCount: department.classSections.reduce((n, c) => n + c.students.length, 0),
        averageAttendance: deptAverage,
      };
    });

    return {
      administrator: {
        id: administrator?.id ?? '',
        name: administrator?.name ?? '',
        email: administrator?.user.email ?? '',
        designation: administrator?.designation ?? '',
      },
      totalStudents: students.length,
      totalFaculty,
      presentToday,
      // Sessions run today, which is what "expected" means before a session has
      // been held for every timetabled slot.
      sessionsToday: expectedToday,
      averageAttendance: average,
      studentsAtRisk: measured.filter((m) => m.percentage < this.threshold).length,
      departmentStats: byDepartment,
    };
  }
}
