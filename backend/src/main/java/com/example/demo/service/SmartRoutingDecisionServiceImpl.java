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

    private static final int CURRENT_RUN_MANDATORY_THRESHOLD = 5;
    private static final int CURRENT_RUN_OPPORTUNISTIC_THRESHOLD = 8;

    private final FuelManagementService fuelManagementService;
    private final PostponedBinRepository postponedBinRepository;
    private final RoutingExecutionLogRepository routingExecutionLogRepository;
    private final RoutingFeedbackService routingFeedbackService;
    private final RoutingPayloadBuilderService routingPayloadBuilderService;

    public SmartRoutingDecisionServiceImpl(
            FuelManagementService fuelManagementService,
            PostponedBinRepository postponedBinRepository,
            RoutingExecutionLogRepository routingExecutionLogRepository,
            RoutingFeedbackService routingFeedbackService,
            RoutingPayloadBuilderService routingPayloadBuilderService
    ) {
        this.fuelManagementService = fuelManagementService;
        this.postponedBinRepository = postponedBinRepository;
        this.routingExecutionLogRepository = routingExecutionLogRepository;
        this.routingFeedbackService = routingFeedbackService;
        this.routingPayloadBuilderService = routingPayloadBuilderService;
    }

    @Override
    public RoutingDecision makeDecision(List<Truck> trucks) {
        if (trucks == null || trucks.isEmpty()) {
            return RoutingDecision.skip(
                    "Aucune décision de routage n'a été prise car aucun camion actif n'est disponible."
            );
        }

        long fuelCriticalCount = trucks.stream()
                .filter(fuelManagementService::isFuelCritical)
                .count();

        if (fuelCriticalCount == trucks.size()) {
            return RoutingDecision.refuelOnly(
                    "Tous les camions disponibles sont en état critique de carburant. " +
                    "Le système recommande une mission de ravitaillement avant toute optimisation."
            );
        }

        Optional<RoutingExecutionLog> lastExecutionOpt =
                routingExecutionLogRepository.findTop1ByOrderByCreatedAtDesc();

        if (lastExecutionOpt.isPresent()) {
            RoutingExecutionLog lastExecution = lastExecutionOpt.get();

            if (lastExecution.getDroppedBinsCount() != null
                    && lastExecution.getDroppedBinsCount() > LAST_RUN_DROPPED_THRESHOLD) {
                return RoutingDecision.mandatoryOnly(
                        "Le dernier cycle a abandonné un nombre élevé de bacs ("
                                + lastExecution.getDroppedBinsCount()
                                + "). Le système passe donc en mode MANDATORY_ONLY pour sécuriser la collecte critique."
                );
            }
        }

        List<PostponedBin> unresolvedPostponedBins =
                postponedBinRepository.findByResolvedFalseOrderByCreatedAtDesc();

        long urgentPostponedCount = unresolvedPostponedBins.stream()
                .filter(pb -> pb.getPredictedHoursToFull() != null && pb.getPredictedHoursToFull() <= 6.0)
                .count();

        if (urgentPostponedCount > URGENT_POSTPONED_THRESHOLD) {
            return RoutingDecision.mandatoryOnly(
                    "Un nombre important de bacs reportés devient urgent ("
                            + urgentPostponedCount
                            + "). Le système privilégie donc uniquement les bacs obligatoires."
                );
        }

        double averageFeedbackScore = routingFeedbackService.getAverageFeedbackScore();
        long highFeedbackBinsCount =
                routingFeedbackService.countHighFeedbackScoreBins(HIGH_FEEDBACK_SCORE_THRESHOLD);

        System.out.println("Feedback trend => averageScore=" + averageFeedbackScore
                + ", highFeedbackBinsCount=" + highFeedbackBinsCount);

        if (averageFeedbackScore >= AVERAGE_FEEDBACK_SCORE_THRESHOLD
                || highFeedbackBinsCount >= HIGH_FEEDBACK_BINS_THRESHOLD) {
            return RoutingDecision.mandatoryOnly(
                    "La tendance des retours terrain indique une pression opérationnelle " +
                    "(score moyen=" + averageFeedbackScore
                            + ", bacs fortement signalés=" + highFeedbackBinsCount
                            + "). Le système active une stratégie focalisée sur les bacs obligatoires."
            );
        }

        long mandatoryNowCount = routingPayloadBuilderService.getMandatoryBinInsights()
                .stream()
                .filter(dto -> "MANDATORY".equals(dto.getDecisionCategory()))
                .count();

        long opportunisticNowCount = routingPayloadBuilderService.getMandatoryBinInsights()
                .stream()
                .filter(dto -> "OPPORTUNISTIC".equals(dto.getDecisionCategory()))
                .count();

        System.out.println("Run-aware classification => mandatoryNow=" + mandatoryNowCount
                + ", opportunisticNow=" + opportunisticNowCount);

        if (mandatoryNowCount >= CURRENT_RUN_MANDATORY_THRESHOLD) {
            return RoutingDecision.mandatoryOnly(
                    "Le cycle courant contient déjà un volume élevé de bacs obligatoires ("
                            + mandatoryNowCount
                            + "). La décision retenue est donc MANDATORY_ONLY."
            );
        }

        if (mandatoryNowCount > 0 || opportunisticNowCount >= CURRENT_RUN_OPPORTUNISTIC_THRESHOLD) {
            return RoutingDecision.fullOptimization(
                    "Le cycle courant contient des bacs obligatoires ou un volume opportuniste significatif " +
                    "(mandatory=" + mandatoryNowCount
                            + ", opportunistic=" + opportunisticNowCount
                            + "). Une optimisation complète est donc justifiée."
            );
        }

        return RoutingDecision.fullOptimization(
                "Les conditions opérationnelles sont normales. Le système autorise une optimisation complète standard."
        );
    }
}