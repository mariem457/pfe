package com.example.demo.service;

import com.example.demo.dto.MissionBinActionRequest;
import com.example.demo.dto.MissionBinResponse;
import com.example.demo.dto.MissionFuelStatusResponse;
import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.MissionRouteResponse;
import com.example.demo.dto.MissionRouteStopDto;
import com.example.demo.dto.RouteCoordinateDto;
import com.example.demo.dto.routing.RecommendedFuelStationDto;
import com.example.demo.entity.Driver;
import com.example.demo.entity.FuelStation;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.MissionBin.AssignmentStatus;
import com.example.demo.entity.RoutePlan;
import com.example.demo.entity.RouteStop;
import com.example.demo.entity.Truck;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.RoutePlanRepository;
import com.example.demo.repository.RouteStopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class MissionServiceImpl implements MissionService {

    private static final double MIN_DISTANCE_BUFFER_TO_STATION_KM = 1.0;
    private static final double PREEMPTIVE_REFUEL_MARGIN_KM = 2.0;
    private static final double MAX_ALLOWED_LEG_KM = 80.0;
    private static final double MAX_ALLOWED_DISTANCE_FROM_FIRST_POINT_KM = 50.0;

    private final MissionRepository missionRepository;
    private final MissionBinRepository missionBinRepository;
    private final DriverRepository driverRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteStopRepository routeStopRepository;
    private final WebClient.Builder webClientBuilder;
    private final FuelManagementService fuelManagementService;
    private final FuelStationService fuelStationService;

    public MissionServiceImpl(
            MissionRepository missionRepository,
            MissionBinRepository missionBinRepository,
            DriverRepository driverRepository,
            RoutePlanRepository routePlanRepository,
            RouteStopRepository routeStopRepository,
            WebClient.Builder webClientBuilder,
            FuelManagementService fuelManagementService,
            FuelStationService fuelStationService
    ) {
        this.missionRepository = missionRepository;
        this.missionBinRepository = missionBinRepository;
        this.driverRepository = driverRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeStopRepository = routeStopRepository;
        this.webClientBuilder = webClientBuilder;
        this.fuelManagementService = fuelManagementService;
        this.fuelStationService = fuelStationService;
    }

    @Override
    @Transactional
    public MissionResponse startMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        if ("COMPLETED".equalsIgnoreCase(mission.getStatus())) {
            throw new BadRequestException("Mission is already completed");
        }

        mission.setStatus("IN_PROGRESS");
        mission.setMissionStatusDetail(Mission.MissionStatusDetail.IN_PROGRESS);

        if (mission.getStartedAt() == null) {
            mission.setStartedAt(Instant.now());
        }

        missionRepository.save(mission);
        autoInsertRefuelStopIfNeeded(mission);

        return mapMissionToResponse(mission);
    }

    @Override
    @Transactional
    public MissionResponse completeMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        List<MissionBin> remainingBins =
                missionBinRepository.findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(missionId);

        if (!remainingBins.isEmpty()) {
            throw new BadRequestException("Cannot complete mission. Some bins are still not collected.");
        }

        mission.setStatus("COMPLETED");
        mission.setMissionStatusDetail(Mission.MissionStatusDetail.COMPLETED);

        if (mission.getStartedAt() == null) {
            mission.setStartedAt(Instant.now());
        }

        mission.setCompletedAt(Instant.now());
        missionRepository.save(mission);

        return mapMissionToResponse(mission);
    }

    @Override
    @Transactional
    public MissionResponse collectMissionBin(Long missionId, Long missionBinId, MissionBinActionRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        MissionBin missionBin = missionBinRepository.findById(missionBinId)
                .orElseThrow(() -> new ResourceNotFoundException("MissionBin not found: " + missionBinId));

        if (!missionBin.getMission().getId().equals(mission.getId())) {
            throw new BadRequestException("MissionBin does not belong to the given mission");
        }

        if (missionBin.isCollected()) {
            throw new BadRequestException("This bin is already collected");
        }

        Driver driver;
        if (request != null && request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.getDriverId()));
        } else {
            driver = mission.getDriver();
        }

        missionBin.setCollected(true);
        missionBin.setCollectedAt(Instant.now());
        missionBin.setCollectedBy(driver);
        missionBin.setAssignmentStatus(AssignmentStatus.COLLECTED);

        if (request != null) {
            missionBin.setDriverNote(request.getDriverNote());
            missionBin.setIssueType(request.getIssueType());
            missionBin.setPhotoUrl(request.getPhotoUrl());
        }

        missionBinRepository.save(missionBin);

        long remaining =
                missionBinRepository.findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(missionId).size();

        if (remaining == 0) {
            mission.setStatus("COMPLETED");
            mission.setMissionStatusDetail(Mission.MissionStatusDetail.COMPLETED);

            if (mission.getStartedAt() == null) {
                mission.setStartedAt(Instant.now());
            }

            mission.setCompletedAt(Instant.now());
            missionRepository.save(mission);
        } else {
            if (!"IN_PROGRESS".equalsIgnoreCase(mission.getStatus())) {
                mission.setStatus("IN_PROGRESS");
                mission.setMissionStatusDetail(Mission.MissionStatusDetail.IN_PROGRESS);
            }

            if (mission.getStartedAt() == null) {
                mission.setStartedAt(Instant.now());
            }

            missionRepository.save(mission);
            autoInsertRefuelStopIfNeeded(mission);
        }

        return mapMissionToResponse(mission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> getAllMissions() {
        return missionRepository.findAll()
                .stream()
                .map(this::mapMissionToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MissionResponse getMissionById(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        return mapMissionToResponse(mission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionBinResponse> getMissionBins(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        return missionBinRepository.findByMissionOrderByVisitOrderAsc(mission)
                .stream()
                .map(this::mapMissionBinToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MissionRouteResponse getMissionRoute(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        List<RoutePlan> plans = routePlanRepository.findByMissionOrderByCreatedAtDesc(mission);
        if (plans.isEmpty()) {
            throw new ResourceNotFoundException("No route plan found for mission: " + missionId);
        }

        RoutePlan latestPlan = plans.get(0);

        List<RouteStop> rawStops = routeStopRepository.findByRoutePlanOrderByStopOrderAsc(latestPlan);
        if (rawStops.isEmpty()) {
            throw new ResourceNotFoundException("No route stops found for mission: " + missionId);
        }

        List<RouteStop> usableStops = rawStops.stream()
                .filter(stop -> isValidCoordinatePair(stop.getLat(), stop.getLng()))
                .sorted(Comparator.comparing(RouteStop::getStopOrder))
                .toList();

        if (usableStops.size() < 2) {
            throw new ResourceNotFoundException("Not enough valid route stops to build route for mission: " + missionId);
        }

        List<String> warnings = new ArrayList<>();
        List<RouteStop> canonicalStops = normalizeAndValidateStops(usableStops, warnings);

        List<MissionRouteStopDto> routeStops = canonicalStops.stream()
                .map(this::mapRouteStopToDto)
                .toList();

        int lastBinIndex = findLastBinIndex(canonicalStops);

        List<RouteStop> collectionStops;
        List<RouteStop> transferStops;

        if (lastBinIndex >= 1 && lastBinIndex < canonicalStops.size() - 1) {
            collectionStops = new ArrayList<>(canonicalStops.subList(0, lastBinIndex + 1));
            transferStops = new ArrayList<>(canonicalStops.subList(lastBinIndex, canonicalStops.size()));
        } else {
            collectionStops = canonicalStops;
            transferStops = List.of();
        }

        OsrmRouteResult fullRoute = buildOsrmRoute(canonicalStops, warnings, "FULL");
        OsrmRouteResult collectionRoute = buildOsrmRoute(collectionStops, warnings, "COLLECTION");
        OsrmRouteResult transferRoute = buildOsrmRoute(transferStops, warnings, "TRANSFER");

        MissionRouteResponse dto = new MissionRouteResponse();
        dto.setMissionId(mission.getId());
        dto.setRoutePlanId(latestPlan.getId());

        if (latestPlan.getTruck() != null) {
            dto.setTruckId(latestPlan.getTruck().getId());
        }

        if (fullRoute.totalDistanceKm != null) {
            dto.setTotalDistanceKm(fullRoute.totalDistanceKm);
        } else if (latestPlan.getTotalDistanceKm() != null) {
            dto.setTotalDistanceKm(latestPlan.getTotalDistanceKm().setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        dto.setEstimatedDurationMin(latestPlan.getEstimatedDurationMin());
        dto.setRouteStops(routeStops);

        dto.setRouteCoordinates(fullRoute.routeCoordinates);
        dto.setSnappedWaypoints(fullRoute.snappedWaypoints);
        dto.setStopLegDistancesKm(fullRoute.legDistancesKm);

        dto.setCollectionRouteCoordinates(collectionRoute.routeCoordinates);
        dto.setCollectionSnappedWaypoints(collectionRoute.snappedWaypoints);
        dto.setCollectionDistanceKm(collectionRoute.totalDistanceKm);

        dto.setTransferRouteCoordinates(transferRoute.routeCoordinates);
        dto.setTransferSnappedWaypoints(transferRoute.snappedWaypoints);
        dto.setTransferDistanceKm(transferRoute.totalDistanceKm);

        dto.setMatrixSource("OSRM");
        dto.setGeometrySource("OSRM");
        dto.setValidationWarnings(warnings);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public MissionFuelStatusResponse getMissionFuelStatus(Long missionId) {
        Mission mission = getMissionOrThrow(missionId);
        RoutePlan latestPlan = getLatestRoutePlanOrThrow(mission);
        return buildMissionFuelStatus(mission, latestPlan);
    }

    @Override
    @Transactional
    public MissionFuelStatusResponse insertRefuelStop(Long missionId) {
        Mission mission = getMissionOrThrow(missionId);
        RoutePlan latestPlan = getLatestRoutePlanOrThrow(mission);

        MissionFuelStatusResponse status = buildMissionFuelStatus(mission, latestPlan);

        if ("CRITICAL".equals(status.getFuelStatus())) {
            status.setMessage("Carburant critique : le camion ne peut pas atteindre la station en sécurité.");
            status.setDecisionReason("Le niveau de carburant est jugé critique ou l'autonomie restante est insuffisante pour rejoindre la station recommandée.");
            return status;
        }

        if ("NORMAL".equals(status.getFuelStatus())) {
            status.setMessage("Niveau de carburant normal. Aucune insertion nécessaire.");
            status.setDecisionReason("L'autonomie sécurisée est suffisante, donc aucune station-service n'est ajoutée à l'itinéraire.");
            return status;
        }

        if (hasPlannedFuelStop(latestPlan)) {
            status.setMessage("Une station-service planifiée existe déjà pour cette mission.");
            status.setDecisionReason("Le système a détecté qu'un arrêt carburant est déjà présent dans l'itinéraire.");
            return status;
        }

        Truck truck = latestPlan.getTruck() != null ? latestPlan.getTruck() : mission.getTruck();
        if (truck == null) {
            throw new BadRequestException("Mission has no assigned truck");
        }

        FuelStation station = fuelStationService.findNearestCompatibleStation(truck);
        if (station == null) {
            status.setFuelStatus("CRITICAL");
            status.setMessage("Aucune station-service compatible active trouvée.");
            status.setDecisionReason("Aucune station compatible avec le type de carburant du camion n'est disponible.");
            return status;
        }

        int suggestedOrder = insertFuelStationStop(latestPlan, station);

        MissionFuelStatusResponse updatedStatus = buildMissionFuelStatus(mission, latestPlan);
        updatedStatus.setRefuelStopInserted(true);
        updatedStatus.setSuggestedInsertionOrder(suggestedOrder);
        updatedStatus.setMessage("Station-service insérée avec succès dans l'itinéraire.");
        updatedStatus.setRouteInsertionReason(
                "La station a été insérée avant l'arrêt numéro " + suggestedOrder
                        + " afin d'éviter un risque d'insuffisance de carburant pendant la mission."
        );

        return updatedStatus;
    }

    private void autoInsertRefuelStopIfNeeded(Mission mission) {
        try {
            if (mission == null || mission.getId() == null) {
                return;
            }

            RoutePlan latestPlan = getLatestRoutePlanOrThrow(mission);

            if (hasPlannedFuelStop(latestPlan)) {
                return;
            }

            MissionFuelStatusResponse fuelStatus = buildMissionFuelStatus(mission, latestPlan);

            if (!"REFUEL_REQUIRED".equals(fuelStatus.getFuelStatus())) {
                return;
            }

            Truck truck = latestPlan.getTruck() != null ? latestPlan.getTruck() : mission.getTruck();
            if (truck == null) {
                return;
            }

            FuelStation station = fuelStationService.findNearestCompatibleStation(truck);
            if (station == null) {
                return;
            }

            insertFuelStationStop(latestPlan, station);
        } catch (Exception e) {
            System.out.println("AUTO REFUEL INSERT FAILED => " + e.getMessage());
        }
    }

    private MissionFuelStatusResponse buildMissionFuelStatus(Mission mission, RoutePlan latestPlan) {
        Truck truck = latestPlan.getTruck() != null ? latestPlan.getTruck() : mission.getTruck();

        if (truck == null) {
            throw new BadRequestException("Mission has no assigned truck: " + mission.getId());
        }

        double autonomyKm = fuelManagementService.calculateEstimatedAutonomyKm(truck);
        FuelStation nearestStation = fuelStationService.findNearestCompatibleStation(truck);
        double distanceToStationKm = fuelStationService.distanceToNearestCompatibleStationKm(truck);

        boolean refuelStopAlreadyInserted = hasPlannedFuelStop(latestPlan);

        MissionFuelStatusResponse dto = new MissionFuelStatusResponse();
        dto.setMissionId(mission.getId());
        dto.setTruckId(truck.getId());
        dto.setSafeAutonomyKm(autonomyKm);
        dto.setDistanceToNearestStationKm(distanceToStationKm == Double.MAX_VALUE ? null : round(distanceToStationKm));
        dto.setRefuelStopInserted(refuelStopAlreadyInserted);
        dto.setRecommendedFuelStation(mapRecommendedFuelStation(truck, nearestStation));
        dto.setAlertThresholdKm(fuelManagementService.getRefuelAlertAutonomyThresholdKm());
        dto.setCriticalFuelThresholdLiters(fuelManagementService.getCriticalFuelThresholdLiters());
        dto.setTriggerDistanceKm(round(distanceToStationKm + MIN_DISTANCE_BUFFER_TO_STATION_KM));

        if (nearestStation == null) {
            dto.setFuelStatus("CRITICAL");
            dto.setMessage("Aucune station-service compatible active trouvée.");
            dto.setDecisionReason("Le système ne trouve aucune station compatible avec le type de carburant du camion.");
            return dto;
        }

        boolean canReachStationSafely = fuelManagementService.canCompleteDistance(
                truck,
                distanceToStationKm + MIN_DISTANCE_BUFFER_TO_STATION_KM
        );

        if (fuelManagementService.isFuelCritical(truck) || !canReachStationSafely) {
            dto.setFuelStatus("CRITICAL");
            dto.setMessage("Carburant critique. Le camion ne peut pas continuer en sécurité.");
            dto.setDecisionReason(
                    "Le carburant est critique ou bien l'autonomie sécurisée ("
                            + round(autonomyKm)
                            + " km) est inférieure à la distance nécessaire pour atteindre la station avec marge de sécurité ("
                            + round(distanceToStationKm + MIN_DISTANCE_BUFFER_TO_STATION_KM)
                            + " km)."
            );
            return dto;
        }

        if (refuelStopAlreadyInserted && !fuelManagementService.isRefuelRecommended(truck)) {
            dto.setFuelStatus("NORMAL");
            dto.setMessage("Niveau de carburant normal. Une station est déjà présente dans l'itinéraire.");
            dto.setDecisionReason(
                    "L'autonomie sécurisée actuelle est suffisante, mais un arrêt carburant a déjà été planifié auparavant."
            );
            return dto;
        }

        if (fuelManagementService.isRefuelRecommended(truck)) {
            int suggestedOrder = estimateSuggestedInsertionOrder(latestPlan, autonomyKm);

            dto.setFuelStatus("REFUEL_REQUIRED");
            dto.setMessage("Alerte carburant déclenchée. Le camion doit passer par une station.");
            dto.setDecisionReason(
                    "L'autonomie sécurisée restante ("
                            + round(autonomyKm)
                            + " km) est inférieure ou égale au seuil d'alerte ("
                            + fuelManagementService.getRefuelAlertAutonomyThresholdKm()
                            + " km), tout en restant suffisante pour rejoindre la station recommandée."
            );
            dto.setSuggestedInsertionOrder(suggestedOrder);
            dto.setRouteInsertionReason(
                    "Insertion recommandée avant l'arrêt numéro " + suggestedOrder
                            + " pour sécuriser la suite de la mission."
            );
            return dto;
        }

        dto.setFuelStatus("NORMAL");
        dto.setMessage("Niveau de carburant normal.");
        dto.setDecisionReason(
                "L'autonomie sécurisée (" + round(autonomyKm)
                        + " km) est supérieure au seuil d'alerte ("
                        + fuelManagementService.getRefuelAlertAutonomyThresholdKm()
                        + " km)."
        );
        return dto;
    }

    private boolean hasPlannedFuelStop(RoutePlan routePlan) {
        return routeStopRepository.existsByRoutePlanAndStopTypeAndStatus(
                routePlan,
                RouteStop.StopType.FUEL_STATION,
                RouteStop.StopStatus.PLANNED
        );
    }

    private int insertFuelStationStop(RoutePlan routePlan, FuelStation station) {
        List<RouteStop> existingStops = routeStopRepository.findByRoutePlanOrderByStopOrderAsc(routePlan);

        if (existingStops.isEmpty()) {
            throw new BadRequestException("No route stops found for route plan: " + routePlan.getId());
        }

        int insertionOrder = findBestFuelInsertionOrder(
                existingStops,
                fuelManagementService.calculateEstimatedAutonomyKm(routePlan.getTruck())
        );

        List<RouteStop> reordered = new ArrayList<>();
        boolean inserted = false;

        for (RouteStop stop : existingStops) {
            if (!inserted && stop.getStopOrder() != null && stop.getStopOrder() == insertionOrder) {
                RouteStop fuelStop = new RouteStop();
                fuelStop.setRoutePlan(routePlan);
                fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
                fuelStop.setFuelStation(station);
                fuelStop.setLat(station.getLat());
                fuelStop.setLng(station.getLng());
                fuelStop.setStatus(RouteStop.StopStatus.PLANNED);
                fuelStop.setNotes("Station-service ajoutée automatiquement : autonomie insuffisante pour terminer la mission en sécurité.");
                reordered.add(fuelStop);
                inserted = true;
            }

            RouteStop cloned = new RouteStop();
            cloned.setRoutePlan(routePlan);
            cloned.setStopType(stop.getStopType());
            cloned.setBin(stop.getBin());
            cloned.setFuelStation(stop.getFuelStation());
            cloned.setLat(stop.getLat());
            cloned.setLng(stop.getLng());
            cloned.setStatus(stop.getStatus());
            cloned.setNotes(stop.getNotes());
            reordered.add(cloned);
        }

        if (!inserted) {
            RouteStop fuelStop = new RouteStop();
            fuelStop.setRoutePlan(routePlan);
            fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
            fuelStop.setFuelStation(station);
            fuelStop.setLat(station.getLat());
            fuelStop.setLng(station.getLng());
            fuelStop.setStatus(RouteStop.StopStatus.PLANNED);
            fuelStop.setNotes("Station-service ajoutée automatiquement : autonomie insuffisante pour terminer la mission en sécurité.");
            reordered.add(fuelStop);
        }

        routeStopRepository.deleteAllInBatch(existingStops);
        routeStopRepository.flush();

        int order = 1;
        for (RouteStop stop : reordered) {
            stop.setStopOrder(order++);
        }

        routeStopRepository.saveAll(reordered);
        routeStopRepository.flush();

        for (RouteStop stop : reordered) {
            if (stop.getStopType() == RouteStop.StopType.FUEL_STATION) {
                return stop.getStopOrder();
            }
        }

        return insertionOrder;
    }

    private int estimateSuggestedInsertionOrder(RoutePlan routePlan, double safeAutonomyKm) {
        List<RouteStop> stops = routeStopRepository.findByRoutePlanOrderByStopOrderAsc(routePlan);
        if (stops.isEmpty()) {
            return 2;
        }
        return findBestFuelInsertionOrder(stops, safeAutonomyKm);
    }

    private int findBestFuelInsertionOrder(List<RouteStop> stops, double safeAutonomyKm) {
        if (stops.size() < 2) {
            return 2;
        }

        double cumulativeDistance = 0.0;
        double triggerDistance = Math.max(1.0, safeAutonomyKm - PREEMPTIVE_REFUEL_MARGIN_KM);

        for (int i = 1; i < stops.size(); i++) {
            RouteStop previous = stops.get(i - 1);
            RouteStop current = stops.get(i);

            double legDistance = haversineDistanceKm(
                    previous.getLat(),
                    previous.getLng(),
                    current.getLat(),
                    current.getLng()
            );

            cumulativeDistance += legDistance;

            if (cumulativeDistance >= triggerDistance) {
                return current.getStopOrder();
            }
        }

        return stops.get(stops.size() - 1).getStopOrder();
    }

    private MissionRouteStopDto mapRouteStopToDto(RouteStop stop) {
        MissionRouteStopDto dto = new MissionRouteStopDto();
        dto.setStopOrder(stop.getStopOrder());
        dto.setStopType(stop.getStopType() != null ? stop.getStopType().name() : null);
        dto.setBinId(stop.getBin() != null ? stop.getBin().getId() : null);
        dto.setFuelStationId(stop.getFuelStation() != null ? stop.getFuelStation().getId() : null);
        dto.setFuelStationName(stop.getFuelStation() != null ? stop.getFuelStation().getName() : null);
        dto.setLat(stop.getLat());
        dto.setLng(stop.getLng());
        return dto;
    }

    private List<RouteStop> normalizeAndValidateStops(List<RouteStop> input, List<String> warnings) {
        List<RouteStop> stops = input.stream()
                .sorted(Comparator.comparing(RouteStop::getStopOrder))
                .toList();

        RouteStop first = stops.get(0);
        RouteStop last = stops.get(stops.size() - 1);

        if (first.getStopType() == null || first.getStopType() != RouteStop.StopType.DEPOT_START) {
            warnings.add("First stop is not DEPOT_START");
        }

        if (last.getStopType() == null || last.getStopType() != RouteStop.StopType.DEPOT_RETURN) {
            warnings.add("Last stop is not DEPOT_RETURN");
        }

        long depotStartCount = stops.stream()
                .filter(s -> s.getStopType() == RouteStop.StopType.DEPOT_START)
                .count();

        long depotReturnCount = stops.stream()
                .filter(s -> s.getStopType() == RouteStop.StopType.DEPOT_RETURN)
                .count();

        if (depotStartCount != 1) {
            warnings.add("Expected exactly 1 DEPOT_START but found " + depotStartCount);
        }

        if (depotReturnCount != 1) {
            warnings.add("Expected exactly 1 DEPOT_RETURN but found " + depotReturnCount);
        }

        RouteStop anchor = stops.get(0);

        for (RouteStop stop : stops) {
            double distanceFromAnchorKm = haversineKm(anchor.getLat(), anchor.getLng(), stop.getLat(), stop.getLng());
            if (distanceFromAnchorKm > MAX_ALLOWED_DISTANCE_FROM_FIRST_POINT_KM) {
                warnings.add("Stop order " + stop.getStopOrder()
                        + " is geographically far from mission zone: " + round(distanceFromAnchorKm) + " km");
            }
        }

        for (int i = 0; i < stops.size() - 1; i++) {
            RouteStop a = stops.get(i);
            RouteStop b = stops.get(i + 1);
            double legKm = haversineKm(a.getLat(), a.getLng(), b.getLat(), b.getLng());
            if (legKm > MAX_ALLOWED_LEG_KM) {
                warnings.add("Abnormal leg detected between stop "
                        + a.getStopOrder() + " and " + b.getStopOrder()
                        + ": " + round(legKm) + " km");
            }
        }

        return stops;
    }

    private int findLastBinIndex(List<RouteStop> stops) {
        for (int i = stops.size() - 1; i >= 0; i--) {
            if (stops.get(i).getStopType() == RouteStop.StopType.BIN_PICKUP) {
                return i;
            }
        }
        return -1;
    }

    private OsrmRouteResult buildOsrmRoute(List<RouteStop> stops, List<String> warnings, String label) {
        OsrmRouteResult empty = new OsrmRouteResult();

        if (stops == null || stops.size() < 2) {
            return empty;
        }

        String coordinates = stops.stream()
                .filter(stop -> isValidCoordinatePair(stop.getLat(), stop.getLng()))
                .map(stop -> stop.getLng() + "," + stop.getLat())
                .reduce((a, b) -> a + ";" + b)
                .orElse(null);

        if (coordinates == null || coordinates.isBlank()) {
            warnings.add("OSRM " + label + " skipped because coordinates are empty/invalid.");
            return empty;
        }

        try {
            WebClient osrmClient = webClientBuilder
                    .baseUrl("http://localhost:5000")
                    .build();

            Map<String, Object> response = osrmClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/driving/" + coordinates)
                            .queryParam("overview", "full")
                            .queryParam("geometries", "geojson")
                            .queryParam("steps", "false")
                            .queryParam("annotations", "true")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseOsrmRouteResponse(response, warnings, label);

        } catch (Exception e) {
            warnings.add("OSRM " + label + " route failed: " + e.getMessage());
            return empty;
        }
    }

    private OsrmRouteResult parseOsrmRouteResponse(Map<String, Object> response, List<String> warnings, String label) {
        OsrmRouteResult result = new OsrmRouteResult();

        if (response == null) {
            warnings.add("OSRM " + label + " response is null");
            return result;
        }

        Object routesObj = response.get("routes");
        if (!(routesObj instanceof List<?> routes) || routes.isEmpty()) {
            warnings.add("OSRM " + label + " returned no routes");
            return result;
        }

        Object firstRouteObj = routes.get(0);
        if (!(firstRouteObj instanceof Map<?, ?> firstRoute)) {
            warnings.add("OSRM " + label + " first route is invalid");
            return result;
        }

        Object totalDistanceObj = firstRoute.get("distance");
        if (totalDistanceObj instanceof Number distanceMeters) {
            result.totalDistanceKm = round(distanceMeters.doubleValue() / 1000.0);
        }

        Object geometryObj = firstRoute.get("geometry");
        if (geometryObj instanceof Map<?, ?> geometry) {
            Object coordsObj = geometry.get("coordinates");
            if (coordsObj instanceof List<?> coordsList) {
                for (Object coordObj : coordsList) {
                    if (coordObj instanceof List<?> pair && pair.size() >= 2) {
                        Object lngObj = pair.get(0);
                        Object latObj = pair.get(1);

                        if (lngObj instanceof Number lng && latObj instanceof Number lat) {
                            result.routeCoordinates.add(new RouteCoordinateDto(lat.doubleValue(), lng.doubleValue()));
                        }
                    }
                }
            }
        }

        Object legsObj = firstRoute.get("legs");
        if (legsObj instanceof List<?> legsList) {
            for (Object legObj : legsList) {
                if (legObj instanceof Map<?, ?> legMap) {
                    Object distanceObj = legMap.get("distance");
                    if (distanceObj instanceof Number distanceMeters) {
                        result.legDistancesKm.add(round(distanceMeters.doubleValue() / 1000.0));
                    }
                }
            }
        }

        Object waypointsObj = response.get("waypoints");
        if (waypointsObj instanceof List<?> waypoints) {
            for (Object waypointObj : waypoints) {
                if (waypointObj instanceof Map<?, ?> waypoint) {
                    Object locationObj = waypoint.get("location");
                    if (locationObj instanceof List<?> pair && pair.size() >= 2) {
                        Object lngObj = pair.get(0);
                        Object latObj = pair.get(1);

                        if (lngObj instanceof Number lng && latObj instanceof Number lat) {
                            result.snappedWaypoints.add(new RouteCoordinateDto(lat.doubleValue(), lng.doubleValue()));
                        }
                    }
                }
            }
        }

        return result;
    }

    private double haversineKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Double.MAX_VALUE;
        }

        return haversineDistanceKm(lat1, lng1, lat2, lng2);
    }

    private double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private boolean isValidCoordinatePair(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180
                && !(lat == 0.0 && lng == 0.0);
    }

    private Mission getMissionOrThrow(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));
    }

    private RoutePlan getLatestRoutePlanOrThrow(Mission mission) {
        List<RoutePlan> plans = routePlanRepository.findByMissionOrderByCreatedAtDesc(mission);
        if (plans.isEmpty()) {
            throw new ResourceNotFoundException("No route plan found for mission: " + mission.getId());
        }
        return plans.get(0);
    }

    private RecommendedFuelStationDto mapRecommendedFuelStation(Truck truck, FuelStation station) {
        if (truck == null || station == null) {
            return null;
        }

        RecommendedFuelStationDto dto = new RecommendedFuelStationDto();
        dto.setTruckId(truck.getId());
        dto.setStationId(station.getId());
        dto.setStationName(station.getName());
        dto.setLat(station.getLat());
        dto.setLng(station.getLng());
        return dto;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private MissionResponse mapMissionToResponse(Mission mission) {
        MissionResponse dto = new MissionResponse();
        dto.setId(mission.getId());
        dto.setMissionCode(mission.getMissionCode());

        if (mission.getDriver() != null) {
            dto.setDriverId(mission.getDriver().getId());
            dto.setDriverName(mission.getDriver().getFullName());
        }

        if (mission.getZone() != null) {
            dto.setZoneId(mission.getZone().getId());
            dto.setZoneName(mission.getZone().getShapeName());
        }

        dto.setStatus(mission.getStatus());
        dto.setPriority(mission.getPriority());
        dto.setPlannedDate(mission.getPlannedDate());
        dto.setCreatedAt(mission.getCreatedAt());
        dto.setStartedAt(mission.getStartedAt());
        dto.setCompletedAt(mission.getCompletedAt());

        if (mission.getCreatedBy() != null) {
            dto.setCreatedByUserId(mission.getCreatedBy().getId());
        }

        dto.setNotes(mission.getNotes());
        return dto;
    }

    private MissionBinResponse mapMissionBinToResponse(MissionBin missionBin) {
        MissionBinResponse dto = new MissionBinResponse();

        dto.setId(missionBin.getId());

        if (missionBin.getMission() != null) {
            dto.setMissionId(missionBin.getMission().getId());
        }

        if (missionBin.getBin() != null) {
            dto.setBinId(missionBin.getBin().getId());
            dto.setBinCode(missionBin.getBin().getBinCode());
            dto.setLat(missionBin.getBin().getLat());
            dto.setLng(missionBin.getBin().getLng());
        }

        dto.setVisitOrder(missionBin.getVisitOrder());
        dto.setTargetFillThreshold(missionBin.getTargetFillThreshold());
        dto.setAssignedReason(missionBin.getAssignedReason());
        dto.setCollected(missionBin.isCollected());
        dto.setCollectedAt(missionBin.getCollectedAt());

        if (missionBin.getCollectedBy() != null) {
            dto.setCollectedByDriverId(missionBin.getCollectedBy().getId());
            dto.setCollectedByDriverName(missionBin.getCollectedBy().getFullName());
        }

        dto.setDriverNote(missionBin.getDriverNote());
        dto.setIssueType(missionBin.getIssueType());
        dto.setPhotoUrl(missionBin.getPhotoUrl());

        if (missionBin.getAssignmentStatus() != null) {
            dto.setAssignmentStatus(missionBin.getAssignmentStatus().name());
        }

        if (missionBin.getReassignedFromTruck() != null) {
            dto.setReassignedFromTruckId(missionBin.getReassignedFromTruck().getId());
        }

        if (missionBin.getReassignedToTruck() != null) {
            dto.setReassignedToTruckId(missionBin.getReassignedToTruck().getId());
        }

        dto.setPlannedArrival(missionBin.getPlannedArrival());
        dto.setActualArrival(missionBin.getActualArrival());
        dto.setSkippedReason(missionBin.getSkippedReason());

        return dto;
    }

    private static class OsrmRouteResult {
        private final List<RouteCoordinateDto> routeCoordinates = new ArrayList<>();
        private final List<RouteCoordinateDto> snappedWaypoints = new ArrayList<>();
        private final List<Double> legDistancesKm = new ArrayList<>();
        private Double totalDistanceKm;
    }
}