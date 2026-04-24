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

        List<MissionBin> remainingBins = missionBinRepository.findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(missionId);
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

        long remaining = missionBinRepository.findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(missionId).size();
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
        }

        autoInsertRefuelStopIfNeeded(mission);
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
        if (lastBinIndex < 1) {
            warnings.add("No BIN_PICKUP found in route. Transfer route will fallback to empty.");
        }

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

        debugMissionRoute(missionId, canonicalStops, fullRoute, collectionRoute, transferRoute, warnings);
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
            status.setMessage("Une station-service planifiée existe déjà.");
            status.setDecisionReason("Un arrêt carburant est déjà présent dans le plan de route.");
            return status;
        }

        Truck truck = latestPlan.getTruck();
        if (truck == null) {
            throw new BadRequestException("Route plan has no truck");
        }

        FuelStation station = fuelStationService.findNearestCompatibleStation(truck);
        if (station == null) {
            throw new BadRequestException("No compatible fuel station found");
        }

        int maxOrder = routeStopRepository.findByRoutePlanOrderByStopOrderAsc(latestPlan)
                .stream()
                .map(RouteStop::getStopOrder)
                .filter(o -> o != null)
                .max(Integer::compareTo)
                .orElse(0);

        RouteStop fuelStop = new RouteStop();
        fuelStop.setRoutePlan(latestPlan);
        fuelStop.setStopOrder(maxOrder);
        fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
        fuelStop.setFuelStation(station);
        fuelStop.setLat(station.getLat());
        fuelStop.setLng(station.getLng());
        fuelStop.setStatus(RouteStop.StopStatus.PLANNED);
        fuelStop.setNotes("Inserted fuel stop");
        routeStopRepository.save(fuelStop);

        status.setRefuelStopInserted(Boolean.TRUE);
        status.setSuggestedInsertionOrder(maxOrder);
        status.setRouteInsertionReason("Insertion en fin de tournée planifiée.");
        status.setMessage("Station-service ajoutée avec succès.");
        status.setDecisionReason("Le niveau carburant est faible, une insertion préventive a été réalisée.");
        return status;
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
                            .queryParam("annotations", "false")
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

    private void debugMissionRoute(
            Long missionId,
            List<RouteStop> stops,
            OsrmRouteResult full,
            OsrmRouteResult collection,
            OsrmRouteResult transfer,
            List<String> warnings
    ) {
        System.out.println("==== MISSION ROUTE DEBUG ====");
        System.out.println("missionId=" + missionId);

        for (RouteStop stop : stops) {
            System.out.println(
                    "STOP order=" + stop.getStopOrder()
                            + ", type=" + (stop.getStopType() != null ? stop.getStopType().name() : "null")
                            + ", binId=" + (stop.getBin() != null ? stop.getBin().getId() : null)
                            + ", fuelStationId=" + (stop.getFuelStation() != null ? stop.getFuelStation().getId() : null)
                            + ", lat=" + stop.getLat()
                            + ", lng=" + stop.getLng()
            );
        }

        System.out.println("FULL route distanceKm=" + full.totalDistanceKm
                + ", waypoints=" + full.snappedWaypoints.size()
                + ", geometryPoints=" + full.routeCoordinates.size());

        System.out.println("COLLECTION distanceKm=" + collection.totalDistanceKm
                + ", waypoints=" + collection.snappedWaypoints.size()
                + ", geometryPoints=" + collection.routeCoordinates.size());

        System.out.println("TRANSFER distanceKm=" + transfer.totalDistanceKm
                + ", waypoints=" + transfer.snappedWaypoints.size()
                + ", geometryPoints=" + transfer.routeCoordinates.size());

        if (!warnings.isEmpty()) {
            for (String warning : warnings) {
                System.out.println("ROUTE WARNING: " + warning);
            }
        }

        System.out.println("============================");
    }

    private double haversineKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Double.MAX_VALUE;
        }

        final double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private boolean isValidCoordinatePair(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180
                && !(lat == 0.0 && lng == 0.0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class OsrmRouteResult {
        private final List<RouteCoordinateDto> routeCoordinates = new ArrayList<>();
        private final List<RouteCoordinateDto> snappedWaypoints = new ArrayList<>();
        private final List<Double> legDistancesKm = new ArrayList<>();
        private Double totalDistanceKm;
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

    private MissionFuelStatusResponse buildMissionFuelStatus(Mission mission, RoutePlan latestPlan) {
        MissionFuelStatusResponse response = new MissionFuelStatusResponse();

        Truck truck = latestPlan.getTruck();
        if (truck == null) {
            response.setFuelStatus("UNKNOWN");
            response.setMessage("Camion introuvable pour la mission.");
            return response;
        }

        double autonomyKm = fuelManagementService.calculateEstimatedAutonomyKm(truck);

        response.setMissionId(mission.getId());
        response.setTruckId(truck.getId());
        response.setSafeAutonomyKm(autonomyKm);
        response.setRefuelStopInserted(Boolean.FALSE);

        Double plannedDistanceKm = latestPlan.getTotalDistanceKm() != null
                ? latestPlan.getTotalDistanceKm().doubleValue()
                : null;

        if (plannedDistanceKm != null && autonomyKm < plannedDistanceKm) {
            response.setFuelStatus("LOW");
            response.setMessage("Autonomie insuffisante pour couvrir la mission complète.");
            response.setDecisionReason("Le camion peut rouler, mais son autonomie sécurisée est inférieure à la distance planifiée.");
            response.setTriggerDistanceKm(round(plannedDistanceKm));
        } else {
            response.setFuelStatus("NORMAL");
            response.setMessage("Autonomie suffisante.");
            response.setDecisionReason("Le camion dispose d'une autonomie sécurisée suffisante.");
            if (plannedDistanceKm != null) {
                response.setTriggerDistanceKm(round(plannedDistanceKm));
            }
        }

        if (autonomyKm <= 0.0) {
            response.setFuelStatus("CRITICAL");
            response.setMessage("Autonomie nulle ou invalide.");
            response.setDecisionReason("Le calcul d'autonomie retourné est nul ou invalide.");
        }

        FuelStation station = fuelStationService.findNearestCompatibleStation(truck);
        if (station != null) {
            RecommendedFuelStationDto dto = new RecommendedFuelStationDto();
            dto.setTruckId(truck.getId());
            dto.setStationId(station.getId());
            dto.setStationName(station.getName());
            dto.setLat(station.getLat());
            dto.setLng(station.getLng());
            response.setRecommendedFuelStation(dto);
        }

        return response;
    }

    private boolean hasPlannedFuelStop(RoutePlan routePlan) {
        return routeStopRepository.findByRoutePlanOrderByStopOrderAsc(routePlan)
                .stream()
                .anyMatch(stop -> stop.getStopType() == RouteStop.StopType.FUEL_STATION);
    }

    private void autoInsertRefuelStopIfNeeded(Mission mission) {
        try {
            RoutePlan latestPlan = getLatestRoutePlanOrThrow(mission);
            Truck truck = latestPlan.getTruck();
            if (truck == null) {
                return;
            }

            double autonomyKm = fuelManagementService.calculateEstimatedAutonomyKm(truck);
            double plannedDistanceKm = latestPlan.getTotalDistanceKm() != null
                    ? latestPlan.getTotalDistanceKm().doubleValue()
                    : 0.0;

            if (plannedDistanceKm <= 0) {
                return;
            }

            if (autonomyKm >= plannedDistanceKm + PREEMPTIVE_REFUEL_MARGIN_KM) {
                return;
            }

            if (hasPlannedFuelStop(latestPlan)) {
                return;
            }

            FuelStation station = fuelStationService.findNearestCompatibleStation(truck);
            if (station == null) {
                return;
            }

            int maxOrder = routeStopRepository.findByRoutePlanOrderByStopOrderAsc(latestPlan)
                    .stream()
                    .map(RouteStop::getStopOrder)
                    .filter(o -> o != null)
                    .max(Integer::compareTo)
                    .orElse(0);

            if (autonomyKm < MIN_DISTANCE_BUFFER_TO_STATION_KM) {
                return;
            }

            RouteStop fuelStop = new RouteStop();
            fuelStop.setRoutePlan(latestPlan);
            fuelStop.setStopOrder(maxOrder);
            fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
            fuelStop.setFuelStation(station);
            fuelStop.setLat(station.getLat());
            fuelStop.setLng(station.getLng());
            fuelStop.setStatus(RouteStop.StopStatus.PLANNED);
            fuelStop.setNotes("Auto-inserted fuel stop due to low autonomy");
            routeStopRepository.save(fuelStop);

            System.out.println("AUTO REFUEL STOP INSERTED => missionId=" + mission.getId()
                    + ", truckId=" + truck.getId()
                    + ", stationId=" + station.getId()
                    + ", autonomyKm=" + autonomyKm
                    + ", plannedDistanceKm=" + plannedDistanceKm);
        } catch (Exception e) {
            System.out.println("AUTO REFUEL INSERT FAILED => " + e.getMessage());
        }
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
        }

        dto.setVisitOrder(missionBin.getVisitOrder());
        dto.setAssignedReason(missionBin.getAssignedReason());
        dto.setCollected(missionBin.isCollected());
        dto.setCollectedAt(missionBin.getCollectedAt());
        dto.setDriverNote(missionBin.getDriverNote());
        dto.setIssueType(missionBin.getIssueType());
        dto.setPhotoUrl(missionBin.getPhotoUrl());

        if (missionBin.getCollectedBy() != null) {
            dto.setCollectedByDriverId(missionBin.getCollectedBy().getId());
            dto.setCollectedByDriverName(missionBin.getCollectedBy().getFullName());
        }

        return dto;
    }
}