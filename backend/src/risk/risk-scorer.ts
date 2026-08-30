/**
 * Attendance Risk Scoring (Section 31).
 *
 * This is a deterministic scoring engine, not a machine-learning model, and the
 * product must not claim otherwise. It is defined as a port so that the
 * heuristic implementation can be replaced by a Python ML service later without
 * any caller changing: swap the provider bound to [RISK_SCORER] and the API,
 * the app and the database all stay as they are.
 */

/** Everything the scorer is allowed to look at. */
export interface RiskSignals {
  /** Attendance across the whole term so far, 0-100. */
  currentPercentage: number;
  /** Classes held and attended over the whole term. */
  totalHeld: number;
  totalAttended: number;
  /** The same figures over the most recent window (see RECENT_WINDOW). */
  recentHeld: number;
  recentAttended: number;
  /** The window immediately before the recent one, for trend direction. */
  priorHeld: number;
  priorAttended: number;
  /** Consecutive most-recent sessions missed. */
  consecutiveAbsences: number;
  /** Per-subject percentages, used to spot a single failing subject. */
  subjectPercentages: number[];
}

export type RiskBand = 'LOW' | 'MODERATE' | 'HIGH';

export interface RiskAssessment {
  currentPercentage: number;
  /** Where attendance lands if the recent pattern continues (see below). */
  projectedPercentage: number;
  band: RiskBand;
  /** Plain wording explaining the band. Never alarmist (Section 30). */
  reason: string;
  /** How the engine arrived at this, for transparency in the UI. */
  method: string;
}

/** Injection token for the scoring engine. */
export const RISK_SCORER = 'RISK_SCORER';

export interface RiskScorer {
  score(signals: RiskSignals): RiskAssessment;
}
