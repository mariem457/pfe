package com.example.demo.service;

import com.example.demo.entity.PostponedBin;
import com.example.demo.entity.RoutingExecutionLog;
import com.example.demo.entity.Truck;
import com.example.demo.repository.PostponedBinRepository;
import com.example.demo.repository.RoutingExecutionLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SmartRoutingDecisionServiceImpl implements SmartRoutingDecisionService {

    private static final int URGENT_POSTPONED_THRESHOLD = 5;
    private static final int LAST_RUN_DROPPED_THRESHOLD = 10;

    private static final double HIGH_FEEDBACK_SCORE_THRESHOLD = 6.0;
    private static final double AVERAGE_FEEDBACK_SCORE_THRESHOLD = 4.5;
    private static final long HIGH_FEEDBACK_BINS_THRESHOLD = 4;

    private final FuelManagementService fuelManagementService;
    private final PostponedBinRepository postponedBinRepository;
    private final RoutingExecutionLogRepository routingExecutionLogRepository;
    private final RoutingFeedbackService routingFeedbackService;

    public SmartRoutingDecisionServiceImpl(
            FuelManagementService fuelManagementService,
            PostponedBinRepository postponedBinRepository,
            RoutingExecutionLogRepository routingExecutionLogRepository,
            RoutingFeedbackService routingFeedbackService
    ) {
        this.fuelManagementService = fuelManagementService;
        this.postponedBinRepository = postponedBinRepository;
        this.routingExecutionLogRepository = routingExecutionLogRepository;
        this.routingFeedbackService = routingFeedbackService;
    }

    @Override
    public RoutingDecision makeDecision(List<Truck> trucks) {
        if (trucks == null || trucks.isEmpty()) {
            return RoutingDecision.skip("No active trucks");
        }

        long fuelCriticalCount = trucks.stream()
                .filter(fuelManagementService::isFuelCritical)
                .count();

        if (fuelCriticalCount == trucks.size()) {
            return RoutingDecision.refuelOnly("All trucks fuel critical");
        }

        Optional<RoutingExecutionLog> lastExecutionOpt =
                routingExecutionLogRepository.findTop1ByOrderByCreatedAtDesc();

        if (lastExecutionOpt.isPresent()) {
            RoutingExecutionLog lastExecution = lastExecutionOpt.get();

            if (lastExecution.getDroppedBinsCount() != null
                    && lastExecution.getDroppedBinsCount() > LAST_RUN_DROPPED_THRESHOLD) {
                return RoutingDecision.mandatoryOnly(
                        "Last run dropped too many bins: " + lastExecution.getDroppedBinsCount()
                );
            }
        }

        List<PostponedBin> unresolvedPostponedBins =
                postponedBinRepository.findByResolvedFalseOrderByCreatedAtDesc();

        long urgentPostponedCount = unresolvedPostponedBins.stream()
                .filter(pb -> pb.getPredictedHoursToFull() != null && pb.getPredictedHoursToFull() <= 6.0)
                .count();

        if (urgentPostponedCount > URGENT_POSTPONED_THRESHOLD) {
            return RoutingDecision.mandatoryOnly("Too many urgent postponed bins");
        }

        double averageFeedbackScore = routingFeedbackService.getAverageFeedbackScore();
        long highFeedbackBinsCount =
                routingFeedbackService.countHighFeedbackScoreBins(HIGH_FEEDBACK_SCORE_THRESHOLD);

        System.out.println("Feedback trend => averageScore=" + averageFeedbackScore
                + ", highFeedbackBinsCount=" + highFeedbackBinsCount);

        if (averageFeedbackScore >= AVERAGE_FEEDBACK_SCORE_THRESHOLD
                || highFeedbackBinsCount >= HIGH_FEEDBACK_BINS_THRESHOLD) {
            return RoutingDecision.mandatoryOnly(
                    "Feedback trend indicates routing pressure (avgScore="
                            + averageFeedbackScore
                            + ", highBins=" + highFeedbackBinsCount + ")"
            );
        }

        return RoutingDecision.fullOptimization("Normal optimization");
    }
}