import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AttendanceStatus } from '@prisma/client';
import { ApiError } from '../common/api-error';
import { AttendanceStatsService, percentageOf } from '../common/attendance-stats.service';
import { formatClassName } from '../common/class-label';
import { PrismaService } from '../common/prisma/prisma.service';
import { RiskService } from '../risk/risk.service';

/** One point on a trend line. */
export interface TrendPoint {
  /** ISO month, e.g. "2026-08". */
  period: string;
  label: string;
  held: number;
  attended: number;
  percentage: number;
}

/** One day on the attendance calendar (Section 28). */
export interface CalendarDay {
  date: string;
  present: number;
  absent: number;
  late: number;
  /** The day's overall standing, so the calendar can be read at a glance. */
  status: 'PRESENT' | 'PARTIAL' | 'ABSENT';
}

@Injectable()
export class AnalyticsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
    private readonly risk: RiskService,
    private readonly stats: AttendanceStatsService,
  ) {}

  private get threshold(): number {
    return Number(this.config.get<string>('ATTENDANCE_THRESHOLD_PERCENT', '75'));
  }

  // ---------------------------------------------------------------- student

  async studentAnalytics(studentId: string) {
    const student = await this.prisma.student.findUnique({
      where: { id: studentId },
      include: { classSection: { include: { department: true } } },
    });
    if (!student) {
      throw ApiError.notFound();
    }

    const records = await this.prisma.attendanceRecord.findMany({
      where: { studentId },
      include: { subject: true },
      orderBy: { markedAt: 'asc' },
    });

    const held = records.length;
    const attended = records.filter((r) => r.status !== AttendanceStatus.ABSENT).length;
    const overall = percentage(attended, held);

    const assessment = await this.risk.assess(studentId);

    return {
      overallPercentage: overall,
      threshold: this.threshold,
      standing: this.standing(overall),
      totals: {
        held,
        present: records.filter((r) => r.status === AttendanceStatus.PRESENT).length,
        late: records.filter((r) => r.status === AttendanceStatus.LATE).length,
        absent: records.filter((r) => r.status === AttendanceStatus.ABSENT).length,
      },
      monthlyTrend: monthlyTrend(records),
      subjectBreakdown: this.subjectBreakdown(records),
      calendar: calendarDays(records),
      risk: assessment,
    };
  }

  // ---------------------------------------------------------------- teacher

  async teacherAnalytics(teacherId: string) {
    const teacher = await this.prisma.teacher.findUnique({
      where: { id: teacherId },
      include: { subjects: { include: { subject: true } } },
    });
    if (!teacher) {
      throw ApiError.notFound();
    }

    const subjectIds = teacher.subjects.map((s) => s.subjectId);

    // Everything below is aggregated by the database rather than by loading the
    // teacher's whole record set into memory.
    const [byStudent, monthly, absentee, subjectTotals, subjects] = await Promise.all([
      this.stats.perStudent({ subjectId: { in: subjectIds } }),
      this.stats.monthlyTrend({ subjectIds }),
      this.stats.dailyAbsences(subjectIds, 14),
      this.stats.perSubject({ subjectId: { in: subjectIds } }),
      this.prisma.subject.findMany({ where: { id: { in: subjectIds } } }),
    ]);

    const held = monthly.reduce((sum, m) => sum + m.held, 0);
    const attended = monthly.reduce((sum, m) => sum + m.attended, 0);

    const studentIds = [...byStudent.keys()];
    const students = await this.prisma.student.findMany({
      where: { id: { in: studentIds } },
    });
    const studentById = new Map(students.map((s) => [s.id, s]));

    // Risk is judged on overall attendance, because that is what the
    // institutional requirement applies to - the same basis the teacher
    // dashboard uses. The subject-scoped figure is reported alongside it rather
    // than instead of it, so the two numbers can never appear to contradict.
    const assessments = await this.risk.assessMany(studentIds);
    const atRiskIds = studentIds.filter(
      (id) => (assessments.get(id)?.currentPercentage ?? 100) < this.threshold,
    );

    return {
      classAverage: percentage(attended, held),
      threshold: this.threshold,
      monthlyTrend: trendPointsFrom(monthly),
      subjectAverages: subjects
        .map((subject) => {
          const totals = subjectTotals.get(subject.id);
          return {
            subjectId: subject.id,
            subjectCode: subject.code,
            subjectName: subject.name,
            percentage: percentageOf(totals),
            held: totals?.held ?? 0,
            attended: totals?.attended ?? 0,
          };
        })
        .sort((a, b) => a.subjectCode.localeCompare(b.subjectCode)),
      // How many students were absent per session date, which is the shape a
      // teacher actually asks about ("was Tuesday unusually empty?").
      absenteeTrend: absentee.map((point) => ({
        date: point.date.toISOString().slice(0, 10),
        absent: point.absent,
        held: point.held,
      })),
      studentsAtRisk: atRiskIds
        .map((id) => {
          const student = studentById.get(id)!;
          const entry = byStudent.get(id)!;
          const assessment = assessments.get(id)!;
          return {
            studentId: id,
            name: student.name,
            registerNumber: student.registerNumber,
            avatarSeed: student.avatarSeed,
            /** Overall attendance: what the requirement is measured against. */
            percentage: assessment.currentPercentage,
            /** The same student, counting only this teacher's subjects. */
            inYourSubjects: percentage(entry.attended, entry.held),
            projectedPercentage: assessment.projectedPercentage,
            band: assessment.band,
            reason: assessment.reason,
          };
        })
        .sort((a, b) => a.percentage - b.percentage),
    };
  }

  // ------------------------------------------------------------------ admin

  async adminAnalytics(institutionId: string) {
    const departments = await this.prisma.department.findMany({
      where: { institutionId },
      include: {
        classSections: { include: { students: true } },
        subjects: true,
      },
    });

    // Aggregated by the database: an institution-wide chart must not require
    // loading every attendance row into the service.
    const [byStudent, monthly] = await Promise.all([
      this.stats.perStudent(),
      this.stats.monthlyTrend(),
    ]);

    const held = monthly.reduce((sum, m) => sum + m.held, 0);
    const attended = monthly.reduce((sum, m) => sum + m.attended, 0);

    const belowThreshold = [...byStudent.entries()]
      .filter(([, e]) => e.held > 0 && percentageOf(e) < this.threshold)
      .map(([id]) => id);

    const departmentStats = departments.map((department) => {
      const studentIds = department.classSections.flatMap((c) => c.students.map((s) => s.id));
      const entries = studentIds
        .map((id) => byStudent.get(id))
        .filter((e): e is { held: number; attended: number } => !!e && e.held > 0);

      return {
        department: department.code,
        studentCount: studentIds.length,
        averageAttendance: average(entries.map((e) => percentage(e.attended, e.held))),
        studentsBelowThreshold: studentIds.filter((id) => belowThreshold.includes(id)).length,
        classSections: department.classSections.map((section) => {
          const sectionEntries = section.students
            .map((s) => byStudent.get(s.id))
            .filter((e): e is { held: number; attended: number } => !!e && e.held > 0);
          return {
            id: section.id,
            name: formatClassName(department.code, section.year, section.section),
            studentCount: section.students.length,
            averageAttendance: average(
              sectionEntries.map((e) => percentage(e.attended, e.held)),
            ),
          };
        }),
      };
    });

    return {
      institutionAverage: percentage(attended, held),
      threshold: this.threshold,
      totalStudents: byStudent.size,
      studentsBelowThreshold: belowThreshold.length,
      monthlyTrend: trendPointsFrom(monthly),
      departmentStats,
    };
  }

  // ---------------------------------------------------------------- helpers

  private subjectBreakdown(
    records: Array<{ subjectId: string; status: AttendanceStatus; subject: { code: string; name: string } }>,
  ) {
    const bySubject = new Map<
      string,
      { code: string; name: string; held: number; attended: number }
    >();

    for (const record of records) {
      const entry = bySubject.get(record.subjectId) ?? {
        code: record.subject.code,
        name: record.subject.name,
        held: 0,
        attended: 0,
      };
      entry.held += 1;
      if (record.status !== AttendanceStatus.ABSENT) {
        entry.attended += 1;
      }
      bySubject.set(record.subjectId, entry);
    }

    return [...bySubject.entries()]
      .map(([subjectId, entry]) => ({
        subjectId,
        subjectCode: entry.code,
        subjectName: entry.name,
        held: entry.held,
        attended: entry.attended,
        percentage: percentage(entry.attended, entry.held),
        belowThreshold: percentage(entry.attended, entry.held) < this.threshold,
      }))
      .sort((a, b) => a.subjectCode.localeCompare(b.subjectCode));
  }

  private standing(value: number): 'GOOD' | 'WARNING' | 'HIGH_RISK' {
    if (value >= this.threshold) {
      return 'GOOD';
    }
    return value >= this.threshold - 10 ? 'WARNING' : 'HIGH_RISK';
  }
}

