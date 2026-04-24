package com.example.demo.service;

import com.example.demo.dto.routing.BinDecisionCategory;
import com.example.demo.dto.routing.MandatoryBinInsightDto;
import com.example.demo.dto.routing.RecommendedFuelStationDto;
import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.dto.routing.RoutingDepotDto;
import com.example.demo.dto.routing.RoutingIncidentDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingTruckDto;
import com.example.demo.entity.Bin;
import com.example.demo.entity.Depot;
import com.example.demo.entity.FuelStation;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.PostponedBin;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckIncident;
import com.example.demo.exception.BadRequestException;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTimePredictionRepository;
import com.example.demo.repository.DepotRepository;
import com.example.demo.repository.PostponedBinRepository;
import com.example.demo.repository.TruckIncidentRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final double OPPORTUNISTIC_BUFFER_HOURS = 6.0;

    private static final int MORNING_START_HOUR = 6;
    private static final int MORNING_END_HOUR = 12;
    private static final int EVENING_START_HOUR = 17;
    private static final int EVENING_END_HOUR = 23;

    private final BinPriorityService binPriorityService;
    private final TruckIncidentRepository truckIncidentRepository;
    private final FuelManagementService fuelManagementService;
    private final FuelStationService fuelStationService;
    private final BinTimePredictionRepository binTimePredictionRepository;
    private final PostponedBinRepository postponedBinRepository;
    private final CollectionScheduleService collectionScheduleService;
    private final TruckWasteCompatibilityService truckWasteCompatibilityService;
    private final BinRepository binRepository;
    private final DepotRepository depotRepository;

    private final List<RecommendedFuelStationDto> lastRecommendedFuelStations = new ArrayList<>();

    private enum RoutingRun {
        MORNING,
        EVENING
    }

    public RoutingPayloadBuilderServiceImpl(
            BinPriorityService binPriorityService,
            TruckIncidentRepository truckIncidentRepository,
            FuelManagementService fuelManagementService,
            FuelStationService fuelStationService,
            BinTimePredictionRepository binTimePredictionRepository,
            PostponedBinRepository postponedBinRepository,
            CollectionScheduleService collectionScheduleService,
            TruckWasteCompatibilityService truckWasteCompatibilityService,
            BinRepository binRepository,
            DepotRepository depotRepository
    ) {
        this.binPriorityService = binPriorityService;
        this.truckIncidentRepository = truckIncidentRepository;
        this.fuelManagementService = fuelManagementService;
        this.fuelStationService = fuelStationService;
        this.binTimePredictionRepository = binTimePredictionRepository;
        this.postponedBinRepository = postponedBinRepository;
        this.collectionScheduleService = collectionScheduleService;
        this.truckWasteCompatibilityService = truckWasteCompatibilityService;
        this.binRepository = binRepository;
        this.depotRepository = depotRepository;
    }

    @Override
    public RoutingRequestDto buildRoutingRequest(List<Truck> trucks) {
        return buildRoutingRequest(trucks, RoutingDecision.fullOptimization("Default routing request"));
    }

    @Override
    public RoutingRequestDto buildRoutingRequest(List<Truck> trucks, RoutingDecision decision) {
        lastRecommendedFuelStations.clear();

        RoutingRun currentRun = resolveCurrentRun();
        RoutingDepotDto depot = buildDefaultDepot();
        List<RoutingTruckDto> routingTrucks = buildTrucks(trucks, depot);

        RoutingRequestDto request = new RoutingRequestDto();
        request.setDepot(depot);
        request.setTrafficMode("NORMAL");
        request.setCurrentRun(currentRun.name());
        request.setTrucks(routingTrucks);
        request.setBins(buildRoutingBins(decision, routingTrucks));
        request.setActiveIncidents(buildActiveIncidents(trucks));

        System.out.println(
                "Routing payload built — strategy=" + (decision != null ? decision.getStrategy() : null)
                        + " — currentRun=" + request.getCurrentRun()
                        + " — depotLat=" + (request.getDepot() != null ? request.getDepot().getLat() : null)
                        + " — depotLng=" + (request.getDepot() != null ? request.getDepot().getLng() : null)
                        + " — trucks=" + request.getTrucks().size()
                        + " — bins=" + request.getBins().size()
                        + " — activeIncidents=" + request.getActiveIncidents().size()
                        + " — recommendedFuelStations=" + lastRecommendedFuelStations.size()
        );

        return request;
    }

    @Override
    public RoutingRequestDto buildReplanRequest(List<Truck> trucks, List<MissionBin> remainingMissionBins) {
        lastRecommendedFuelStations.clear();

        RoutingRun currentRun = resolveCurrentRun();
        RoutingDepotDto depot = buildDefaultDepot();
        List<RoutingTruckDto> routingTrucks = buildTrucks(trucks, depot);

        RoutingRequestDto request = new RoutingRequestDto();
        request.setDepot(depot);
        request.setTrafficMode("NORMAL");
        request.setCurrentRun(currentRun.name());
        request.setTrucks(routingTrucks);
        request.setBins(buildRoutingBinsFromMissionBins(remainingMissionBins, routingTrucks));
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
            enrichBinSpatialMetadataFromRepository(bin);

            if (!isValidCoordinatePair(bin.getLat(), bin.getLng())) {
                continue;
            }

            enrichRoutingBin(bin);
            initializeCollectionMetadata(bin);

            boolean mandatoryByUrgency = isDecisionReasonUrgency(bin.getDecisionReason());
            boolean mandatoryByFeedback = "MANDATORY_BY_FEEDBACK_SCORE".equals(bin.getDecisionReason());

            insights.add(
                    toMandatoryInsight(
                            bin,
                            Boolean.TRUE.equals(bin.getMandatory()),
                            mandatoryByUrgency,
                            mandatoryByFeedback,
                            bin.getPostponementCount() != null ? bin.getPostponementCount() : 0L,
                            bin.getFeedbackScore() != null ? bin.getFeedbackScore() : 0.0
                    )
            );
        }

        return insights;
    }

    private List<RoutingBinDto> buildRoutingBins(RoutingDecision decision, List<RoutingTruckDto> routingTrucks) {
        List<RoutingBinDto> sourceBins = binPriorityService.getPriorityBinsForRouting();
        List<RoutingBinDto> result = new ArrayList<>();

        if (sourceBins == null) {
            return result;
        }

        for (RoutingBinDto bin : sourceBins) {
            enrichBinSpatialMetadataFromRepository(bin);

            if (!isValidCoordinatePair(bin.getLat(), bin.getLng())) {
                System.out.println("SKIPPED BIN => invalid coordinates, binId=" + bin.getId()
                        + ", lat=" + bin.getLat()
                        + ", lng=" + bin.getLng());
                continue;
            }

            enrichRoutingBin(bin);
            initializeCollectionMetadata(bin);

            System.out.println(
                    "PRE-CLASSIFICATION => id=" + bin.getId()
                            + ", lat=" + bin.getLat()
                            + ", lng=" + bin.getLng()
                            + ", fillLevel=" + bin.getFillLevel()
                            + ", predictedPriority=" + bin.getPredictedPriority()
                            + ", predictedHoursToFull=" + bin.getPredictedHoursToFull()
                            + ", wasteType=" + bin.getWasteType()
                            + ", zoneId=" + bin.getZoneId()
                            + ", clusterId=" + bin.getClusterId()
                            + ", allowedNow=" + bin.getCollectionAllowedNow()
                            + ", windowStart=" + bin.getWindowStartMinutes()
                            + ", windowEnd=" + bin.getWindowEndMinutes()
                            + ", decisionCategory=" + bin.getDecisionCategory()
                            + ", decisionReason=" + bin.getDecisionReason()
            );

            if (!Boolean.TRUE.equals(bin.getCollectionAllowedNow())) {
                System.out.println(
                        "SKIPPED_BEFORE_CLASSIFICATION => id=" + bin.getId()
                                + ", reason=SCHEDULE_BLOCKED"
                                + ", fillLevel=" + bin.getFillLevel()
                                + ", predictedPriority=" + bin.getPredictedPriority()
                );
                continue;
            }

            if (!truckWasteCompatibilityService.hasAtLeastOneCompatibleTruck(routingTrucks, bin)) {
                bin.setMandatory(false);
                bin.setOpportunistic(false);
                bin.setReportable(true);
                bin.setDecisionCategory("NO_COMPATIBLE_TRUCK");
                bin.setDecisionReason("Aucun camion compatible n'est disponible pour ce type de bac.");
                bin.setOpportunisticScore(0.0);

                System.out.println(
                        "SKIPPED_BEFORE_CLASSIFICATION => id=" + bin.getId()
                                + ", reason=NO_COMPATIBLE_TRUCK"
                                + ", fillLevel=" + bin.getFillLevel()
                                + ", predictedPriority=" + bin.getPredictedPriority()
                                + ", wasteType=" + bin.getWasteType()
                );
                continue;
            }

            applyDecisionClassification(bin);

            System.out.println(
                    "POST-CLASSIFICATION => id=" + bin.getId()
                            + ", fillLevel=" + bin.getFillLevel()
                            + ", predictedPriority=" + bin.getPredictedPriority()
                            + ", predictedHoursToFull=" + bin.getPredictedHoursToFull()
                            + ", mandatory=" + bin.getMandatory()
                            + ", opportunistic=" + bin.getOpportunistic()
                            + ", reportable=" + bin.getReportable()
                            + ", decisionCategory=" + bin.getDecisionCategory()
                            + ", decisionReason=" + bin.getDecisionReason()
                            + ", feedbackScore=" + bin.getFeedbackScore()
                            + ", postponementCount=" + bin.getPostponementCount()
                            + ", opportunisticScore=" + bin.getOpportunisticScore()
            );

            if (decision != null && !decision.isShouldOptimize()) {
                System.out.println(
                        "SKIPPED_AFTER_CLASSIFICATION => id=" + bin.getId()
                                + ", reason=DECISION_SHOULD_NOT_OPTIMIZE"
                );
                continue;
            }

            if (decision != null && decision.isRefuelOnly()) {
                System.out.println(
                        "SKIPPED_AFTER_CLASSIFICATION => id=" + bin.getId()
                                + ", reason=REFUEL_ONLY_MODE"
                );
                continue;
            }

            if (Boolean.TRUE.equals(bin.getMandatory())) {
                result.add(bin);
                System.out.println(
                        "ADDED_TO_FINAL_PAYLOAD => id=" + bin.getId()
                                + ", reason=MANDATORY"
                                + ", fillLevel=" + bin.getFillLevel()
                                + ", predictedPriority=" + bin.getPredictedPriority()
                                + ", decisionReason=" + bin.getDecisionReason()
                );
                continue;
            }

            if (decision != null && decision.isIncludeOpportunistic() && Boolean.TRUE.equals(bin.getOpportunistic())) {
                result.add(bin);
                System.out.println(
                        "ADDED_TO_FINAL_PAYLOAD => id=" + bin.getId()
                                + ", reason=OPPORTUNISTIC"
                                + ", fillLevel=" + bin.getFillLevel()
                                + ", predictedPriority=" + bin.getPredictedPriority()
                                + ", decisionReason=" + bin.getDecisionReason()
                );
                continue;
            }

            System.out.println(
                    "NOT_ADDED_TO_FINAL_PAYLOAD => id=" + bin.getId()
                            + ", fillLevel=" + bin.getFillLevel()
                            + ", predictedPriority=" + bin.getPredictedPriority()
                            + ", mandatory=" + bin.getMandatory()
                            + ", opportunistic=" + bin.getOpportunistic()
                            + ", reportable=" + bin.getReportable()
                            + ", decisionReason=" + bin.getDecisionReason()
            );
        }

        long mandatoryNow = result.stream().filter(b -> Boolean.TRUE.equals(b.getMandatory())).count();
        long opportunisticNow = result.stream().filter(b -> Boolean.TRUE.equals(b.getOpportunistic())).count();
        long reportableNow = result.stream().filter(b -> Boolean.TRUE.equals(b.getReportable())).count();

        System.out.println(
                "Run-aware classification => mandatoryNow=" + mandatoryNow
                        + ", opportunisticNow=" + opportunisticNow
                        + ", reportableNow=" + reportableNow
        );

        System.out.println(
                "Routing decision => shouldOptimize=" + (decision != null && decision.isShouldOptimize())
                        + ", refuelOnly=" + (decision != null && decision.isRefuelOnly())
                        + ", includeOpportunistic=" + (decision != null && decision.isIncludeOpportunistic())
                        + ", strategy=" + (decision != null ? decision.getStrategy() : null)
                        + ", reason=" + (decision != null ? decision.getReason() : null)
        );

        for (RoutingBinDto b : result) {
            System.out.println(
                    "FINAL_BIN => id=" + b.getId()
                            + ", fillLevel=" + b.getFillLevel()
                            + ", predictedPriority=" + b.getPredictedPriority()
                            + ", predictedHoursToFull=" + b.getPredictedHoursToFull()
                            + ", mandatory=" + b.getMandatory()
                            + ", opportunistic=" + b.getOpportunistic()
                            + ", reportable=" + b.getReportable()
                            + ", decisionCategory=" + b.getDecisionCategory()
                            + ", decisionReason=" + b.getDecisionReason()
            );
        }

        System.out.println("FINAL BINS SENT TO PYTHON = " + result.size());

        return result;
    }

    private List<RoutingBinDto> buildRoutingBinsFromMissionBins(
            List<MissionBin> remainingMissionBins,
            List<RoutingTruckDto> routingTrucks
    ) {
        List<RoutingBinDto> result = new ArrayList<>();

        if (remainingMissionBins == null) {
            return result;
        }

        for (MissionBin missionBin : remainingMissionBins) {
            if (missionBin == null || missionBin.getBin() == null) {
                continue;
            }

            Bin binEntity = missionBin.getBin();

            Double lat = resolveBinRoutingLat(binEntity);
            Double lng = resolveBinRoutingLng(binEntity);

            if (!isValidCoordinatePair(lat, lng)) {
                System.out.println("REPLAN SKIPPED => invalid bin coordinates, binId=" + binEntity.getId()
                        + ", lat=" + lat
                        + ", lng=" + lng);
                continue;
            }

            RoutingBinDto dto = new RoutingBinDto();
            dto.setId(binEntity.getId());
            dto.setLat(lat);
            dto.setLng(lng);
            dto.setWasteType(extractBinWasteType(binEntity));
            enrichBinSpatialMetadata(dto, binEntity);

            enrichRoutingBin(dto);
            initializeCollectionMetadata(dto);

            System.out.println(
                    "REPLAN_PRE-CLASSIFICATION => id=" + dto.getId()
                            + ", fillLevel=" + dto.getFillLevel()
                            + ", predictedPriority=" + dto.getPredictedPriority()
                            + ", predictedHoursToFull=" + dto.getPredictedHoursToFull()
                            + ", wasteType=" + dto.getWasteType()
                            + ", allowedNow=" + dto.getCollectionAllowedNow()
            );

            if (!Boolean.TRUE.equals(dto.getCollectionAllowedNow())) {
                System.out.println(
                        "REPLAN_SKIPPED_BEFORE_CLASSIFICATION => id=" + dto.getId()
                                + ", reason=SCHEDULE_BLOCKED"
                );
                continue;
            }

            if (!truckWasteCompatibilityService.hasAtLeastOneCompatibleTruck(routingTrucks, dto)) {
                System.out.println(
                        "REPLAN_SKIPPED_BEFORE_CLASSIFICATION => id=" + dto.getId()
                                + ", reason=NO_COMPATIBLE_TRUCK"
                                + ", wasteType=" + dto.getWasteType()
                );
                continue;
            }

            applyDecisionClassification(dto);

            System.out.println(
                    "REPLAN_POST-CLASSIFICATION => id=" + dto.getId()
                            + ", fillLevel=" + dto.getFillLevel()
                            + ", predictedPriority=" + dto.getPredictedPriority()
                            + ", predictedHoursToFull=" + dto.getPredictedHoursToFull()
                            + ", mandatory=" + dto.getMandatory()
                            + ", opportunistic=" + dto.getOpportunistic()
                            + ", reportable=" + dto.getReportable()
                            + ", decisionCategory=" + dto.getDecisionCategory()
                            + ", decisionReason=" + dto.getDecisionReason()
            );

            result.add(dto);
        }

        return result;
    }

    private void enrichRoutingBin(RoutingBinDto bin) {
        double fillLevel = safeDouble(bin.getFillLevel());
        double estimatedLoadKg = computeEstimatedLoadKg(fillLevel, DEFAULT_BIN_MAX_CAPACITY_KG);
        bin.setEstimatedLoadKg(estimatedLoadKg);

        Double predictedHoursToFull = findLatestPredictedHours(bin.getId());
        bin.setPredictedHoursToFull(predictedHoursToFull);

        if (bin.getWasteType() == null || bin.getWasteType().isBlank()) {
            bin.setWasteType("UNKNOWN");
        }
    }

    private void enrichBinSpatialMetadata(RoutingBinDto dto, Bin binEntity) {
        if (dto == null || binEntity == null) {
            return;
        }

        dto.setClusterId(binEntity.getClusterId());

        if (binEntity.getZone() != null && binEntity.getZone().getId() != null) {
            dto.setZoneId(binEntity.getZone().getId());
        }
    }

    private void enrichBinSpatialMetadataFromRepository(RoutingBinDto dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }

        try {
            Bin binEntity = binRepository.findById(dto.getId()).orElse(null);
            if (binEntity == null) {
                return;
            }

            dto.setLat(resolveBinRoutingLat(binEntity));
            dto.setLng(resolveBinRoutingLng(binEntity));

            enrichBinSpatialMetadata(dto, binEntity);
        } catch (Exception e) {
            System.out.println("Failed to enrich spatial metadata for binId=" + dto.getId() + ": " + e.getMessage());
        }
    }

    private void initializeCollectionMetadata(RoutingBinDto bin) {
        boolean allowed = true;

        bin.setCollectionAllowedNow(allowed);
        bin.setCollectionWindowExplanation(collectionScheduleService.explainCollectionWindow(bin));
        bin.setWindowStartMinutes(resolveCollectionWindowStartMinutes(bin));
        bin.setWindowEndMinutes(resolveCollectionWindowEndMinutes(bin));

        if (!allowed) {
            bin.setMandatory(false);
            bin.setOpportunistic(false);
            bin.setReportable(true);
            bin.setDecisionCategory("SCHEDULE_BLOCKED");
            bin.setDecisionReason(
                    "Ce bac est exclu de la tournée actuelle car la fenêtre de collecte autorisée n'est pas ouverte. "
                            + collectionScheduleService.explainCollectionWindow(bin)
            );
            bin.setFeedbackScore(0.0);
            bin.setPostponementCount(0L);
            bin.setOpportunisticScore(0.0);
        }
    }

    private Integer resolveCollectionWindowStartMinutes(RoutingBinDto bin) {
        if (bin == null || bin.getWasteType() == null || bin.getWasteType().isBlank()) {
            return null;
        }

        String wasteType = bin.getWasteType().trim().toUpperCase();

        if ("GRAY".equals(wasteType) || "GREEN".equals(wasteType)) {
            return 16 * 60;
        }

        if ("YELLOW".equals(wasteType)) {
            return (15 * 60) + 30;
        }

        if ("WHITE".equals(wasteType)) {
            return 14 * 60;
        }

        return null;
    }

    private Integer resolveCollectionWindowEndMinutes(RoutingBinDto bin) {
        if (bin == null || bin.getWasteType() == null || bin.getWasteType().isBlank()) {
            return null;
        }

        String wasteType = bin.getWasteType().trim().toUpperCase();

        if ("GRAY".equals(wasteType) || "GREEN".equals(wasteType)) {
            return 23 * 60;
        }

        if ("YELLOW".equals(wasteType)) {
            return (22 * 60) + 30;
        }

        if ("WHITE".equals(wasteType)) {
            return 20 * 60;
        }

        return null;
    }

    private void applyDecisionClassification(RoutingBinDto bin) {
        if (!Boolean.TRUE.equals(bin.getCollectionAllowedNow())) {
            return;
        }

        RoutingRun currentRun = resolveCurrentRun();

        List<PostponedBin> activePostponedBins = findActivePostponements(bin.getId());
        long postponementCount = activePostponedBins.size();
        double feedbackScore = computeFeedbackScore(bin, activePostponedBins);

        double priority = safeDouble(bin.getPredictedPriority());
        double fillLevel = safeDouble(bin.getFillLevel());
        Double predictedHours = bin.getPredictedHoursToFull();
        double hoursUntilNextRun = computeHoursUntilNextRun(currentRun);

        boolean overflowBeforeCurrentRunEnd = willOverflowBeforeCurrentRunEnd(predictedHours, currentRun);
        boolean overflowBeforeNextRun = willOverflowBeforeNextRun(predictedHours, currentRun);
        boolean mandatoryByUrgentHours = predictedHours != null && predictedHours <= MANDATORY_HOURS_THRESHOLD;
        boolean mandatoryByHighFill = fillLevel >= MANDATORY_FILL_THRESHOLD;
        boolean mandatoryByHighPriority =
                priority >= MANDATORY_PRIORITY_THRESHOLD
                        && (fillLevel >= 80.0 || (predictedHours != null && predictedHours <= 24.0));
        boolean mandatoryByFeedback = feedbackScore >= FEEDBACK_SCORE_THRESHOLD;

        bin.setFeedbackScore(round(feedbackScore));
        bin.setPostponementCount(postponementCount);

        if (overflowBeforeCurrentRunEnd) {
            markMandatory(bin, "MANDATORY_OVERFLOW_BEFORE_CURRENT_RUN_END");
            return;
        }

        if (overflowBeforeNextRun) {
            markMandatory(bin, "MANDATORY_OVERFLOW_BEFORE_NEXT_RUN");
            return;
        }

        if (mandatoryByUrgentHours) {
            markMandatory(bin, "MANDATORY_BY_URGENT_HOURS");
            return;
        }

        if (mandatoryByHighFill) {
            markMandatory(bin, "MANDATORY_BY_HIGH_FILL");
            return;
        }

        if (mandatoryByHighPriority) {
            markMandatory(bin, "MANDATORY_BY_HIGH_PRIORITY");
            return;
        }

        if (mandatoryByFeedback) {
            markMandatory(bin, "MANDATORY_BY_FEEDBACK_SCORE");
            return;
        }

        boolean opportunisticByRunWindow =
                predictedHours != null
                        && predictedHours > hoursUntilNextRun
                        && predictedHours <= (hoursUntilNextRun + OPPORTUNISTIC_BUFFER_HOURS);

        boolean opportunistic =
                opportunisticByRunWindow
                        || (priority >= OPPORTUNISTIC_PRIORITY_THRESHOLD
                        && (fillLevel >= 60.0 || (predictedHours != null && predictedHours <= 48.0)))
                        || fillLevel >= OPPORTUNISTIC_FILL_THRESHOLD
                        || (predictedHours != null && predictedHours <= OPPORTUNISTIC_HOURS_THRESHOLD)
                        || feedbackScore >= OPPORTUNISTIC_FEEDBACK_THRESHOLD
                        || postponementCount >= 1;

        if (opportunistic) {
            double opportunisticScore = computeOpportunisticScore(
                    priority,
                    fillLevel,
                    predictedHours,
                    feedbackScore,
                    postponementCount,
                    hoursUntilNextRun,
                    opportunisticByRunWindow
            );

            bin.setMandatory(false);
            bin.setDecisionCategory(BinDecisionCategory.OPPORTUNISTIC.getCode());
            bin.setDecisionReason(
                    opportunisticByRunWindow
                            ? "OPPORTUNISTIC_CAN_WAIT_UNTIL_NEXT_RUN_WITH_LOW_MARGIN"
                            : "OPPORTUNISTIC_BY_MEDIUM_URGENCY"
            );
            bin.setOpportunistic(true);
            bin.setReportable(false);
            bin.setOpportunisticScore(round(opportunisticScore));
            return;
        }

        bin.setMandatory(false);
        bin.setDecisionCategory(BinDecisionCategory.REPORTABLE.getCode());
        bin.setDecisionReason("REPORTABLE_CAN_WAIT_BEYOND_NEXT_RUN");
        bin.setOpportunistic(false);
        bin.setReportable(true);
        bin.setOpportunisticScore(0.0);
    }

    private double computeOpportunisticScore(
            double priority,
            double fillLevel,
            Double predictedHours,
            double feedbackScore,
            long postponementCount,
            double hoursUntilNextRun,
            boolean opportunisticByRunWindow
    ) {
        double score = 0.0;

        if (opportunisticByRunWindow) {
            score += 3.0;
        }

        if (predictedHours != null) {
            if (predictedHours <= 12.0) {
                score += 3.0;
            } else if (predictedHours <= 24.0) {
                score += 2.0;
            } else if (predictedHours <= 48.0) {
                score += 1.0;
            }

            if (predictedHours <= (hoursUntilNextRun + OPPORTUNISTIC_BUFFER_HOURS)) {
                score += 1.0;
            }
        }

        if (fillLevel >= 90.0) {
            score += 3.0;
        } else if (fillLevel >= 80.0) {
            score += 2.0;
        } else if (fillLevel >= 70.0) {
            score += 1.0;
        }

        if (priority >= 0.95) {
            score += 3.0;
        } else if (priority >= 0.90) {
            score += 2.0;
        } else if (priority >= 0.85) {
            score += 1.0;
        }

        if (feedbackScore >= 6.0) {
            score += 2.0;
        } else if (feedbackScore >= 3.0) {
            score += 1.0;
        }

        score += Math.min(postponementCount, 3);

        return score;
    }

    private boolean isDecisionReasonUrgency(String decisionReason) {
        if (decisionReason == null) {
            return false;
        }

        return "MANDATORY_OVERFLOW_BEFORE_CURRENT_RUN_END".equals(decisionReason)
                || "MANDATORY_OVERFLOW_BEFORE_NEXT_RUN".equals(decisionReason)
                || "MANDATORY_BY_URGENT_HOURS".equals(decisionReason)
                || "MANDATORY_BY_HIGH_FILL".equals(decisionReason);
    }

    private RoutingRun resolveCurrentRun() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        if (hour >= 14) {
            return RoutingRun.EVENING;
        }

        return RoutingRun.MORNING;
    }

    private LocalDateTime resolveNextRunStart(LocalDateTime now, RoutingRun currentRun) {
        if (currentRun == RoutingRun.MORNING) {
            return now.withHour(EVENING_START_HOUR).withMinute(0).withSecond(0).withNano(0);
        }

        return now.plusDays(1)
                .withHour(MORNING_START_HOUR)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private LocalDateTime resolveCurrentRunEnd(LocalDateTime now, RoutingRun currentRun) {
        if (currentRun == RoutingRun.MORNING) {
            return now.withHour(MORNING_END_HOUR).withMinute(0).withSecond(0).withNano(0);
        }

        return now.withHour(EVENING_END_HOUR).withMinute(0).withSecond(0).withNano(0);
    }

    private double computeHoursUntilNextRun(RoutingRun currentRun) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRunStart = resolveNextRunStart(now, currentRun);
        long minutes = Duration.between(now, nextRunStart).toMinutes();
        return Math.max(0.0, minutes / 60.0);
    }

    private boolean willOverflowBeforeCurrentRunEnd(Double predictedHoursToFull, RoutingRun currentRun) {
        if (predictedHoursToFull == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime predictedFullAt = now.plusMinutes((long) (predictedHoursToFull * 60));
        LocalDateTime currentRunEnd = resolveCurrentRunEnd(now, currentRun);

        return !predictedFullAt.isAfter(currentRunEnd);
    }

    private boolean willOverflowBeforeNextRun(Double predictedHoursToFull, RoutingRun currentRun) {
        if (predictedHoursToFull == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime predictedFullAt = now.plusMinutes((long) (predictedHoursToFull * 60));
        LocalDateTime nextRunStart = resolveNextRunStart(now, currentRun);

        return !predictedFullAt.isAfter(nextRunStart);
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
        dto.setFillLevel(round(safeDouble(bin.getFillLevel())));
        dto.setPredictedPriority(round(safeDouble(bin.getPredictedPriority())));
        dto.setPredictedHoursToFull(round(bin.getPredictedHoursToFull()));
        dto.setMandatory(mandatory);
        dto.setMandatoryByUrgency(mandatoryByUrgency);
        dto.setMandatoryByFeedback(mandatoryByFeedback);
        dto.setPostponementCount(postponementCount);
        dto.setFeedbackScore(round(feedbackScore));

        dto.setReason(bin.getDecisionReason());
        dto.setDecisionCategory(bin.getDecisionCategory());
        dto.setDecisionReason(resolveDecisionReasonFr(bin));
        dto.setOpportunistic(bin.getOpportunistic());
        dto.setReportable(bin.getReportable());
        dto.setOpportunisticScore(round(safeDouble(bin.getOpportunisticScore())));

        dto.setScoreExplanation(buildScoreExplanationFr(bin));
        dto.setUrgencyExplanation(buildUrgencyExplanationFr(bin));
        dto.setFeedbackExplanation(buildFeedbackExplanationFr(bin));
        dto.setPostponementExplanation(buildPostponementExplanationFr(bin));
        dto.setClassificationExplanation(buildClassificationExplanationFr(bin));

        return dto;
    }

    private List<RoutingTruckDto> buildTrucks(List<Truck> trucks, RoutingDepotDto depot) {
        List<RoutingTruckDto> result = new ArrayList<>();

        if (trucks == null) {
            return result;
        }

        for (Truck truck : trucks) {
            if (truck == null) {
                continue;
            }

            RoutingTruckDto dto = new RoutingTruckDto();
            dto.setId(truck.getId());

            Double truckLat = extractDouble(truck, "getLastKnownLat");
            Double truckLng = extractDouble(truck, "getLastKnownLng");

            if (!isValidCoordinatePair(truckLat, truckLng) && depot != null) {
                truckLat = depot.getLat();
                truckLng = depot.getLng();

                System.out.println("TRUCK COORD FALLBACK TO DEPOT => truckId=" + truck.getId()
                        + ", lat=" + truckLat
                        + ", lng=" + truckLng);
            }

            dto.setLat(truckLat);
            dto.setLng(truckLng);
            dto.setRemainingCapacityKg(resolveRemainingCapacityKg(truck));
            dto.setFuelLevelLiters(extractBigDecimalAsDouble(truck, "getFuelLevelLiters"));
            dto.setFuelConsumptionPerKm(extractBigDecimalAsDouble(truck, "getFuelConsumptionPerKm"));
            dto.setStatus(extractEnumName(truck, "getStatus"));
            dto.setSupportedWasteTypes(extractSupportedWasteTypes(truck));

            if (truck.getZone() != null && truck.getZone().getId() != null) {
                dto.setZoneId(truck.getZone().getId());
            } else {
                dto.setZoneId(null);
            }

            if (!isValidCoordinatePair(dto.getLat(), dto.getLng())) {
                System.out.println("TRUCK SKIPPED => invalid coordinates, truckId=" + truck.getId()
                        + ", lat=" + dto.getLat()
                        + ", lng=" + dto.getLng());
                continue;
            }

            double safeAutonomyKm = fuelManagementService.calculateEstimatedAutonomyKm(truck);
            boolean fuelCritical = fuelManagementService.isFuelCritical(truck);
            boolean refuelRecommended = fuelManagementService.isRefuelRecommended(truck);

            System.out.println(
                    "FUEL DEBUG => truckId=" + truck.getId()
                            + ", fuelLiters=" + truck.getFuelLevelLiters()
                            + ", consumptionPerKm=" + truck.getFuelConsumptionPerKm()
                            + ", safeAutonomyKm=" + safeAutonomyKm
                            + ", critical=" + fuelCritical
                            + ", refuelRecommended=" + refuelRecommended
            );

            result.add(dto);

            FuelStation station = fuelStationService.findNearestCompatibleStation(truck);
            if (station != null && fuelManagementService.isRefuelRecommended(truck)) {
                RecommendedFuelStationDto recommended = new RecommendedFuelStationDto();
                recommended.setTruckId(truck.getId());
                recommended.setStationId(station.getId());
                recommended.setStationName(station.getName());
                recommended.setLat(station.getLat());
                recommended.setLng(station.getLng());
                lastRecommendedFuelStations.add(recommended);
            }
        }

        for (RoutingTruckDto t : result) {
            System.out.println(
                    "TRUCK DEBUG => id=" + t.getId()
                            + ", lat=" + t.getLat()
                            + ", lng=" + t.getLng()
                            + ", status=" + t.getStatus()
                            + ", supportedWasteTypes=" + t.getSupportedWasteTypes()
                            + ", remainingCapacityKg=" + t.getRemainingCapacityKg()
                            + ", zoneId=" + t.getZoneId()
            );
        }

        System.out.println("FINAL TRUCKS SENT TO PYTHON = " + result.size());

        return result;
    }

    private List<RoutingIncidentDto> buildActiveIncidents(List<Truck> trucks) {
        List<RoutingIncidentDto> result = new ArrayList<>();

        if (trucks == null || trucks.isEmpty()) {
            return result;
        }

        Set<Long> allowedTruckIds = trucks.stream()
                .filter(truck -> truck != null && truck.getId() != null)
                .map(Truck::getId)
                .collect(Collectors.toSet());

        if (allowedTruckIds.isEmpty()) {
            return result;
        }

        List<TruckIncident> allActiveIncidents;
        try {
            allActiveIncidents = truckIncidentRepository.findByStatusIn(
                    List.of(
                            TruckIncident.IncidentStatus.OPEN,
                            TruckIncident.IncidentStatus.IN_PROGRESS
                    )
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }

        for (TruckIncident incident : allActiveIncidents) {
            if (incident == null || incident.getTruck() == null || incident.getTruck().getId() == null) {
                continue;
            }

            Long truckId = incident.getTruck().getId();
            if (!allowedTruckIds.contains(truckId)) {
                continue;
            }

            RoutingIncidentDto dto = new RoutingIncidentDto();
            dto.setId(incident.getId());
            dto.setTruckId(truckId);
            dto.setType(incident.getIncidentType() != null ? incident.getIncidentType().name() : null);
            dto.setSeverity(incident.getSeverity() != null ? incident.getSeverity().name() : null);
            dto.setDescription(incident.getDescription());

            result.add(dto);
        }

        return result;
    }

    private RoutingDepotDto buildDefaultDepot() {
        Depot depot = depotRepository.findByIsActiveTrue()
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No active depot found"));

        if (!isValidCoordinatePair(depot.getLat(), depot.getLng())) {
            throw new BadRequestException("Active depot has invalid coordinates");
        }

        System.out.println("ACTIVE DEPOT USED FOR ROUTING => lat=" + depot.getLat() + ", lng=" + depot.getLng());

        return new RoutingDepotDto(depot.getLat(), depot.getLng());
    }

    private List<PostponedBin> findActivePostponements(Long binId) {
        try {
            return postponedBinRepository.findByBinIdAndResolvedFalseOrderByCreatedAtDesc(binId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Double findLatestPredictedHours(Long binId) {
        try {
            Object latestPrediction = binTimePredictionRepository.findTopByBinIdOrderByCreatedAtDesc(binId).orElse(null);
            if (latestPrediction == null) {
                return null;
            }

            try {
                Object value = latestPrediction.getClass().getMethod("getPredictedHoursToFull").invoke(latestPrediction);
                return value instanceof Double ? (Double) value : null;
            } catch (Exception ignored) {
            }

            try {
                Object value = latestPrediction.getClass().getMethod("getPredictedHoursToThreshold").invoke(latestPrediction);
                return value instanceof Double ? (Double) value : null;
            } catch (Exception ignoredAgain) {
            }

            try {
                Object value = latestPrediction.getClass().getMethod("getPredictedHours").invoke(latestPrediction);
                if (value instanceof Double d) {
                    return d;
                }
                if (value instanceof Number n) {
                    return n.doubleValue();
                }
                return null;
            } catch (Exception ignoredThird) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private double computeEstimatedLoadKg(double fillLevel, double maxCapacityKg) {
        return round((Math.max(0.0, fillLevel) / 100.0) * maxCapacityKg);
    }

    private double computeFeedbackScore(RoutingBinDto bin, List<PostponedBin> activePostponedBins) {
        double score = 0.0;

        double fillLevel = safeDouble(bin.getFillLevel());
        double priority = safeDouble(bin.getPredictedPriority());
        Double predictedHours = bin.getPredictedHoursToFull();

        if (activePostponedBins != null && !activePostponedBins.isEmpty()) {
            score += activePostponedBins.size() * UNRESOLVED_POSTPONED_WEIGHT;
        }

        if (predictedHours != null && predictedHours <= MANDATORY_HOURS_THRESHOLD) {
            score += URGENT_HOURS_WEIGHT;
        }

        if (fillLevel >= OPPORTUNISTIC_FILL_THRESHOLD) {
            score += HIGH_FILL_WEIGHT;
        }

        if (priority >= OPPORTUNISTIC_PRIORITY_THRESHOLD) {
            score += HIGH_PRIORITY_WEIGHT;
        }

        return round(score);
    }

    private void markMandatory(RoutingBinDto bin, String reasonCode) {
        bin.setMandatory(true);
        bin.setDecisionCategory(BinDecisionCategory.MANDATORY.getCode());
        bin.setDecisionReason(reasonCode);
        bin.setOpportunistic(false);
        bin.setReportable(false);
        bin.setOpportunisticScore(0.0);
    }

    private String resolveDecisionReasonFr(RoutingBinDto bin) {
        if (bin == null || bin.getDecisionReason() == null || bin.getDecisionReason().isBlank()) {
            return "Aucune justification métier détaillée n'est disponible pour ce bac.";
        }

        return switch (bin.getDecisionReason()) {
            case "MANDATORY_OVERFLOW_BEFORE_CURRENT_RUN_END" ->
                    "Bac classé MANDATORY car il risque de déborder avant la fin du cycle de collecte en cours.";
            case "MANDATORY_OVERFLOW_BEFORE_NEXT_RUN" ->
                    "Bac classé MANDATORY car il risque de déborder avant le prochain passage planifié.";
            case "MANDATORY_BY_URGENT_HOURS" ->
                    "Bac classé MANDATORY car le temps estimé avant saturation est jugé critique.";
            case "MANDATORY_BY_HIGH_FILL" ->
                    "Bac classé MANDATORY car son niveau de remplissage est déjà très élevé et nécessite une collecte immédiate.";
            case "MANDATORY_BY_HIGH_PRIORITY" ->
                    "Bac classé MANDATORY car son score de priorité prédite est exceptionnellement élevé.";
            case "MANDATORY_BY_FEEDBACK_SCORE" ->
                    "Bac classé MANDATORY car les retours terrain et l'historique opérationnel indiquent une forte nécessité d'intervention.";
            case "OPPORTUNISTIC_CAN_WAIT_UNTIL_NEXT_RUN_WITH_LOW_MARGIN" ->
                    "Bac classé OPPORTUNISTIC car il peut probablement attendre le prochain passage, mais avec une marge de sécurité limitée.";
            case "OPPORTUNISTIC_BY_MEDIUM_URGENCY" ->
                    "Bac classé OPPORTUNISTIC car sa situation n'est pas encore critique, mais elle devient suffisamment sensible pour profiter d'une tournée disponible.";
            case "REPORTABLE_CAN_WAIT_BEYOND_NEXT_RUN" ->
                    "Bac classé REPORTABLE car il peut attendre au-delà du prochain cycle sans risque opérationnel immédiat.";
            default ->
                    bin.getDecisionReason();
        };
    }

    private String buildScoreExplanationFr(RoutingBinDto bin) {
        if (bin == null) {
            return "Le score n'a pas pu être calculé.";
        }

        if ("SCHEDULE_BLOCKED".equals(bin.getDecisionCategory())) {
            return "Aucun score opportuniste n'est retenu car la collecte n'est pas autorisée à ce moment.";
        }

        if ("NO_COMPATIBLE_TRUCK".equals(bin.getDecisionCategory())) {
            return "Le bac n'est pas envoyé au moteur d'optimisation car aucun camion compatible n'est disponible.";
        }

        double priority = round(safeDouble(bin.getPredictedPriority()));
        double fillLevel = round(safeDouble(bin.getFillLevel()));
        Double predictedHours = round(bin.getPredictedHoursToFull());
        double feedback = round(safeDouble(bin.getFeedbackScore()));
        long postponementCount = bin.getPostponementCount() != null ? bin.getPostponementCount() : 0L;
        double opportunisticScore = round(safeDouble(bin.getOpportunisticScore()));

        if (Boolean.TRUE.equals(bin.getMandatory())) {
            return "Même si un score opportuniste peut être estimé, la règle MANDATORY domine la décision. "
                    + "Les indicateurs observés sont : priorité prédite=" + priority
                    + ", remplissage=" + fillLevel + "%, heures avant saturation="
                    + (predictedHours != null ? predictedHours : "indisponibles")
                    + ", feedback=" + feedback
                    + ", reports=" + postponementCount + ".";
        }

        if (Boolean.TRUE.equals(bin.getOpportunistic())) {
            return "Le score opportuniste (" + opportunisticScore
                    + ") résulte d'un compromis entre la priorité prédite (" + priority
                    + "), le niveau de remplissage (" + fillLevel + "%), "
                    + "l'horizon avant saturation (" + (predictedHours != null ? predictedHours : "indisponible") + " h), "
                    + "le score de feedback (" + feedback + ") et "
                    + "l'historique de reports (" + postponementCount + ").";
        }

        return "Le score opportuniste (" + opportunisticScore
                + ") reste insuffisant pour justifier une collecte immédiate. "
                + "Les indicateurs observés sont : priorité=" + priority
                + ", remplissage=" + fillLevel + "%, heures avant saturation="
                + (predictedHours != null ? predictedHours : "indisponibles")
                + ", feedback=" + feedback
                + ", reports=" + postponementCount + ".";
    }

    private String buildUrgencyExplanationFr(RoutingBinDto bin) {
        if (bin == null) {
            return "Le niveau d'urgence n'a pas pu être évalué.";
        }

        if ("SCHEDULE_BLOCKED".equals(bin.getDecisionCategory())) {
            return "Le bac n'est pas analysé par le moteur de priorité car la fenêtre de collecte n'est pas ouverte.";
        }

        if ("NO_COMPATIBLE_TRUCK".equals(bin.getDecisionCategory())) {
            return "Le bac n'est pas retenu malgré son état car aucun camion compatible avec son type n'est disponible.";
        }

        Double predictedHours = bin.getPredictedHoursToFull();
        if (predictedHours == null) {
            return "Aucune estimation fiable du temps restant avant saturation n'est disponible.";
        }

        double hours = round(predictedHours);
        RoutingRun currentRun = resolveCurrentRun();

        if (willOverflowBeforeCurrentRunEnd(predictedHours, currentRun)) {
            return "Le bac devrait atteindre la saturation dans le cycle courant (≈ " + hours
                    + " h restantes), ce qui le rend immédiatement prioritaire.";
        }

        if (willOverflowBeforeNextRun(predictedHours, currentRun)) {
            return "Le bac devrait atteindre la saturation avant le prochain passage planifié (≈ " + hours
                    + " h restantes), ce qui justifie une décision renforcée.";
        }

        if (predictedHours <= MANDATORY_HOURS_THRESHOLD) {
            return "Le temps restant avant saturation est très faible (≈ " + hours
                    + " h), donc le niveau d'urgence est considéré comme critique.";
        }

        if (predictedHours <= OPPORTUNISTIC_HOURS_THRESHOLD) {
            return "Le temps restant avant saturation (≈ " + hours
                    + " h) n'est pas critique, mais il devient suffisamment court pour une collecte opportuniste.";
        }

        return "Le temps restant avant saturation (≈ " + hours
                + " h) laisse une marge confortable. Le bac peut être suivi sans action immédiate.";
    }

    private String buildFeedbackExplanationFr(RoutingBinDto bin) {
        if (bin == null) {
            return "Aucune analyse de feedback n'est disponible.";
        }

        if ("SCHEDULE_BLOCKED".equals(bin.getDecisionCategory())) {
            return "Le feedback n'est pas exploité pour cette tournée car la collecte n'est pas autorisée actuellement.";
        }

        if ("NO_COMPATIBLE_TRUCK".equals(bin.getDecisionCategory())) {
            return "Même si un feedback existe, aucun camion compatible n'est disponible pour ce type de bac.";
        }

        double feedback = round(safeDouble(bin.getFeedbackScore()));

        if (feedback >= FEEDBACK_SCORE_THRESHOLD) {
            return "Le score de feedback est élevé (" + feedback
                    + "), ce qui traduit une pression terrain importante et renforce la priorité de collecte.";
        }

        if (feedback >= OPPORTUNISTIC_FEEDBACK_THRESHOLD) {
            return "Le score de feedback (" + feedback
                    + ") révèle des signaux opérationnels modérés, suffisants pour favoriser une collecte opportuniste.";
        }

        return "Le score de feedback (" + feedback
                + ") reste faible et ne suffit pas, à lui seul, à imposer une collecte prioritaire.";
    }

    private String buildPostponementExplanationFr(RoutingBinDto bin) {
        if (bin == null) {
            return "Aucune information sur les reports n'est disponible.";
        }

        if ("SCHEDULE_BLOCKED".equals(bin.getDecisionCategory())) {
            return "Le bac reste hors tournée courante à cause de la contrainte horaire, indépendamment de l'historique de report.";
        }

        if ("NO_COMPATIBLE_TRUCK".equals(bin.getDecisionCategory())) {
            return "Le bac ne peut pas être affecté tant qu'aucun camion compatible n'est disponible.";
        }

        long postponementCount = bin.getPostponementCount() != null ? bin.getPostponementCount() : 0L;

        if (postponementCount <= 0) {
            return "Aucun report actif n'a été identifié pour ce bac.";
        }

        if (postponementCount == 1) {
            return "Le bac a déjà été reporté une fois, ce qui augmente légèrement sa sensibilité opérationnelle.";
        }

        return "Le bac présente un historique de " + postponementCount
                + " reports actifs, ce qui augmente la pression de collecte et pèse dans la décision finale.";
    }

    private String buildClassificationExplanationFr(RoutingBinDto bin) {
        if (bin == null || bin.getDecisionCategory() == null) {
            return "La classification n'a pas pu être expliquée.";
        }

        return switch (bin.getDecisionCategory()) {
            case "SCHEDULE_BLOCKED" ->
                    "Le bac est exclu de la tournée actuelle car la fenêtre de collecte autorisée n'est pas ouverte. "
                            + (bin.getCollectionWindowExplanation() != null ? bin.getCollectionWindowExplanation() : "");
            case "NO_COMPATIBLE_TRUCK" ->
                    "Le bac est temporairement exclu car aucun camion compatible avec son type n'est disponible pour éviter le mélange des déchets.";
            case "MANDATORY" ->
                    "Le bac est classé MANDATORY car au moins une règle critique domine la décision métier : risque de débordement imminent, remplissage très élevé, priorité prédite forte ou pression terrain significative.";
            case "OPPORTUNISTIC" ->
                    "Le bac est classé OPPORTUNISTIC car il présente un intérêt opérationnel réel sans atteindre le niveau de criticité d'un bac obligatoire. Il peut être intégré si la tournée conserve une marge suffisante.";
            case "REPORTABLE" ->
                    "Le bac est classé REPORTABLE car les indicateurs actuels restent compatibles avec un report sans risque immédiat sur la continuité du service.";
            default ->
                    "La catégorie métier a été renseignée, mais aucune explication standard n'est disponible.";
        };
    }

    private double resolveRemainingCapacityKg(Truck truck) {
        if (truck == null) {
            return 0.0;
        }

        BigDecimal maxLoad = extractBigDecimal(truck, "getMaxLoadKg");
        if (maxLoad == null) {
            return 0.0;
        }

        BigDecimal currentLoad = extractBigDecimal(truck, "getCurrentLoadKg");
        if (currentLoad == null) {
            currentLoad = BigDecimal.ZERO;
        }

        BigDecimal remainingCapacity = maxLoad.subtract(currentLoad);

        if (remainingCapacity.compareTo(BigDecimal.ZERO) < 0) {
            return 0.0;
        }

        return remainingCapacity.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Double resolveBinRoutingLat(Bin bin) {
        Double accessLat = extractDouble(bin, "getAccessLat");
        if (isValidLatitude(accessLat)) {
            return accessLat;
        }
        return extractDouble(bin, "getLat");
    }

    private Double resolveBinRoutingLng(Bin bin) {
        Double accessLng = extractDouble(bin, "getAccessLng");
        if (isValidLongitude(accessLng)) {
            return accessLng;
        }
        return extractDouble(bin, "getLng");
    }

    private String extractBinWasteType(Bin bin) {
        Object value = invokeGetter(bin, "getWasteType");
        if (value == null) {
            return "UNKNOWN";
        }
        return value.toString().trim().toUpperCase();
    }

    private List<String> extractSupportedWasteTypes(Truck truck) {
        Object value = invokeGetter(truck, "getSupportedWasteTypes");

        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item != null)
                    .map(item -> item.toString().trim().toUpperCase())
                    .toList();
        }

        return new ArrayList<>();
    }

    private Object invokeGetter(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private Double extractDouble(Object target, String methodName) {
        Object value = invokeGetter(target, methodName);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private BigDecimal extractBigDecimal(Object target, String methodName) {
        Object value = invokeGetter(target, methodName);

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        return null;
    }

    private Double extractBigDecimalAsDouble(Object target, String methodName) {
        BigDecimal value = extractBigDecimal(target, methodName);
        return value != null ? value.doubleValue() : null;
    }

    private String extractEnumName(Object target, String methodName) {
        Object value = invokeGetter(target, methodName);
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return value != null ? value.toString() : null;
    }

    private boolean isValidLatitude(Double lat) {
        return lat != null && lat >= -90 && lat <= 90 && lat != 0.0;
    }

    private boolean isValidLongitude(Double lng) {
        return lng != null && lng >= -180 && lng <= 180 && lng != 0.0;
    }

    private boolean isValidCoordinatePair(Double lat, Double lng) {
        return isValidLatitude(lat) && isValidLongitude(lng);
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}