import { Global, Module } from '@nestjs/common';
import { HeuristicRiskScorer } from './heuristic-risk-scorer';
import { RISK_SCORER } from './risk-scorer';
import { RiskService } from './risk.service';

/**
 * Binds the scoring engine (Section 31).
 *
 * Replacing the heuristic with a remote ML service later is a change to this
 * one provider: nothing that consumes RiskService needs to know which engine
 * answered.
 */
@Global()
@Module({
  providers: [
    RiskService,
    HeuristicRiskScorer,
    { provide: RISK_SCORER, useExisting: HeuristicRiskScorer },
  ],
  exports: [RiskService],
})
export class RiskModule {}