// ------------------------------------------------------------------ shared

function percentage(attended: number, held: number): number {
  return held === 0 ? 0 : Math.round((attended / held) * 1000) / 10;
}

function average(values: number[]): number {
  if (values.length === 0) {
    return 0;
  }
  return Math.round((values.reduce((sum, v) => sum + v, 0) / values.length) * 10) / 10;
}

const MONTH_LABELS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

/** Formats database-aggregated months for the trend chart. */
function trendPointsFrom(
  months: Array<{ month: Date; held: number; attended: number }>,
): TrendPoint[] {
  return months.map((bucket) => ({
    period: `${bucket.month.getFullYear()}-${String(bucket.month.getMonth() + 1).padStart(2, '0')}`,
    label: `${MONTH_LABELS[bucket.month.getMonth()]} ${bucket.month.getFullYear()}`,
    held: bucket.held,
    attended: bucket.attended,
    percentage: percentage(bucket.attended, bucket.held),
  }));
}

/** Groups records by calendar month, oldest first (Section 28). */
function monthlyTrend(
  records: Array<{ markedAt: Date; status: AttendanceStatus }>,
): TrendPoint[] {
  const buckets = new Map<string, { held: number; attended: number; date: Date }>();

  for (const record of records) {
    const date = record.markedAt;
    const period = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    const bucket = buckets.get(period) ?? { held: 0, attended: 0, date };
    bucket.held += 1;
    if (record.status !== AttendanceStatus.ABSENT) {
      bucket.attended += 1;
    }
    buckets.set(period, bucket);
  }

  return [...buckets.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([period, bucket]) => ({
      period,
      label: `${MONTH_LABELS[bucket.date.getMonth()]} ${bucket.date.getFullYear()}`,
      held: bucket.held,
      attended: bucket.attended,
      percentage: percentage(bucket.attended, bucket.held),
    }));
}

/** One entry per day the student had any session (Section 28). */
function calendarDays(
  records: Array<{ markedAt: Date; status: AttendanceStatus }>,
): CalendarDay[] {
  const buckets = new Map<string, { present: number; absent: number; late: number }>();

  for (const record of records) {
    const date = record.markedAt.toISOString().slice(0, 10);
    const bucket = buckets.get(date) ?? { present: 0, absent: 0, late: 0 };
    if (record.status === AttendanceStatus.PRESENT) {
      bucket.present += 1;
    } else if (record.status === AttendanceStatus.LATE) {
      bucket.late += 1;
    } else {
      bucket.absent += 1;
    }
    buckets.set(date, bucket);
  }

  return [...buckets.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, bucket]) => ({
      date,
      ...bucket,
      status:
        bucket.absent === 0
          ? ('PRESENT' as const)
          : bucket.present + bucket.late === 0
            ? ('ABSENT' as const)
            : ('PARTIAL' as const),
    }));
}
