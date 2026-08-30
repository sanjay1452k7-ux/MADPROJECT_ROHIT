import { HeuristicRiskScorer } from './heuristic-risk-scorer';
import { RiskSignals } from './risk-scorer';

/**
 * The scoring engine drives what students are told about their standing, so its
 * bands and its projection are pinned down explicitly rather than assumed.
 */
describe('HeuristicRiskScorer', () => {
  const scorer = new HeuristicRiskScorer();

  const signals = (overrides: Partial<RiskSignals> = {}): RiskSignals => ({
    currentPercentage: 90,
    totalHeld: 100,
    totalAttended: 90,
    recentHeld: 10,
    recentAttended: 9,
    priorHeld: 10,
    priorAttended: 9,
    consecutiveAbsences: 0,
    subjectPercentages: [90],
    ...overrides,
  });

  it('never claims to be a machine-learning model', () => {
    const result = scorer.score(signals());

    expect(result.method).toBe('Rule-based projection from recent attendance rate');
    expect(result.method.toLowerCase()).not.toContain('ai');
    expect(result.method.toLowerCase()).not.toContain('machine learning');
  });

  it('rates a steady student above the requirement as low risk', () => {
    const result = scorer.score(signals());

    expect(result.band).toBe('LOW');
    expect(result.currentPercentage).toBe(90);
    expect(result.reason).toBe('Attendance is 90% and steady.');
  });

  it('rates a student below the requirement as at least moderate', () => {
    const result = scorer.score(
      signals({ currentPercentage: 74, totalAttended: 74, recentAttended: 7 }),
    );

    expect(['MODERATE', 'HIGH']).toContain(result.band);
    expect(result.reason).toContain('74%');
  });

  it('rates a student far below the requirement as high risk', () => {
    const result = scorer.score(
      signals({ currentPercentage: 60, totalAttended: 60, recentAttended: 6 }),
    );

    expect(result.band).toBe('HIGH');
    expect(result.reason).toContain('below the 75% requirement');
  });

  it('treats four consecutive absences as high risk even when the average is healthy', () => {
    const result = scorer.score(signals({ consecutiveAbsences: 4 }));

    expect(result.band).toBe('HIGH');
    expect(result.reason).toBe('The last 4 sessions were missed.');
  });

  it('flags a decline even while the running average still passes', () => {
    const result = scorer.score(
      signals({
        currentPercentage: 80,
        totalAttended: 80,
        priorHeld: 10,
        priorAttended: 10,
        recentHeld: 10,
        recentAttended: 8,
      }),
    );

    expect(result.band).toBe('MODERATE');
    expect(result.reason).toBe('Attendance has declined compared with earlier in the term.');
  });

  it('projects downward when the recent rate is worse than the average', () => {
    const result = scorer.score(
      signals({ currentPercentage: 90, totalAttended: 90, recentHeld: 10, recentAttended: 5 }),
    );

    // 90 attended of 100, plus 50% of another 100, gives 140/200 = 70%.
    expect(result.projectedPercentage).toBe(70);
    expect(result.projectedPercentage).toBeLessThan(result.currentPercentage);
  });

  it('projects upward when the recent rate is better than the average', () => {
    const result = scorer.score(
      signals({ currentPercentage: 60, totalAttended: 60, recentHeld: 10, recentAttended: 10 }),
    );

    expect(result.projectedPercentage).toBeGreaterThan(result.currentPercentage);
  });

  it('does not invent a trend when there is no recent data', () => {
    const result = scorer.score(
      signals({ recentHeld: 0, recentAttended: 0, priorHeld: 0, priorAttended: 0 }),
    );

    expect(result.projectedPercentage).toBe(result.currentPercentage);
  });

  it('handles a student with no attendance at all', () => {
    const result = scorer.score(
      signals({
        currentPercentage: 0,
        totalHeld: 0,
        totalAttended: 0,
        recentHeld: 0,
        recentAttended: 0,
        priorHeld: 0,
        priorAttended: 0,
        subjectPercentages: [],
      }),
    );

    expect(result.projectedPercentage).toBe(0);
    expect(result.reason).toBe('No attendance has been recorded yet.');
  });

  it('mentions a single failing subject even when the overall figure is healthy', () => {
    const result = scorer.score(signals({ subjectPercentages: [95, 96, 60] }));

    expect(result.band).toBe('LOW');
    expect(result.reason).toContain('one subject is below the requirement');
  });

  it('keeps wording factual rather than alarmist', () => {
    const worst = scorer.score(
      signals({ currentPercentage: 30, totalAttended: 30, recentAttended: 2 }),
    );

    for (const word of ['fail', 'urgent', 'danger', 'warning!', 'critical']) {
      expect(worst.reason.toLowerCase()).not.toContain(word);
    }
  });
});
