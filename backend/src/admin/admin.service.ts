import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AttendanceStatus } from '@prisma/client';
import { PrismaService } from '../common/prisma/prisma.service';

@Injectable()
export class AdminService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
  ) {}

  private get threshold(): number {
    return Number(this.config.get<string>('ATTENDANCE_THRESHOLD_PERCENT', '75'));
  }

  async dashboard(institutionId: string) {
    const departments = await this.prisma.department.findMany({
      where: { institutionId },
      include: { classSections: { include: { students: true } } },
    });

    const students = departments.flatMap((d) => d.classSections.flatMap((c) => c.students));

    const measurements = await Promise.all(
      students.map(async (student) => {
        const [held, attended] = await Promise.all([
          this.prisma.attendanceRecord.count({ where: { studentId: student.id } }),
          this.prisma.attendanceRecord.count({
            where: { studentId: student.id, status: { not: AttendanceStatus.ABSENT } },
          }),
        ]);
        return {
          studentId: student.id,
          classSectionId: student.classSectionId,
          held,
          percentage: held === 0 ? 0 : Math.round((attended / held) * 1000) / 10,
        };
      }),
    );

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
