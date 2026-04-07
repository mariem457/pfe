package com.example.demo.service;

import com.example.demo.dto.routing.MandatoryBinInsightDto;
import com.example.demo.dto.routing.RecommendedFuelStationDto;
import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.dto.routing.RoutingDepotDto;
import com.example.demo.dto.routing.RoutingIncidentDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingTruckDto;
import com.example.demo.entity.Bin;
import com.example.demo.entity.FuelStation;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.PostponedBin;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckIncident;
import com.example.demo.repository.BinTimePredictionRepository;
import com.example.demo.repository.PostponedBinRepository;
import com.example.demo.repository.TruckIncidentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoutingPayloadBuilderServiceImpl implements RoutingPayloadBuilderService {

    private static final double DEFAULT_BIN_MAX_CAPACITY_KG = 50.0;

    private static final double FEEDBACK_SCORE_THRESHOLD = 6.0;

    private static final double UNRESOLVED_POSTPONED_WEIGHT = 2.0;
    private static final double URGENT_HOURS_WEIGHT = 2.5;
    private static final double HIGH_FILL_WEIGHT = 1.5;
    private static final double HIGH_PRIORITY_WEIGHT = 1.5;

    private static final double MANDATORY_PRIORITY_THRESHOLD = 0.98;
    private static final double MANDATORY_FILL_THRESHOLD = 95.0;
    private static final double MANDATORY_HOURS_THRESHOLD = 6.0;

    private static final double OPPORTUNISTIC_PRIORITY_THRESHOLD = 0.85;
    private static final double OPPORTUNISTIC_FILL_THRESHOLD = 75.0;
    private static final double OPPORTUNISTIC_HOURS_THRESHOLD = 24.0;
    private static final double OPPORTUNISTIC_FEEDBACK_THRESHOLD = 3.0;

    private static final int WORKDAY_START_HOUR = 6;
    private static final int WORKDAY_END_HOUR = 14;

    private final BinPriorityService binPriorityService;
    private final TruckIncidentRepository truckIncidentRepository;
    private final FuelManagementService fuelManagementService;
    private final FuelStationService fuelStationService;
    private final BinTimePredictionRepository binTimePredictionRepository;
    private final PostponedBinRepository postponedBinRepository;

    private final List<RecommendedFuelStationDto> lastRecommendedFuelStations = new ArrayList<>();

    public RoutingPayloadBuilderServiceImpl(
            BinPriorityService binPriorityService,
            TruckIncidentRepository truckIncidentRepository,
            FuelManagementService fuelManagementService,
            FuelStationService fuelStationService,
            BinTimePredictionRepository binTimePredictionRepository,
            PostponedBinRepository postponedBinRepository
    ) {
        this.binPriorityService = binPriorityService;
        this.truckIncidentRepository = truckIncidentRepository;
        this.fuelManagementService = fuelManagementService;
        this.fuelStationService = fuelStationService;
        this.binTimePredictionRepository = binTimePredictionRepository;
        this.postponedBinRepository = postponedBinRepository;
    }

    @Override
    public RoutingRequestDto buildRoutingRequest(List<Truck> trucks) {
        return buildRoutingRequest(trucks, RoutingDecision.fullOptimization("Default routing request"));
    }

    @Override
    public RoutingRequestDto buildRoutingRequest(List<Truck> trucks, RoutingDecision decision) {
        lastRecommendedFuelStations.clear();

        RoutingRequestDto request = new RoutingRequestDto();
        request.setDepot(buildDefaultDepot());
        request.setTrafficMode("NORMAL");
        request.setBins(buildRoutingBins(decision));
        request.setTrucks(buildTrucks(trucks));
        request.setActiveIncidents(buildActiveIncidents(trucks));

        System.out.println(
                "Routing payload built | strategy=" + (decision != null ? decision.getStrategy() : null)
                        + " | trucks=" + request.getTrucks().size()
                        + " | bins=" + request.getBins().size()
                        + " | activeIncidents=" + request.getActiveIncidents().size()
                        + " | recommendedFuelStations=" + lastRecommendedFuelStations.size()
        );

        return request;
    }

    @Override
    public RoutingRequestDto buildReplanRequest(List<Truck> trucks, List<MissionBin> remainingMissionBins) {
        lastRecommendedFuelStations.clear();

        RoutingRequestDto request = new RoutingRequestDto();
        request.setDepot(buildDefaultDepot());
        request.setTrafficMode("NORMAL");
        request.setBins(buildRoutingBinsFromMissionBins(remainingMissionBins));
        request.setTrucks(buildTrucks(trucks));
        request.setActiveIncidents(buildActiveIncidents(trucks));

        return request;
    }

    @Override
    public List<RecommendedFuelStationDto> getLastRecommendedFuelStations() {
        return new ArrayList<>(lastRecommendedFuelStations);
    }

    @Override
    public List<MandatoryBinInsightDto> getMandatoryBinInsights() {
        List<RoutingBinDto> bins = binPriorityService.getPriorityBinsForRouting();

        if (bins == null) {
            return new ArrayList<>();
        }

        List<MandatoryBinInsightDto> insights = new ArrayList<>();

        for (RoutingBinDto bin : bins) {
            enrichRoutingBin(bin);
            applyDecisionClassification(bin);

            boolean mandatoryByUrgency = isDecisionReasonUrgency(bin.getDecisionReason());
            boolean mandatoryByFeedback = "MANDATORY_BY_FEEDBACK_SCORE".equals(bin.getDecisionReason());

            insights.add(toMandatoryInsight(
                    bin,
                    Boolean.TRUE.equals(bin.getMandatory()),
                    mandatoryByUrgency,
                    mandatoryByFeedback,
                    bin.getPostponementCount() != null ? bin.getPostponementCount() : 0L,
                    bin.getFeedbackScore() != null ? bin.getFeedbackScore() : 0.0
            ));
        }

        return insights;
    }

    private void enrichRoutingBin(RoutingBinDto bin) {
        double fillLevel = safeDouble(bin.getFillLevel());
        double estimatedLoadKg = computeEstimatedLoadKg(fillLevel, DEFAULT_BIN_MAX_CAPACITY_KG);
        bin.setEstimatedLoadKg(estimatedLoadKg);

        Double predictedHoursToFull = findLatestPredictedHours(bin.getId());
        bin.setPredictedHoursToFull(predictedHoursToFull);
    }

    private void applyDecisionClassification(RoutingBinDto bin) {
        List<PostponedBin> activePostponedBins = findActivePostponements(bin.getId());
        long postponementCount = activePostponedBins.size();
        double feedbackScore = computeFeedbackScore(bin, activePostponedBins);

        double priority = safeDouble(bin.getPredictedPriority());
        double fillLevel = safeDouble(bin.getFillLevel());
        Double predictedHours = bin.getPredictedHoursToFull();

        boolean mandatoryByHighFill = fillLevel >= MANDATORY_FILL_THRESHOLD;
        boolean mandatoryByUrgentHours = predictedHours != null && predictedHours <= MANDATORY_HOURS_THRESHOLD;
        boolean mandatoryByFeedback = feedbackScore >= FEEDBACK_SCORE_THRESHOLD;

        boolean overflowBeforeShiftEnd = willOverflowBeforeEndOfShift(predictedHours);
        boolean overflowBeforeNextShift = willOverflowBeforeNextShift(predictedHours);

        boolean mandatoryByHighPriority =
                priority >= MANDATORY_PRIORITY_THRESHOLD
                        && (
                        fillLevel >= 80.0
                                || (predictedHours != null && predictedHours <= 24.0)
                );

        bin.setFeedbackScore(feedbackScore);
        bin.setPostponementCount(postponementCount);

        if (overflowBeforeShiftEnd) {
            bin.setMandatory(true);
            bin.setDecisionCategory("MANDATORY");
            bin.setDecisionReason("MANDATORY_OVERFLOW_BEFORE_SHIFT_END");
            bin.setOpportunistic(false);
            bin.setReportable(false);
            return;
        }

        if (overflowBeforeNextShift) {
            bin.setMandatory(true);
            bin.setDecisionCategory("MANDATORY");
            bin.setDecisionReason("MANDATORY_OVERFLOW_BEFORE_NEXT_SHIFT");
            bin.setOpportunistic(false);
            bin.setReportable(false);
            return;
        }

        if (mandatoryByUrgentHours) {
            bin.setMandatory(true);
            bin.setDecisionCategory("MANDATORY");
            bin.setDecisionReason("MANDATORY_BY_URGENT_HOURS");
            bin.setOpportunistic(false);
            bin.setReportable(false);
            return;
        }

        if (mandatoryByHighFill) {
            bin.setMandatory(true);
            bin.setDecisionCategory("MANDATORY");
            bin.setDecisionReason("MANDATORY_BY_HIGH_FILL");
            bin.setOpportunistic(false);
            bin.setReportable(false);
            return;
        }

        if (mandatoryByHighPriority) {
            bin.setMandatory(true);
            bin.setDecisionCategory("MANDATORY");
            bin.setDecisionReason("MANDATORY_BY_HIGH_PRIORITY");
            bin.setOpportunistic(false);
            bin.setReportable(false);
            return;
        }

        if (mandatoryByFeedback) {
            bin.setMandatory(true);
            bin.setDecisionCategory("MANDATORY");
            bin.setDecisionReason("MANDATORY_BY_FEEDBACK_SCORE");
            bin.setOpportunistic(false);
            bin.setReportable(false);
            return;
        }

        boolean opportunistic =
                (priority >= OPPORTUNISTIC_PRIORITY_THRESHOLD
                        && (fillLevel >= 60.0 || (predictedHours != null && predictedHours <= 48.0)))
                        || fillLevel >= OPPORTUNISTIC_FILL_THRESHOLD
                        || (predictedHours != null && predictedHours <= OPPORTUNISTIC_HOURS_THRESHOLD)
                        || feedbackScore >= OPPORTUNISTIC_FEEDBACK_THRESHOLD
                        || postponementCount >= 1;

        if (opportunistic) {
            bin.setMandatory(false);
            bin.setDecisionCategory("OPPORTUNISTIC");
            bin.setDecisionReason("OPPORTUNISTIC_BY_MEDIUM_URGENCY");
            bin.setOpportunistic(true);
            bin.setReportable(false);
            return;
        }

        bin.setMandatory(false);
        bin.setDecisionCategory("REPORTABLE");
        bin.setDecisionReason("REPORTABLE_LOW_RISK");
        bin.setOpportunistic(false);
        bin.setReportable(true);
    }

    private boolean isMandatoryByHighPriority(RoutingBinDto bin) {
        double priority = safeDouble(bin.getPredictedPriority());
        double fillLevel = safeDouble(bin.getFillLevel());
        Double predictedHours = bin.getPredictedHoursToFull();

        return priority >= MANDATORY_PRIORITY_THRESHOLD
                && (
                fillLevel >= 80.0
                        || (predictedHours != null && predictedHours <= 24.0)
        );
    }

    private boolean isMandatoryByHighFill(RoutingBinDto bin) {
        return safeDouble(bin.getFillLevel()) >= MANDATORY_FILL_THRESHOLD;
    }

    private boolean isMandatoryByUrgentHours(RoutingBinDto bin) {
        return bin.getPredictedHoursToFull() != null
                && bin.getPredictedHoursToFull() <= MANDATORY_HOURS_THRESHOLD;
    }

    private boolean isDecisionReasonUrgency(String decisionReason) {
        if (decisionReason == null) {
            return false;
        }

        return "MANDATORY_OVERFLOW_BEFORE_SHIFT_END".equals(decisionReason)
                || "MANDATORY_OVERFLOW_BEFORE_NEXT_SHIFT".equals(decisionReason)
                || "MANDATORY_BY_URGENT_HOURS".equals(decisionReason)
                || "MANDATORY_BY_HIGH_FILL".equals(decisionReason);
    }

    private LocalDateTime computePredictedFullAt(Double predictedHoursToFull) {
        if (predictedHoursToFull == null) {
            return null;
        }
        return LocalDateTime.now().plusMinutes((long) (predictedHoursToFull * 60));
    }

    private boolean willOverflowBeforeEndOfShift(Double predictedHoursToFull) {
        LocalDateTime predictedFullAt = computePredictedFullAt(predictedHoursToFull);
        if (predictedFullAt == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfShift = now.withHour(WORKDAY_END_HOUR).withMinute(0).withSecond(0).withNano(0);

        if (now.isAfter(endOfShift)) {
            return false;
        }

        return !predictedFullAt.isAfter(endOfShift);
    }

    private boolean willOverflowBeforeNextShift(Double predictedHoursToFull) {
        LocalDateTime predictedFullAt = computePredictedFullAt(predictedHoursToFull);
        if (predictedFullAt == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextShiftStart = now.plusDays(1)
                .withHour(WORKDAY_START_HOUR)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        return !predictedFullAt.isAfter(nextShiftStart);
    }

    private MandatoryBinInsightDto toMandatoryInsight(
            RoutingBinDto bin,
            boolean mandatory,
            boolean mandatoryByUrgency,
            boolean mandatoryByFeedback,
            long postponementCount,
            double feedbackScore
    ) {
        MandatoryBinInsightDto dto = new MandatoryBinInsightDto();
        dto.setBinId(bin.getId());
        dto.setFillLevel(bin.getFillLevel());
        dto.setPredictedPriority(bin.getPredictedPriority());
        dto.setPredictedHoursToFull(bin.getPredictedHoursToFull());
        dto.setMandatory(mandatory);
        dto.setMandatoryByUrgency(mandatoryByUrgency);
        dto.setMandatoryByFeedback(mandatoryByFeedback);
        dto.setPostponementCount(postponementCount);
        dto.setFeedbackScore(feedbackScore);

        dto.setDecisionCategory(bin.getDecisionCategory());
        dto.setDecisionReason(bin.getDecisionReason());
        dto.setOpportunistic(bin.getOpportunistic());
        dto.setReportable(bin.getReportable());

        dto.setReason(bin.getDecisionReason());

        return dto;
    }

    private RoutingDepotDto buildDefaultDepot() {
        RoutingDepotDto depot = new RoutingDepotDto();
        depot.setLat(35.5070);
        depot.setLng(11.0700);
        return depot;
    }

    private List<RoutingIncidentDto> buildActiveIncidents(List<Truck> trucks) {
        List<RoutingIncidentDto> incidents = new ArrayList<>();
        Set<Long> refuelRequiredTruckIds = new HashSet<>();

        List<TruckIncident> allActiveIncidents = truckIncidentRepository.findByStatusIn(
                List.of(
                        TruckIncident.IncidentStatus.OPEN,
                        TruckIncident.IncidentStatus.IN_PROGRESS
                )
        );

        for (TruckIncident incident : allActiveIncidents) {
            if (incident.getTruck() == null || incident.getTruck().getId() == null) {
                continue;
            }

            RoutingIncidentDto dto = new RoutingIncidentDto();
            dto.setId(incident.getId());
            dto.setTruckId(incident.getTruck().getId());
            dto.setType(incident.getIncidentType() != null ? incident.getIncidentType().name() : null);
            dto.setSeverity(incident.getSeverity() != null ? incident.getSeverity().name() : null);
            dto.setDescription(incident.getDescription());
            incidents.add(dto);
        }

        for (Truck truck : trucks) {
            if (truck == null || truck.getId() == null) {
                continue;
            }

            if (!fuelManagementService.isFuelCritical(truck)) {
                continue;
            }

            if (refuelRequiredTruckIds.contains(truck.getId())) {
                continue;
            }

            RoutingIncidentDto dto = new RoutingIncidentDto();
            dto.setId(-truck.getId());
            dto.setTruckId(truck.getId());
            dto.setType("REFUEL_REQUIRED");
            dto.setSeverity("CRITICAL");
            dto.setDescription("Truck fuel is critical");

            incidents.add(dto);
            refuelRequiredTruckIds.add(truck.getId());

            FuelStation station = fuelStationService.findNearestCompatibleStation(truck);
            if (station != null && station.getId() != null) {
                RecommendedFuelStationDto recommendation = new RecommendedFuelStationDto();
                recommendation.setTruckId(truck.getId());
                recommendation.setStationId(station.getId());
                recommendation.setStationName(station.getName());
                recommendation.setLat(station.getLat());
                recommendation.setLng(station.getLng());

                lastRecommendedFuelStations.add(recommendation);
            }
        }

        return incidents;
    }

    private List<RoutingBinDto> buildRoutingBins(RoutingDecision decision) {
        List<RoutingBinDto> bins = binPriorityService.getPriorityBinsForRouting();

        if (bins == null) {
            return new ArrayList<>();
        }

        for (RoutingBinDto bin : bins) {
            enrichRoutingBin(bin);
            applyDecisionClassification(bin);
        }

        long mandatoryCount = bins.stream()
                .filter(b -> "MANDATORY".equals(b.getDecisionCategory()))
                .count();

        long opportunisticCount = bins.stream()
                .filter(b -> "OPPORTUNISTIC".equals(b.getDecisionCategory()))
                .count();

        long reportableCount = bins.stream()
                .filter(b -> "REPORTABLE".equals(b.getDecisionCategory()))
                .count();

        System.out.println(
                "All candidate bins=" + bins.size()
                        + ", mandatory=" + mandatoryCount
                        + ", opportunistic=" + opportunisticCount
                        + ", reportable=" + reportableCount
        );

        if (decision != null && decision.getStrategy() == RoutingDecision.Strategy.MANDATORY_ONLY) {
            return bins.stream()
                    .filter(b -> "MANDATORY".equals(b.getDecisionCategory()))
                    .toList();
        }

        return bins;
    }

    private List<PostponedBin> findActivePostponements(Long binId) {
        if (binId == null) {
            return List.of();
        }
        return postponedBinRepository.findByBinIdAndResolvedFalseOrderByCreatedAtDesc(binId);
    }

    private double computeFeedbackScore(RoutingBinDto bin, List<PostponedBin> activePostponedBins) {
        double score = 0.0;

        for (PostponedBin postponedBin : activePostponedBins) {
            score += UNRESOLVED_POSTPONED_WEIGHT * computeAgeDecayFactor(postponedBin.getCreatedAt());
        }

        if (bin.getPredictedHoursToFull() != null && bin.getPredictedHoursToFull() <= MANDATORY_HOURS_THRESHOLD) {
            score += URGENT_HOURS_WEIGHT;
        }

        if (safeDouble(bin.getFillLevel()) >= 90.0) {
            score += HIGH_FILL_WEIGHT;
        }

        if (safeDouble(bin.getPredictedPriority()) >= 0.90) {
            score += HIGH_PRIORITY_WEIGHT;
        }

        return score;
    }

    private double computeAgeDecayFactor(Instant createdAt) {
        if (createdAt == null) {
            return 1.0;
        }

        long ageDays = Duration.between(createdAt, Instant.now()).toDays();

        if (ageDays <= 2) {
            return 1.0;
        }

        if (ageDays <= 7) {
            return 0.7;
        }

        return 0.4;
    }

    private List<RoutingBinDto> buildRoutingBinsFromMissionBins(List<MissionBin> missionBins) {
        List<RoutingBinDto> bins = new ArrayList<>();

        if (missionBins == null || missionBins.isEmpty()) {
            return bins;
        }

        for (MissionBin missionBin : missionBins) {
            if (missionBin == null || missionBin.getBin() == null) {
                continue;
            }

            Bin bin = missionBin.getBin();

            RoutingBinDto dto = new RoutingBinDto();
            dto.setId(bin.getId());
            dto.setLat(resolveBinLat(bin));
            dto.setLng(resolveBinLng(bin));

            double fillLevel = missionBin.getTargetFillThreshold() != null
                    ? missionBin.getTargetFillThreshold().doubleValue()
                    : 80.0;

            dto.setFillLevel(fillLevel);
            dto.setPredictedPriority(1.0);
            dto.setEstimatedLoadKg(computeEstimatedLoadKg(fillLevel, DEFAULT_BIN_MAX_CAPACITY_KG));
            dto.setPredictedHoursToFull(0.0);
            dto.setMandatory(true);
            dto.setDecisionCategory("MANDATORY");
            dto.setDecisionReason("REPLAN_REMAINING_BIN");
            dto.setFeedbackScore(0.0);
            dto.setPostponementCount(0L);
            dto.setOpportunistic(false);
            dto.setReportable(false);

            bins.add(dto);
        }

        return bins;
    }

    private Double findLatestPredictedHours(Long binId) {
        if (binId == null) {
            return null;
        }

        return binTimePredictionRepository
                .findTopByBinIdOrderByCreatedAtDesc(binId)
                .map(pred -> pred.getPredictedHours() != null ? pred.getPredictedHours().doubleValue() : null)
                .orElse(null);
    }

    private boolean isMandatoryBin(RoutingBinDto bin, Double predictedHoursToFull) {
        double priority = safeDouble(bin.getPredictedPriority());
        double fillLevel = safeDouble(bin.getFillLevel());

        if (fillLevel >= MANDATORY_FILL_THRESHOLD) {
            return true;
        }

        if (predictedHoursToFull != null && predictedHoursToFull <= MANDATORY_HOURS_THRESHOLD) {
            return true;
        }

        return priority >= MANDATORY_PRIORITY_THRESHOLD
                && (
                fillLevel >= 80.0
                        || (predictedHoursToFull != null && predictedHoursToFull <= 24.0)
        );
    }

    private List<RoutingTruckDto> buildTrucks(List<Truck> trucks) {
        return trucks.stream().map(this::mapTruckToRoutingTruckDto).toList();
    }

    private RoutingTruckDto mapTruckToRoutingTruckDto(Truck truck) {
        RoutingTruckDto dto = new RoutingTruckDto();

        dto.setId(truck.getId());
        dto.setLat(truck.getLastKnownLat());
        dto.setLng(truck.getLastKnownLng());
        dto.setRemainingCapacityKg(calculateRemainingCapacity(truck));
        dto.setFuelLevelLiters(toDouble(truck.getFuelLevelLiters()));
        dto.setFuelConsumptionPerKm(toDouble(truck.getFuelConsumptionPerKm()));
        dto.setStatus(truck.getStatus() != null ? truck.getStatus().name() : null);

        return dto;
    }

    private Double calculateRemainingCapacity(Truck truck) {
        BigDecimal maxLoad = truck.getMaxLoadKg() != null ? truck.getMaxLoadKg() : BigDecimal.ZERO;
        BigDecimal currentLoad = truck.getCurrentLoadKg() != null ? truck.getCurrentLoadKg() : BigDecimal.ZERO;
        return maxLoad.subtract(currentLoad).doubleValue();
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private double computeEstimatedLoadKg(double fillLevel, double maxCapacityKg) {
        double normalized = Math.max(0.0, Math.min(fillLevel, 100.0));
        double estimated = (normalized / 100.0) * maxCapacityKg;
        return Math.max(1.0, Math.round(estimated));
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private double resolveBinLat(Bin bin) {
        return bin.getAccessLat() != null ? bin.getAccessLat() : bin.getLat();
    }

    private double resolveBinLng(Bin bin) {
        return bin.getAccessLng() != null ? bin.getAccessLng() : bin.getLng();
    }
}