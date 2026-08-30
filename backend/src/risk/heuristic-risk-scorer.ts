import { Injectable } from '@nestjs/common';
import { RiskAssessment, RiskBand, RiskScorer, RiskSignals } from './risk-scorer';

/**
 * The rule-based scoring engine that ships today (Section 31).
 *
 * The projection is an explicit, inspectable calculation - not a learned model.
 * It assumes the rate of the most recent window continues for the rest of the
 * term, and reports where that would leave the student. The `method` field on
 * the result says so, so nothing in the interface can imply more than the
 * engine actually does.
 */
@Injectable()
export class HeuristicRiskScorer implements RiskScorer {

  score(signals: RiskSignals): RiskAssessment {
    const current = round1(signals.currentPercentage);
    const projected = this.project(signals);
    const band = this.band(signals, projected);

    return {
      currentPercentage: current,
      projectedPercentage: projected,
      band,
      reason: this.explain(signals, band, current, projected),
      method: 'Rule-based projection from recent attendance rate',
    };
  }

  /**
   * Projects the end-of-term percentage.
   *
   * The remaining term is assumed to be the same length as the part already
   * held - a deliberate, stated assumption rather than a hidden constant - and
   * the recent rate is applied to it. With no recent data the projection is
   * simply the current figure, because inventing a trend from nothing would be
   * worse than admitting there is none.
   */
  private project(signals: RiskSignals): number {
    if (signals.totalHeld === 0) {
      return 0;
    }
    if (signals.recentHeld === 0) {
      return round1(signals.currentPercentage);
    }

    const recentRate = signals.recentAttended / signals.recentHeld;
    const assumedRemaining = signals.totalHeld;

    const projectedAttended = signals.totalAttended + recentRate * assumedRemaining;
    const projectedHeld = signals.totalHeld + assumedRemaining;

    return round1((projectedAttended / projectedHeld) * 100);
  }

  private band(signals: RiskSignals, projected: number): RiskBand {
    // Missing several sessions back to back is the strongest single signal
    // that something has changed, whatever the running average still says.
    if (signals.consecutiveAbsences >= 4) {
      return 'HIGH';
    }

    if (projected < HIGH_RISK_CEILING || signals.currentPercentage < HIGH_RISK_CEILING) {
      return 'HIGH';
    }
    if (projected < THRESHOLD || signals.currentPercentage < THRESHOLD) {
      return 'MODERATE';
    }
    if (signals.consecutiveAbsences >= 2 || this.isDeclining(signals)) {
      return 'MODERATE';
    }
    return 'LOW';
  }

  /** True when the recent window is meaningfully worse than the one before. */
  private isDeclining(signals: RiskSignals): boolean {
    if (signals.recentHeld === 0 || signals.priorHeld === 0) {
      return false;
    }
    const recent = signals.recentAttended / signals.recentHeld;
    const prior = signals.priorAttended / signals.priorHeld;
    return prior - recent >= DECLINE_SENSITIVITY;
  }

  /**
   * Factual wording only (Section 30). Each branch names the specific signal
   * that produced the band, so a student is never told they are "at risk"
   * without being told why.
   */
  private explain(
    signals: RiskSignals,
    band: RiskBand,
    current: number,
    projected: number,
  ): string {
    if (signals.totalHeld === 0) {
      return 'No attendance has been recorded yet.';
    }

    if (signals.consecutiveAbsences >= 4) {
      return `The last ${signals.consecutiveAbsences} sessions were missed.`;
    }

    if (band === 'HIGH') {
      return current < THRESHOLD
        ? `Attendance is ${current}%, below the ${THRESHOLD}% requirement.`
        : `Attendance is projected to fall to ${projected}% if the recent pattern continues.`;
    }

    if (band === 'MODERATE') {
      if (current < THRESHOLD) {
        return `Attendance is ${current}%, just below the ${THRESHOLD}% requirement.`;
      }
      if (this.isDeclining(signals)) {
        return 'Attendance has declined compared with earlier in the term.';
      }
      if (signals.consecutiveAbsences >= 2) {
        return `The last ${signals.consecutiveAbsences} sessions were missed.`;
      }
      return `Attendance is projected to fall to ${projected}%, below the ${THRESHOLD}% requirement.`;
    }

    const weakest = signals.subjectPercentages.length
      ? Math.min(...signals.subjectPercentages)
      : current;
    if (weakest < THRESHOLD) {
      return `Overall attendance is ${current}%, though one subject is below the requirement.`;
    }
    return `Attendance is ${current}% and steady.`;
  }
}

/** The institutional requirement. Mirrors ATTENDANCE_THRESHOLD_PERCENT. */
const THRESHOLD = 75;

/** More than ten points below the requirement is treated as high risk. */
const HIGH_RISK_CEILING = THRESHOLD - 10;

/** A fall of ten percentage points between windows counts as a decline. */
const DECLINE_SENSITIVITY = 0.1;

function round1(value: number): number {
  return Math.round(value * 10) / 10;
}
