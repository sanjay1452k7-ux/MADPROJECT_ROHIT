import { Injectable } from '@nestjs/common';
import { AttendanceStatus, Prisma } from '@prisma/client';
import { PrismaService } from './prisma/prisma.service';

/** Restricts a trend to a set of students or subjects. */
export interface TrendFilter {
  studentIds?: string[];
  subjectIds?: string[];
}

/** One month's totals. */
export interface MonthlyTotals {
  month: Date;
  held: number;
  attended: number;
}

/** Classes held and attended for one subject or one student. */
export interface AttendanceTotals {
  held: number;
  attended: number;
}

/**
 * Aggregates attendance in the database rather than in the application.
 *
 * Every dashboard used to count each student's records with two queries in a
 * loop: thirty-one students meant sixty-two round trips for one screen, and the
 * cost grew with the cohort. Grouping by (student, status) answers the same
 * question in a single query whose cost does not depend on how many students
 * there are.
 */
@Injectable()
export class AttendanceStatsService {
  constructor(private readonly prisma: PrismaService) {}

  /** Totals per student, for whichever records the filter selects. */
  async perStudent(
    where: Prisma.AttendanceRecordWhereInput = {},
  ): Promise<Map<string, AttendanceTotals>> {
    const rows = await this.prisma.attendanceRecord.groupBy({
      by: ['studentId', 'status'],
      where,
      _count: { _all: true },
    });
    return fold(rows, (row) => row.studentId);
  }

  /**
   * Attendance per calendar month, aggregated by the database.
   *
   * Building this in the application meant loading every attendance row just to
   * bucket it by month - the whole table for an institution-wide chart. Postgres
   * can group by month directly, so only one row per month crosses the wire.
   */
  async monthlyTrend(filter: TrendFilter = {}): Promise<MonthlyTotals[]> {
    const conditions: Prisma.Sql[] = [];
    if (filter.studentIds) {
      if (filter.studentIds.length === 0) {
        return [];
      }
      conditions.push(Prisma.sql`student_id IN (${Prisma.join(filter.studentIds)})`);
    }
    if (filter.subjectIds) {
      if (filter.subjectIds.length === 0) {
        return [];
      }
      conditions.push(Prisma.sql`subject_id IN (${Prisma.join(filter.subjectIds)})`);
    }
    const where = conditions.length
      ? Prisma.sql`WHERE ${Prisma.join(conditions, ' AND ')}`
      : Prisma.empty;

    const rows = await this.prisma.$queryRaw<
      Array<{ month: Date; held: bigint; attended: bigint }>
    >(Prisma.sql`
      SELECT date_trunc('month', marked_at) AS month,
             COUNT(*) AS held,
             COUNT(*) FILTER (WHERE status <> 'ABSENT') AS attended
      FROM attendance_records
      ${where}
      GROUP BY 1
      ORDER BY 1
    `);

    return rows.map((row) => ({
      month: row.month,
      held: Number(row.held),
      attended: Number(row.attended),
    }));
  }

  /**
   * Absences per day over the recent window, aggregated by the database.
   *
   * The same reasoning as the monthly trend: one row per day rather than every
   * record for the period.
   */
  async dailyAbsences(
    subjectIds: string[],
    days: number,
  ): Promise<Array<{ date: Date; absent: number; held: number }>> {
    if (subjectIds.length === 0) {
      return [];
    }

    const rows = await this.prisma.$queryRaw<
      Array<{ day: Date; held: bigint; absent: bigint }>
    >(Prisma.sql`
      SELECT date_trunc('day', marked_at) AS day,
             COUNT(*) AS held,
             COUNT(*) FILTER (WHERE status = 'ABSENT') AS absent
      FROM attendance_records
      WHERE subject_id IN (${Prisma.join(subjectIds)})
      GROUP BY 1
      ORDER BY 1 DESC
      LIMIT ${days}
    `);

    // Query ordered newest-first to apply the limit; charts read oldest-first.
    return rows
      .map((row) => ({
        date: row.day,
        held: Number(row.held),
        absent: Number(row.absent),
      }))
      .reverse();
  }

  /** Totals per subject, for whichever records the filter selects. */
  async perSubject(
    where: Prisma.AttendanceRecordWhereInput = {},
  ): Promise<Map<string, AttendanceTotals>> {
    const rows = await this.prisma.attendanceRecord.groupBy({
      by: ['subjectId', 'status'],
      where,
      _count: { _all: true },
    });
    return fold(rows, (row) => row.subjectId);
  }
}

/** Collapses the per-status rows into held/attended pairs. */
function fold<T extends { status: AttendanceStatus; _count: { _all: number } }>(
  rows: T[],
  keyOf: (row: T) => string,
): Map<string, AttendanceTotals> {
  const totals = new Map<string, AttendanceTotals>();

  for (const row of rows) {
    const key = keyOf(row);
    const entry = totals.get(key) ?? { held: 0, attended: 0 };
    entry.held += row._count._all;
    // Late still counts as having turned up.
    if (row.status !== AttendanceStatus.ABSENT) {
      entry.attended += row._count._all;
    }
    totals.set(key, entry);
  }
  return totals;
}

/** Percentage attended, to one decimal place. Shared so rounding is uniform. */
export function percentageOf(totals: AttendanceTotals | undefined): number {
  if (!totals || totals.held === 0) {
    return 0;
  }
  return Math.round((totals.attended / totals.held) * 1000) / 10;
}
