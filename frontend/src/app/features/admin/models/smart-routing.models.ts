export interface RoutingExecutionLog {
  id?: number;
  strategy: 'FULL_OPTIMIZATION' | 'MANDATORY_ONLY' | 'REFUEL_ONLY' | 'SKIP' | string;
  reason: string;
  shouldOptimize: boolean;
  refuelOnly: boolean;
  trucksCount: number;
  binsSentCount: number;
  mandatoryBinsCount: number;
  missionsCreatedCount: number;
  droppedBinsCount: number;
  matrixSource: string;
  createdAt: string;
}

export interface MandatoryBinInsight {
  binId: number;
  fillLevel: number;
  predictedPriority: number;
  predictedHoursToFull: number;
  mandatory: boolean;
  mandatoryByUrgency: boolean;
  mandatoryByFeedback: boolean;
  postponementCount: number;
  feedbackScore: number;
  reason: string;
}

export interface SmartRoutingDashboardData {
  lastExecution: RoutingExecutionLog | null;
  executionHistory: RoutingExecutionLog[];
  mandatoryInsights: MandatoryBinInsight[];
  feedbackMandatoryBins: MandatoryBinInsight[];
}