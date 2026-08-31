import { Inject, Injectable } from '@nestjs/common';
import { AttendanceStatus } from '@prisma/client';
import { PrismaService } from '../common/prisma/prisma.service';
import { RISK_SCORER, RiskAssessment, RiskScorer, RiskSignals } from './risk-scorer';

/**
 * Gathers the signals a scorer needs and delegates the judgement to it.
 *
 * Keeping data collection here and the rules in the scorer is what makes the
 * engine replaceable: a future ML service consumes the same [RiskSignals] and
 * this class does not change.
 */
@Injectable()
export class RiskService {
  constructor(
    private readonly prisma: PrismaService,
    @Inject(RISK_SCORER) private readonly scorer: RiskScorer,
  ) {}

  async assess(studentId: string): Promise<RiskAssessment> {
    return this.scorer.score(await this.signalsFor(studentId));
  }

  /**
   * Assesses a whole group from one query.
   *
   * Scoring a cohort used to issue one query per student, so a teacher's
   * analytics screen cost thirty-one round trips before it could draw
   * anything. The records for everyone are fetched once and grouped in memory
   * instead; the scoring itself is pure and needs no database at all.
   */
  async assessMany(studentIds: string[]): Promise<Map<string, RiskAssessment>> {
    const results = new Map<string, RiskAssessment>();
    if (studentIds.length === 0) {
      return results;
    }

    const records = await this.prisma.attendanceRecord.findMany({
      where: { studentId: { in: studentIds } },
      orderBy: { markedAt: 'desc' },
      select: { studentId: true, status: true, subjectId: true },
    });

    const byStudent = new Map<string, Array<{ status: AttendanceStatus; subjectId: string }>>();
    for (const studentId of studentIds) {
      byStudent.set(studentId, []);
    }
    for (const record of records) {
      byStudent.get(record.studentId)?.push(record);
    }

    for (const studentId of studentIds) {
      results.set(studentId, this.scorer.score(signalsFrom(byStudent.get(studentId) ?? [])));
    }
    return results;
  }

  private async signalsFor(studentId: string): Promise<RiskSignals> {
    // Newest first: the recent window is the head of this list.
    const records = await this.prisma.attendanceRecord.findMany({
      where: { studentId },
      orderBy: { markedAt: 'desc' },
      select: { status: true, subjectId: true },
    });
    return signalsFrom(records);
  }
}

/**
 * Derives the scoring signals from a student's records, newest first.
 *
 * Pure so that both the single and the batched path compute identically - two
 * implementations would eventually disagree and give one student a different
 * band depending on which screen asked.
 */
function signalsFrom(records: Array<{ status: AttendanceStatus; subjectId: string }>): RiskSignals {
  const totalHeld = records.length;
  const totalAttended = records.filter((r) => r.status !== AttendanceStatus.ABSENT).length;

  const recent = records.slice(0, RECENT_WINDOW);
  const prior = records.slice(RECENT_WINDOW, RECENT_WINDOW * 2);

  let consecutiveAbsences = 0;
  for (const record of records) {
    if (record.status === AttendanceStatus.ABSENT) {
      consecutiveAbsences += 1;
    } else {
      break;
    }
  }

  // Per-subject percentages, so a single failing subject is visible even when
  // the overall figure looks healthy.
  const bySubject = new Map<string, { held: number; attended: number }>();
  for (const record of records) {
    const entry = bySubject.get(record.subjectId) ?? { held: 0, attended: 0 };
    entry.held += 1;
    if (record.status !== AttendanceStatus.ABSENT) {
      entry.attended += 1;
    }
    bySubject.set(record.subjectId, entry);
  }

  return {
    currentPercentage: totalHeld === 0 ? 0 : (totalAttended / totalHeld) * 100,
    totalHeld,
    totalAttended,
    recentHeld: recent.length,
    recentAttended: recent.filter((r) => r.status !== AttendanceStatus.ABSENT).length,
    priorHeld: prior.length,
    priorAttended: prior.filter((r) => r.status !== AttendanceStatus.ABSENT).length,
    consecutiveAbsences,
  subjectPercentages: [...bySubject.values()]
    .filter((s) => s.held > 0)
    .map((s) => (s.attended / s.held) * 100),
  };
}

/**
 * How many recent sessions count as "recent". Ten is roughly a fortnight of
 * teaching for one cohort - long enough to be a pattern, short enough to react.
 */
const RECENT_WINDOW = 10;
