package com.example.demo.service;


import com.example.demo.entity.Alert;
import com.example.demo.repository.AlertRepository;

import java.lang.reflect.Method;
import java.util.Collection;
import com.example.demo.entity.TruckLocation;

import com.example.demo.repository.TruckLocationRepository;
import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.ReplanRequestDto;
import com.example.demo.dto.routing.RoutingDisposalSiteDto;
import com.example.demo.dto.routing.RoutingMissionDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingResponseDto;
import com.example.demo.dto.routing.RoutingStopDto;
import com.example.demo.entity.Bin;
import com.example.demo.entity.Depot;
import com.example.demo.entity.Driver;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.MissionReassignment;
import com.example.demo.entity.RoutePlan;
import com.example.demo.entity.RouteStop;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckIncident;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.DepotRepository;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.MissionReassignmentRepository;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.RoutePlanRepository;
import com.example.demo.repository.RouteStopRepository;
import com.example.demo.repository.TruckIncidentRepository;
import com.example.demo.repository.TruckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Propagation;
@Service
public class DynamicReplanningServiceImpl implements DynamicReplanningService {

    private static final String REPLAN_ALGORITHM_VERSION = "v4.3";
    private static final double MAX_URGENT_DISTANCE_INCREASE_RATIO = 0.80; // +80%
    private static final double MAX_URGENT_DURATION_INCREASE_RATIO = 1.20; // +120%
    private static final double MAX_URGENT_DISTANCE_INCREASE_KM = 6.0;
    private static final int MAX_URGENT_DURATION_INCREASE_MIN = 35;

    private static final Set<String> ALLOWED_INCIDENT_TYPES = Set.of(
            "BREAKDOWN",
            "TRAFFIC",
            "TRAFFIC_BLOCK",
            "FUEL_LOW",
            "REFUEL_REQUIRED",
            "DELAY",
            "MANUAL",
            "OTHER"
    );

    private final MissionRepository missionRepository;
    private final MissionBinRepository missionBinRepository;
    private final MissionReassignmentRepository missionReassignmentRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteStopRepository routeStopRepository;
    private final TruckRepository truckRepository;
    private final TruckIncidentRepository truckIncidentRepository;
    private final DepotRepository depotRepository;
    private final DriverRepository driverRepository;
    private final RoutingPayloadBuilderService routingPayloadBuilderService;
    private final PythonRoutingClient pythonRoutingClient;
    private final BinRepository binRepository;
    private final TruckLocationRepository truckLocationRepository;
    private final AlertRepository alertRepository;
    private final MissionRealtimeService missionRealtimeService;

    public DynamicReplanningServiceImpl(MissionRepository missionRepository,
                                        MissionBinRepository missionBinRepository,
                                        MissionReassignmentRepository missionReassignmentRepository,
                                        RoutePlanRepository routePlanRepository,
                                        RouteStopRepository routeStopRepository,
                                        TruckRepository truckRepository,
                                        TruckIncidentRepository truckIncidentRepository,
                                        DepotRepository depotRepository,
                                        DriverRepository driverRepository,
                                        RoutingPayloadBuilderService routingPayloadBuilderService,
                                        PythonRoutingClient pythonRoutingClient,
                                        BinRepository binRepository,
                                        TruckLocationRepository truckLocationRepository,
                                        AlertRepository alertRepository,
                                        MissionRealtimeService missionRealtimeService
                                        
    		) {
        this.missionRepository = missionRepository;
        this.missionBinRepository = missionBinRepository;
        this.missionReassignmentRepository = missionReassignmentRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeStopRepository = routeStopRepository;
        this.truckRepository = truckRepository;
        this.truckIncidentRepository = truckIncidentRepository;
        this.depotRepository = depotRepository;
        this.driverRepository = driverRepository;
        this.routingPayloadBuilderService = routingPayloadBuilderService;
        this.pythonRoutingClient = pythonRoutingClient;
        this.binRepository = binRepository;
        this.truckLocationRepository = truckLocationRepository;
        this.alertRepository = alertRepository;
        this.missionRealtimeService = missionRealtimeService;
    }

    @Override
    @Transactional
    public void handleUrgentBin(Long binId, Long telemetryId) {
        if (binId == null) {
            System.out.println("URGENT BIN IGNORED => binId is null");
            return;
        }

        Bin bin = binRepository.findById(binId)
                .orElseThrow(() -> new ResourceNotFoundException("Urgent bin not found: " + binId));

        if (bin.getZone() == null) {
            System.out.println("URGENT BIN IGNORED => bin has no zone, binId=" + binId);
            return;
        }

        String urgentWasteType = extractWasteTypeFromBin(bin);

        if (urgentWasteType == null || urgentWasteType.isBlank()) {
            System.out.println("URGENT BIN IGNORED => wasteType missing, binId=" + binId);
            return;
        }

        boolean alreadyAssignedToActiveMission = missionBinRepository
                .findByBinIdOrderByIdDesc(binId)
                .stream()
                .anyMatch(mb ->
                        mb.getMission() != null
                                && mb.getMission().getStatus() != null
                                && List.of("CREATED", "PLANNED", "IN_PROGRESS").contains(mb.getMission().getStatus().toUpperCase())
                                && !mb.isCollected()
                                && mb.getAssignmentStatus() != MissionBin.AssignmentStatus.CANCELLED
                );

        if (alreadyAssignedToActiveMission) {
            System.out.println("URGENT BIN IGNORED => already assigned to active mission, binId=" + binId);
            return;
        }

        Mission mission = findCompatibleActiveMissionForUrgentBin(bin, urgentWasteType);

        if (mission == null) {
            System.out.println(
                    "URGENT BIN => no compatible active mission found. No new mission created. binId="
                            + binId
                            + ", wasteType="
                            + urgentWasteType
            );

            markOpenBinAlertsNotInserted(bin);
            return;
        }

        validateMissionEligibleForReplan(mission);

        if (mission.getTruck() == null || !isTruckCompatibleWithWasteType(mission.getTruck(), urgentWasteType)) {
            System.out.println(
                    "URGENT BIN IGNORED => mission truck not compatible, missionId="
                            + mission.getId()
                            + ", truckId="
                            + (mission.getTruck() != null ? mission.getTruck().getId() : null)
                            + ", binWasteType="
                            + urgentWasteType
            );

            markOpenBinAlertsNotInserted(bin);
            return;
        }

        boolean alreadyAssigned = missionBinRepository.existsByMissionIdAndBinId(mission.getId(), binId);

        if (alreadyAssigned) {
            System.out.println("URGENT BIN IGNORED => already assigned to missionId=" + mission.getId());
            return;
        }

        MissionBin urgentMissionBin = new MissionBin();
        urgentMissionBin.setMission(mission);
        urgentMissionBin.setBin(bin);
        urgentMissionBin.setVisitOrder(9999);
        urgentMissionBin.setCollected(false);
        urgentMissionBin.setAssignedReason("THRESHOLD");
        urgentMissionBin.setAssignmentStatus(MissionBin.AssignmentStatus.PLANNED);

        missionBinRepository.save(urgentMissionBin);

        List<MissionBin> remainingBins =
                missionBinRepository.findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(mission.getId());

        RoutingRequestDto routingRequest = buildUrgentPartialReplanRequest(mission, remainingBins);

        RoutingResponseDto routingResponse = pythonRoutingClient.optimizeRoutes(routingRequest);

        if (routingResponse == null
                || routingResponse.getMissions() == null
                || routingResponse.getMissions().isEmpty()) {

            missionBinRepository.delete(urgentMissionBin);
            markOpenBinAlertsNotInserted(bin);

            throw new BadRequestException("Urgent bin routing failed for binId=" + binId);
        }

        RoutingMissionDto routingMission = routingResponse.getMissions().get(0);

        boolean urgentBinServed = routingMission.getStops() != null
                && routingMission.getStops()
                .stream()
                .anyMatch(stop -> binId.equals(stop.getBinId()));

        if (!urgentBinServed) {
            missionBinRepository.delete(urgentMissionBin);
            markOpenBinAlertsNotInserted(bin);

            throw new BadRequestException(
                    "Urgent bin was not accepted by routing engine, rollback applied. binId=" + binId
            );
        }

        validateUrgentReplanCostAcceptable(mission, routingMission, urgentMissionBin, binId);

        updateMissionBinVisitOrders(mission, routingMission);

        replaceCurrentRoutePlanForUrgentBin(
                mission,
                mission.getTruck(),
                mission.getDepot() != null ? mission.getDepot() : resolveActiveDepot(),
                routingMission,
                routingRequest
        );

        mission.setMissionStatusDetail(Mission.MissionStatusDetail.REPLANNED);
        mission.setReplannedCount((mission.getReplannedCount() != null ? mission.getReplannedCount() : 0) + 1);
        missionRepository.save(mission);

        markOpenBinAlertsInsertedInMission(bin, mission);

        missionRealtimeService.publishMissionReplanned(mission, null);
        missionRealtimeService.publishMissionBinUpdated(
                urgentMissionBin,
                "MISSION_URGENT_BIN_INSERTED"
        );

       

        System.out.println(
                "✅ URGENT BIN INSERTED INTO EXISTING MISSION => binId="
                        + binId
                        + ", telemetryId="
                        + telemetryId
                        + ", missionId="
                        + mission.getId()
                        + ", truckId="
                        + mission.getTruck().getId()
                        + ", wasteType="
                        + urgentWasteType
        );
    }
    
    
    private Mission findCompatibleActiveMissionForUrgentBin(Bin bin, String wasteType) {
        if (bin == null || bin.getZone() == null || wasteType == null) {
            return null;
        }

        List<String> activeStatuses = List.of("IN_PROGRESS", "CREATED", "PLANNED");

        return missionRepository.findByZone(bin.getZone())
                .stream()
                .filter(m -> m.getStatus() != null)
                .filter(m -> activeStatuses.contains(m.getStatus().toUpperCase()))
                .filter(m -> m.getTruck() != null)
                .filter(m -> isTruckCompatibleWithWasteType(m.getTruck(), wasteType))
                .filter(m -> {
                    try {
                        validateMissionEligibleForReplan(m);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }

    private String extractWasteTypeFromBin(Bin bin) {
        if (bin == null) {
            return null;
        }

        Object value = invokeNoArg(bin, "getWasteType");

        if (value == null) {
            return null;
        }

        if (value instanceof Enum<?> enumValue) {
            return enumValue.name().trim().toUpperCase();
        }

        return String.valueOf(value).trim().toUpperCase();
    }

    private boolean isTruckCompatibleWithWasteType(Truck truck, String wasteType) {
        if (truck == null || wasteType == null || wasteType.isBlank()) {
            return false;
        }

        String normalizedWasteType = wasteType.trim().toUpperCase();

        Object supportedWasteTypes = invokeNoArg(truck, "getSupportedWasteTypes");

        if (supportedWasteTypes instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(v -> v != null)
                    .map(this::normalizeWasteTypeValue)
                    .anyMatch(normalizedWasteType::equals);
        }

        Object singleWasteType = invokeNoArg(truck, "getWasteType");

        if (singleWasteType != null) {
            return normalizedWasteType.equals(normalizeWasteTypeValue(singleWasteType));
        }

        Object wasteTypes = invokeNoArg(truck, "getWasteTypes");

        if (wasteTypes instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(v -> v != null)
                    .map(this::normalizeWasteTypeValue)
                    .anyMatch(normalizedWasteType::equals);
        }

        Object supportedWasteType = invokeNoArg(truck, "getSupportedWasteType");

        if (supportedWasteType != null) {
            return normalizedWasteType.equals(normalizeWasteTypeValue(supportedWasteType));
        }

        return false;
    }

    private String normalizeWasteTypeValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Enum<?> enumValue) {
            return enumValue.name().trim().toUpperCase();
        }

        return String.valueOf(value).trim().toUpperCase();
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private void markOpenBinAlertsInsertedInMission(Bin bin, Mission mission) {
        if (bin == null || mission == null || mission.getId() == null) {
            return;
        }

        List<Alert> alerts = alertRepository.findByBinIdAndResolvedFalseWithRelations(bin.getId());

        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        Alert mainAlert = alerts.stream()
                .filter(a -> a.getAlertType() != null)
                .filter(a -> "BIN_FULL".equalsIgnoreCase(a.getAlertType()))
                .findFirst()
                .orElse(null);

        if (mainAlert == null) {
            mainAlert = alerts.stream()
                    .filter(a -> a.getAlertType() != null)
                    .filter(a -> "BIN_SUDDEN_FILL".equalsIgnoreCase(a.getAlertType()))
                    .findFirst()
                    .orElse(null);
        }

        if (mainAlert == null) {
            mainAlert = alerts.stream()
                    .filter(a -> a.getAlertType() != null)
                    .filter(a -> "BIN_ALMOST_FULL".equalsIgnoreCase(a.getAlertType()))
                    .findFirst()
                    .orElse(null);
        }

        if (mainAlert != null) {
            String missionCode = mission.getMissionCode() != null ? mission.getMissionCode() : ("#" + mission.getId());
            String truckCode = mission.getTruck() != null && mission.getTruck().getTruckCode() != null
                    ? mission.getTruck().getTruckCode()
                    : "—";

            mainAlert.setMission(mission);
            mainAlert.setEntityType("MISSION");
            mainAlert.setEntityId(mission.getId());
            mainAlert.setActionType("IN_MISSION");

            mainAlert.setTitle("Bac intégré automatiquement dans une mission");

            mainAlert.setRecommendation(
                    "Le bac urgent a été intégré automatiquement dans la mission "
                            + missionCode
                            + " (camion "
                            + truckCode
                            + ")."
            );

            mainAlert.setMessage(
                    "Le bac "
                            + bin.getBinCode()
                            + " est devenu urgent et a été intégré automatiquement dans la mission "
                            + missionCode
                            + "."
            );

            alertRepository.save(mainAlert);
        }

        for (Alert alert : alerts) {
            if (mainAlert != null && alert.getId().equals(mainAlert.getId())) {
                continue;
            }

            if (!isSecondaryCollectionAlert(alert)) {
                continue;
            }

            alert.setResolved(true);
            alert.setResolvedAt(Instant.now());
            alert.setActionType("AUTO_CLOSED");
            alert.setRecommendation(
                    "Cette alerte a été clôturée automatiquement car le bac a déjà été intégré dans une mission active."
            );

            alertRepository.save(alert);
        }

        alertRepository.flush();
    }
    
    private boolean isSecondaryCollectionAlert(Alert alert) {
        if (alert == null || alert.getAlertType() == null) {
            return false;
        }

        String type = alert.getAlertType().trim().toUpperCase();

        return type.equals("BIN_FAST_FILLING")
                || type.equals("NEED_EXTRA_BIN_NEARBY")
                || type.equals("BIN_ALMOST_FULL")
                || type.equals("THRESHOLD");
    }

    private void markOpenBinAlertsNotInserted(Bin bin) {
        if (bin == null) {
            return;
        }

        List<Alert> alerts = alertRepository.findByBinIdAndResolvedFalseWithRelations(bin.getId());

        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        for (Alert alert : alerts) {
            if (!isUrgentBinAlert(alert)) {
                continue;
            }

            alert.setActionType("WAITING_MISSION");
            alert.setRecommendation(
                    "Aucune mission active compatible n’a été trouvée pour ce bac. "
                            + "La municipalité doit lancer une génération ou attendre une mission compatible."
            );

            alertRepository.save(alert);
        }

        alertRepository.flush();
    }

    private boolean isUrgentBinAlert(Alert alert) {
        if (alert == null || alert.getAlertType() == null) {
            return false;
        }

        String type = alert.getAlertType().trim().toUpperCase();

        return type.equals("BIN_FULL")
                || type.equals("BIN_SUDDEN_FILL")
                || type.equals("BIN_ALMOST_FULL");
    }
    
    private RoutingRequestDto buildUrgentPartialReplanRequest(Mission mission, List<MissionBin> remainingBins) {
        Double startLat = null;
        Double startLng = null;

        try {
            if (mission.getDriver() != null && mission.getDriver().getId() != null) {
                TruckLocation latestLocation = truckLocationRepository
                        .findLatestByDriverId(mission.getDriver().getId())
                        .orElse(null);

                if (latestLocation != null) {
                    startLat = latestLocation.getLat();
                    startLng = latestLocation.getLng();
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load latest truck location for partial replan: " + e.getMessage());
        }

        return routingPayloadBuilderService.buildReplanRequestFromStart(
                List.of(mission.getTruck()),
                remainingBins,
                startLat,
                startLng
        );
    }
    
    
    
    
    private void validateUrgentReplanCostAcceptable(
            Mission mission,
            RoutingMissionDto routingMission,
            MissionBin urgentMissionBin,
            Long binId
    ) {
        if (mission == null || routingMission == null) {
            return;
        }

        List<RoutePlan> existingPlans = routePlanRepository.findByMissionOrderByCreatedAtDesc(mission);

        if (existingPlans == null || existingPlans.isEmpty()) {
            return;
        }

        RoutePlan currentPlan = existingPlans.get(0);

        if (currentPlan.getTotalDistanceKm() == null
                || currentPlan.getEstimatedDurationMin() == null
                || routingMission.getTotalDistanceKm() == null
                || routingMission.getTotalDurationMinutes() == null) {
            return;
        }

        double oldDistanceKm = currentPlan.getTotalDistanceKm().doubleValue();
        int oldDurationMin = currentPlan.getEstimatedDurationMin();

        double newDistanceKm = routingMission.getTotalDistanceKm();
        int newDurationMin = (int) Math.round(routingMission.getTotalDurationMinutes());

        double distanceIncreaseKm = newDistanceKm - oldDistanceKm;
        int durationIncreaseMin = newDurationMin - oldDurationMin;

        boolean distanceTooHigh =
                distanceIncreaseKm > MAX_URGENT_DISTANCE_INCREASE_KM
                        && newDistanceKm > oldDistanceKm * (1.0 + MAX_URGENT_DISTANCE_INCREASE_RATIO);

        boolean durationTooHigh =
                durationIncreaseMin > MAX_URGENT_DURATION_INCREASE_MIN
                        && newDurationMin > oldDurationMin * (1.0 + MAX_URGENT_DURATION_INCREASE_RATIO);

        if (distanceTooHigh || durationTooHigh) {
            missionBinRepository.delete(urgentMissionBin);

            System.out.println(
                    "URGENT BIN REPLAN REJECTED BY COST => binId=" + binId
                            + ", oldDistanceKm=" + oldDistanceKm
                            + ", newDistanceKm=" + newDistanceKm
                            + ", oldDurationMin=" + oldDurationMin
                            + ", newDurationMin=" + newDurationMin
            );

            throw new BadRequestException(
                    "Urgent bin replan rejected because route cost increase is too high. binId=" + binId
            );
        }

        System.out.println(
                "URGENT BIN REPLAN COST ACCEPTED => binId=" + binId
                        + ", oldDistanceKm=" + oldDistanceKm
                        + ", newDistanceKm=" + newDistanceKm
                        + ", oldDurationMin=" + oldDurationMin
                        + ", newDurationMin=" + newDurationMin
        );
    }

    private void updateMissionBinVisitOrders(Mission mission, RoutingMissionDto routingMission) {
        if (mission == null || routingMission == null || routingMission.getStops() == null) {
            return;
        }

        int order = 1;

        for (RoutingStopDto stop : routingMission.getStops()) {
            if (stop == null || stop.getBinId() == null) {
                continue;
            }

            MissionBin missionBin = missionBinRepository
                    .findByMissionIdAndBinId(mission.getId(), stop.getBinId())
                    .orElse(null);

            if (missionBin == null) {
                continue;
            }

            missionBin.setVisitOrder(order++);
            missionBinRepository.save(missionBin);
        }
    }

    private void replaceCurrentRoutePlanForUrgentBin(Mission mission,
                                                     Truck truck,
                                                     Depot depot,
                                                     RoutingMissionDto routingMissionDto,
                                                     RoutingRequestDto routingRequest) {
        List<RoutePlan> existingPlans = routePlanRepository.findByMissionOrderByCreatedAtDesc(mission);

        if (!existingPlans.isEmpty()) {
            RoutePlan latestPlan = existingPlans.get(0);
            latestPlan.setPlanStatus(RoutePlan.PlanStatus.REPLACED);
            routePlanRepository.save(latestPlan);
        }

        RoutePlan routePlan = new RoutePlan();
        routePlan.setMission(mission);
        routePlan.setTruck(truck);
        routePlan.setDepot(depot);
        routePlan.setPlanType(RoutePlan.PlanType.REPLANNED);
        routePlan.setPlanStatus(RoutePlan.PlanStatus.PLANNED);
        routePlan.setOptimizationAlgorithm("OR_TOOLS_OSRM_URGENT_BIN");
        routePlan.setOptimizationVersion(REPLAN_ALGORITHM_VERSION);
        routePlan.setTrafficMode(RoutePlan.TrafficMode.REAL);

        if (routingMissionDto.getTotalDistanceKm() != null) {
            routePlan.setTotalDistanceKm(BigDecimal.valueOf(routingMissionDto.getTotalDistanceKm()));
        }

        if (routingMissionDto.getTotalDurationMinutes() != null) {
            routePlan.setEstimatedDurationMin((int) Math.round(routingMissionDto.getTotalDurationMinutes()));
        }

        RoutePlan savedRoutePlan = routePlanRepository.save(routePlan);

        int stopOrder = 1;

        RouteStop depotStart = new RouteStop();
        depotStart.setRoutePlan(savedRoutePlan);
        depotStart.setStopOrder(stopOrder++);
        depotStart.setStopType(RouteStop.StopType.DEPOT_START);
        depotStart.setLat(resolveDepotLat(routingRequest, depot));
        depotStart.setLng(resolveDepotLng(routingRequest, depot));
        depotStart.setStatus(RouteStop.StopStatus.PLANNED);
        depotStart.setNotes("Départ après injection d'un bac urgent");
        routeStopRepository.save(depotStart);

        if (routingMissionDto.getStops() != null) {
            for (RoutingStopDto stopDto : routingMissionDto.getStops()) {
                if (stopDto == null) {
                    continue;
                }

                String stopType = stopDto.getStopType() != null
                        ? stopDto.getStopType().trim().toUpperCase()
                        : "BIN_PICKUP";

                if ("BIN_PICKUP".equals(stopType)) {
                    if (stopDto.getBinId() == null) {
                        continue;
                    }

                    MissionBin missionBin = missionBinRepository
                            .findByMissionIdAndBinId(mission.getId(), stopDto.getBinId())
                            .orElse(null);

                    if (missionBin == null || missionBin.getBin() == null) {
                        continue;
                    }

                    Bin stopBin = missionBin.getBin();

                    RouteStop routeStop = new RouteStop();
                    routeStop.setRoutePlan(savedRoutePlan);
                    routeStop.setStopOrder(stopOrder++);
                    routeStop.setStopType(RouteStop.StopType.BIN_PICKUP);
                    routeStop.setBin(stopBin);
                    routeStop.setLat(resolveBinRoutingLat(stopBin));
                    routeStop.setLng(resolveBinRoutingLng(stopBin));
                    routeStop.setStatus(RouteStop.StopStatus.PLANNED);
                    routeStop.setNotes("Collecte après injection temps réel d'un bac urgent");
                    routeStopRepository.save(routeStop);

                    continue;
                }

                if ("DISPOSAL_SITE".equals(stopType)) {
                    if (stopDto.getDisposalSiteId() == null) {
                        continue;
                    }

                    RoutingDisposalSiteDto disposalSite = null;

                    if (routingRequest != null && routingRequest.getDisposalSites() != null) {
                        disposalSite = routingRequest.getDisposalSites()
                                .stream()
                                .filter(site -> stopDto.getDisposalSiteId().equals(site.getId()))
                                .findFirst()
                                .orElse(null);
                    }

                    if (disposalSite == null) {
                        throw new BadRequestException(
                                "Disposal site not found in urgent bin routing request: "
                                        + stopDto.getDisposalSiteId()
                        );
                    }

                    RouteStop disposalStop = new RouteStop();
                    disposalStop.setRoutePlan(savedRoutePlan);
                    disposalStop.setStopOrder(stopOrder++);
                    disposalStop.setStopType(RouteStop.StopType.DISPOSAL_SITE);
                    disposalStop.setDisposalSiteId(disposalSite.getId());
                    disposalStop.setDisposalSiteName(disposalSite.getName());
                    disposalStop.setLat(disposalSite.getLat());
                    disposalStop.setLng(disposalSite.getLng());
                    disposalStop.setStatus(RouteStop.StopStatus.PLANNED);
                    disposalStop.setNotes("Déchargement pendant route mise à jour temps réel");
                    routeStopRepository.save(disposalStop);
                }
            }
        }

        RouteStop depotReturn = new RouteStop();
        depotReturn.setRoutePlan(savedRoutePlan);
        depotReturn.setStopOrder(stopOrder);
        depotReturn.setStopType(RouteStop.StopType.DEPOT_RETURN);
        depotReturn.setLat(depot.getLat());
        depotReturn.setLng(depot.getLng());
        depotReturn.setStatus(RouteStop.StopStatus.PLANNED);
        depotReturn.setNotes("Retour dépôt après route mise à jour temps réel");
        routeStopRepository.save(depotReturn);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<MissionResponse> replanMission(Long missionId, ReplanRequestDto request) {
        Mission originalMission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        validateMissionEligibleForReplan(originalMission);
        validateIncidentType(request);

        List<MissionBin> remainingMissionBins =
                missionBinRepository.findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(missionId);

        if (remainingMissionBins.isEmpty()) {
            throw new BadRequestException("No remaining bins to replan for mission: " + missionId);
        }

        Long affectedTruckId = resolveAffectedTruckId(originalMission, request);

        if (originalMission.getTruck() == null || !originalMission.getTruck().getId().equals(affectedTruckId)) {
            throw new BadRequestException(
                    "Affected truck does not match the truck assigned to mission " + missionId
            );
        }
        applyAffectedTruckStatus(originalMission, request);

        Set<Long> trucksWithActiveBlockingIncidents = findTrucksWithActiveBlockingIncidents();

        List<Truck> availableTrucks = truckRepository.findByIsActiveTrue()
                .stream()
                .filter(this::isRoutingCandidate)
                .filter(t -> !t.getId().equals(affectedTruckId))
                .filter(t -> !trucksWithActiveBlockingIncidents.contains(t.getId()))
                .toList();

        if (availableTrucks.isEmpty()) {
            throw new BadRequestException("No available trucks for replanning");
        }

        List<RoutePlan> existingPlans = routePlanRepository.findByMissionOrderByCreatedAtDesc(originalMission);
        if (!existingPlans.isEmpty()) {
            RoutePlan latestPlan = existingPlans.get(0);
            latestPlan.setPlanStatus(RoutePlan.PlanStatus.REPLACED);
            routePlanRepository.save(latestPlan);
        }

        RoutingRequestDto routingRequest = routingPayloadBuilderService.buildReplanRequest(
                availableTrucks,
                remainingMissionBins
        );

        RoutingResponseDto routingResponse = pythonRoutingClient.optimizeRoutes(routingRequest);

        if (routingResponse == null || routingResponse.getMissions() == null || routingResponse.getMissions().isEmpty()) {
            throw new BadRequestException(
                    "Replanning returned no missions. Matrix source = "
                            + (routingResponse != null ? routingResponse.getMatrixSource() : "NULL")
            );
        }

        Map<Long, MissionBin> remainingMissionBinsByBinId = remainingMissionBins.stream()
                .filter(mb -> mb.getBin() != null && mb.getBin().getId() != null)
                .collect(Collectors.toMap(
                        mb -> mb.getBin().getId(),
                        Function.identity(),
                        (a, b) -> a
                ));

        List<MissionResponse> replannedMissions = new ArrayList<>();

        for (RoutingMissionDto routingMissionDto : routingResponse.getMissions()) {
            Mission savedMission = saveReplannedMissionFromRouting(
                    originalMission,
                    routingMissionDto,
                    availableTrucks,
                    routingRequest,
                    request,
                    remainingMissionBinsByBinId
            );
            replannedMissions.add(mapMissionToResponse(savedMission));
        }

        originalMission.setMissionStatusDetail(Mission.MissionStatusDetail.PARTIALLY_REASSIGNED);
        originalMission.setReplannedCount((originalMission.getReplannedCount() != null ? originalMission.getReplannedCount() : 0) + 1);
        missionRepository.save(originalMission);

        Long newMissionId = replannedMissions.isEmpty()
                ? null
                : replannedMissions.get(0).getId();

        missionRealtimeService.publishMissionPartiallyReassigned(originalMission, newMissionId);

        return replannedMissions;

       
    }

    private Set<Long> findTrucksWithActiveBlockingIncidents() {
        List<TruckIncident> activeIncidents;

        try {
            activeIncidents = truckIncidentRepository.findByStatusIn(
                    List.of(
                            TruckIncident.IncidentStatus.OPEN,
                            TruckIncident.IncidentStatus.IN_PROGRESS
                    )
            );
        } catch (Exception e) {
            return Set.of();
        }

        return activeIncidents.stream()
                .filter(incident -> incident != null)
                .filter(incident -> incident.getTruck() != null)
                .filter(incident -> incident.getTruck().getId() != null)
                .filter(this::isBlockingIncident)
                .map(incident -> incident.getTruck().getId())
                .collect(Collectors.toSet());
    }

    private boolean isBlockingIncident(TruckIncident incident) {
        if (incident == null || incident.getIncidentType() == null) {
            return false;
        }

        String incidentType = incident.getIncidentType().name().trim().toUpperCase();

        return incidentType.equals("BREAKDOWN")
                || incidentType.equals("DRIVER_UNAVAILABLE")
                || incidentType.equals("REFUEL_REQUIRED");
    }

    private void validateMissionEligibleForReplan(Mission mission) {
        if (mission == null) {
            throw new BadRequestException("Mission is required for replanning");
        }

        if ("COMPLETED".equalsIgnoreCase(mission.getStatus())) {
            throw new BadRequestException("Cannot replan a completed mission: " + mission.getId());
        }

        if (isRefuelOnlyMission(mission)) {
            throw new BadRequestException("Cannot replan a refuel-only mission: " + mission.getId());
        }

        if (mission.getTruck() == null || mission.getTruck().getId() == null) {
            throw new BadRequestException("Mission has no assigned truck: " + mission.getId());
        }
    }

    private boolean isRefuelOnlyMission(Mission mission) {
        String notes = mission.getNotes();
        if (notes == null) {
            return false;
        }

        String normalized = notes.trim().toLowerCase();
        return normalized.contains("auto-generated refuel mission")
                || normalized.contains("refuel-only")
                || normalized.contains("refueling");
    }

    private void validateIncidentType(ReplanRequestDto request) {
        if (request == null || request.getIncidentType() == null || request.getIncidentType().isBlank()) {
            throw new BadRequestException("incidentType is required for replanning");
        }

        String normalized = request.getIncidentType().trim().toUpperCase();
        if (!ALLOWED_INCIDENT_TYPES.contains(normalized)) {
            throw new BadRequestException(
                    "Unsupported incidentType: " + request.getIncidentType()
                            + ". Allowed values: " + ALLOWED_INCIDENT_TYPES
            );
        }
    }

    private Long resolveAffectedTruckId(Mission originalMission, ReplanRequestDto request) {
        if (request != null && request.getAffectedTruckId() != null) {
            return request.getAffectedTruckId();
        }

        if (originalMission.getTruck() != null && originalMission.getTruck().getId() != null) {
            return originalMission.getTruck().getId();
        }

        throw new BadRequestException("Affected truck id is required for replanning");
    }
    
    private void applyAffectedTruckStatus(Mission originalMission, ReplanRequestDto request) {
        if (originalMission == null || originalMission.getTruck() == null || request == null) {
            return;
        }

        String incidentType = request.getIncidentType() != null
                ? request.getIncidentType().trim().toUpperCase()
                : "";

        Truck affectedTruck = originalMission.getTruck();

        switch (incidentType) {
            case "BREAKDOWN" -> affectedTruck.setStatus(Truck.TruckStatus.BREAKDOWN);
            case "FUEL_LOW", "REFUEL_REQUIRED" -> affectedTruck.setStatus(Truck.TruckStatus.REFUELING);
            default -> {
                return;
            }
        }

        affectedTruck.setLastStatusUpdate(OffsetDateTime.now());
        truckRepository.save(affectedTruck);

        System.out.println(
                "AFFECTED TRUCK STATUS UPDATED => truckId=" + affectedTruck.getId()
                        + " | incidentType=" + incidentType
                        + " | newStatus=" + affectedTruck.getStatus()
        );
    }

    private boolean isRoutingCandidate(Truck truck) {
        if (truck == null) {
            return false;
        }

        if (truck.getIsActive() == null || !truck.getIsActive()) {
            return false;
        }

        if (truck.getStatus() == null || truck.getStatus() != Truck.TruckStatus.AVAILABLE) {
            return false;
        }

        if (truck.getMaxLoadKg() == null) {
            return false;
        }

        BigDecimal currentLoad = truck.getCurrentLoadKg() != null ? truck.getCurrentLoadKg() : BigDecimal.ZERO;
        BigDecimal remainingCapacity = truck.getMaxLoadKg().subtract(currentLoad);

        return remainingCapacity.compareTo(BigDecimal.ZERO) > 0;
    }

    private Mission saveReplannedMissionFromRouting(Mission originalMission,
                                                    RoutingMissionDto routingMissionDto,
                                                    List<Truck> availableTrucks,
                                                    RoutingRequestDto routingRequest,
                                                    ReplanRequestDto request,
                                                    Map<Long, MissionBin> remainingMissionBinsByBinId) {
        Truck targetTruck = availableTrucks.stream()
                .filter(t -> t.getId().equals(routingMissionDto.getTruckId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Truck not found for replanned mission: " + routingMissionDto.getTruckId()
                ));

        Driver driver = resolveDriverForTruck(targetTruck);
        Depot depot = resolveActiveDepot();

        Mission mission = new Mission();
        mission.setMissionCode(generateMissionCode());
        mission.setDriver(driver);
        mission.setTruck(targetTruck);
        mission.setDepot(depot);
        mission.setZone(targetTruck.getZone());
        mission.setStatus("CREATED");
        mission.setMissionStatusDetail(Mission.MissionStatusDetail.PLANNED);
        mission.setPriority("HIGH");
        mission.setPlannedDate(LocalDate.now());
        mission.setCreatedAt(Instant.now());
        mission.setNotes(buildReplanNotes(originalMission, request));

        if (routingMissionDto.getTotalDistanceKm() != null) {
            mission.setEstimatedDistanceKm(BigDecimal.valueOf(routingMissionDto.getTotalDistanceKm()));
        }

        if (routingMissionDto.getTotalDurationMinutes() != null) {
            mission.setEstimatedDurationMin((int) Math.round(routingMissionDto.getTotalDurationMinutes()));
        }

        Mission savedMission = missionRepository.save(mission);

        if (routingMissionDto.getStops() != null && !routingMissionDto.getStops().isEmpty()) {
            int fallbackOrder = 1;

            for (RoutingStopDto stopDto : routingMissionDto.getStops()) {
                if (stopDto == null) {
                    continue;
                }

                String stopType = stopDto.getStopType() != null
                        ? stopDto.getStopType().trim().toUpperCase()
                        : "BIN_PICKUP";

                if (!"BIN_PICKUP".equals(stopType) || stopDto.getBinId() == null) {
                    continue;
                }

                MissionBin missionBin = buildMissionBin(
                        savedMission,
                        stopDto,
                        resolveVisitOrder(stopDto, fallbackOrder++),
                        remainingMissionBinsByBinId,
                        originalMission,
                        targetTruck,
                        request
                );

                missionBinRepository.save(missionBin);
            }
        }

        saveReplannedRoutePlanAndStops(
                savedMission,
                targetTruck,
                depot,
                routingMissionDto,
                routingRequest,
                remainingMissionBinsByBinId
        );

        return savedMission;
    }

    private Driver resolveDriverForTruck(Truck truck) {
        if (truck == null) {
            throw new BadRequestException("Truck is null while resolving driver");
        }

        if (truck.getTruckCode() == null || truck.getTruckCode().isBlank()) {
            throw new BadRequestException("Truck code is missing for truck id: " + truck.getId());
        }

        String truckCode = truck.getTruckCode().trim();

        return driverRepository.findAll()
                .stream()
                .filter(driver -> driver.getVehicleCode() != null)
                .filter(driver -> driver.getVehicleCode().trim().equalsIgnoreCase(truckCode))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Truck " + truck.getId()
                                + " has no assigned driver. Expected vehicle_code="
                                + truckCode
                ));
    }

    private void saveReplannedRoutePlanAndStops(Mission mission,
                                                Truck truck,
                                                Depot depot,
                                                RoutingMissionDto routingMissionDto,
                                                RoutingRequestDto routingRequest,
                                                Map<Long, MissionBin> remainingMissionBinsByBinId) {
        RoutePlan routePlan = new RoutePlan();
        routePlan.setMission(mission);
        routePlan.setTruck(truck);
        routePlan.setDepot(depot);
        routePlan.setPlanType(RoutePlan.PlanType.REPLANNED);
        routePlan.setPlanStatus(RoutePlan.PlanStatus.PLANNED);
        routePlan.setOptimizationAlgorithm("OR_TOOLS_OSRM_REPLAN");
        routePlan.setOptimizationVersion(REPLAN_ALGORITHM_VERSION);
        routePlan.setTrafficMode(RoutePlan.TrafficMode.REAL);

        if (routingMissionDto.getTotalDistanceKm() != null) {
            routePlan.setTotalDistanceKm(BigDecimal.valueOf(routingMissionDto.getTotalDistanceKm()));
        }

        if (routingMissionDto.getTotalDurationMinutes() != null) {
            routePlan.setEstimatedDurationMin((int) Math.round(routingMissionDto.getTotalDurationMinutes()));
        }

        RoutePlan savedRoutePlan = routePlanRepository.save(routePlan);

        int stopOrder = 1;

        RouteStop depotStart = new RouteStop();
        depotStart.setRoutePlan(savedRoutePlan);
        depotStart.setStopOrder(stopOrder++);
        depotStart.setStopType(RouteStop.StopType.DEPOT_START);
        depotStart.setLat(resolveDepotLat(routingRequest, depot));
        depotStart.setLng(resolveDepotLng(routingRequest, depot));
        depotStart.setStatus(RouteStop.StopStatus.PLANNED);
        depotStart.setNotes("Depart de la route replanifiee depuis le depot");
        routeStopRepository.save(depotStart);

        if (routingMissionDto.getStops() != null) {
            for (RoutingStopDto stopDto : routingMissionDto.getStops()) {
                if (stopDto == null) {
                    continue;
                }

                String stopType = stopDto.getStopType() != null
                        ? stopDto.getStopType().trim().toUpperCase()
                        : "BIN_PICKUP";

                if ("BIN_PICKUP".equals(stopType)) {
                    if (stopDto.getBinId() == null) {
                        continue;
                    }

                    MissionBin remainingMissionBin = remainingMissionBinsByBinId.get(stopDto.getBinId());
                    if (remainingMissionBin == null || remainingMissionBin.getBin() == null) {
                        continue;
                    }

                    Bin bin = remainingMissionBin.getBin();

                    RouteStop routeStop = new RouteStop();
                    routeStop.setRoutePlan(savedRoutePlan);
                    routeStop.setStopOrder(stopOrder++);
                    routeStop.setStopType(RouteStop.StopType.BIN_PICKUP);
                    routeStop.setBin(bin);
                    routeStop.setLat(resolveBinRoutingLat(bin));
                    routeStop.setLng(resolveBinRoutingLng(bin));
                    routeStop.setStatus(RouteStop.StopStatus.PLANNED);
                    routeStop.setNotes("Collecte du bac dans la mission replanifiee");
                    routeStopRepository.save(routeStop);

                    continue;
                }

                if ("DISPOSAL_SITE".equals(stopType)) {
                    if (stopDto.getDisposalSiteId() == null) {
                        continue;
                    }

                    RoutingDisposalSiteDto disposalSite = null;

                    if (routingRequest != null && routingRequest.getDisposalSites() != null) {
                        disposalSite = routingRequest.getDisposalSites()
                                .stream()
                                .filter(site -> stopDto.getDisposalSiteId().equals(site.getId()))
                                .findFirst()
                                .orElse(null);
                    }

                    if (disposalSite == null) {
                        throw new BadRequestException(
                                "Disposal site not found in replan routing request: "
                                        + stopDto.getDisposalSiteId()
                        );
                    }

                    RouteStop disposalStop = new RouteStop();
                    disposalStop.setRoutePlan(savedRoutePlan);
                    disposalStop.setStopOrder(stopOrder++);
                    disposalStop.setStopType(RouteStop.StopType.DISPOSAL_SITE);
                    disposalStop.setDisposalSiteId(disposalSite.getId());
                    disposalStop.setDisposalSiteName(disposalSite.getName());
                    disposalStop.setLat(disposalSite.getLat());
                    disposalStop.setLng(disposalSite.getLng());
                    disposalStop.setStatus(RouteStop.StopStatus.PLANNED);
                    disposalStop.setNotes("Dechargement du camion pendant la mission replanifiee");
                    routeStopRepository.save(disposalStop);
                }
            }
        }

        RouteStop depotReturn = new RouteStop();
        depotReturn.setRoutePlan(savedRoutePlan);
        depotReturn.setStopOrder(stopOrder);
        depotReturn.setStopType(RouteStop.StopType.DEPOT_RETURN);
        depotReturn.setLat(resolveDepotLat(routingRequest, depot));
        depotReturn.setLng(resolveDepotLng(routingRequest, depot));
        depotReturn.setStatus(RouteStop.StopStatus.PLANNED);
        depotReturn.setNotes("Retour au depot apres la mission replanifiee");
        routeStopRepository.save(depotReturn);
    }

    private Depot resolveActiveDepot() {
        List<Depot> activeDepots = depotRepository.findByIsActiveTrue();
        if (activeDepots.isEmpty()) {
            throw new BadRequestException("No active depot found");
        }
        return activeDepots.get(0);
    }

    private Double resolveDepotLat(RoutingRequestDto routingRequest, Depot depot) {
        if (routingRequest != null && routingRequest.getDepot() != null && routingRequest.getDepot().getLat() != null) {
            return routingRequest.getDepot().getLat();
        }
        return depot.getLat();
    }

    private Double resolveDepotLng(RoutingRequestDto routingRequest, Depot depot) {
        if (routingRequest != null && routingRequest.getDepot() != null && routingRequest.getDepot().getLng() != null) {
            return routingRequest.getDepot().getLng();
        }
        return depot.getLng();
    }

    private Double resolveBinRoutingLat(Bin bin) {
        if (bin.getAccessLat() != null) {
            return bin.getAccessLat();
        }
        return bin.getLat();
    }

    private Double resolveBinRoutingLng(Bin bin) {
        if (bin.getAccessLng() != null) {
            return bin.getAccessLng();
        }
        return bin.getLng();
    }

    private MissionBin buildMissionBin(Mission mission,
                                       RoutingStopDto stopDto,
                                       int visitOrder,
                                       Map<Long, MissionBin> remainingMissionBinsByBinId,
                                       Mission originalMission,
                                       Truck targetTruck,
                                       ReplanRequestDto request) {
    	MissionBin originalMissionBin = remainingMissionBinsByBinId.get(stopDto.getBinId());

    	if (originalMissionBin == null || originalMissionBin.getBin() == null) {
    	    throw new ResourceNotFoundException("Bin not found for replanned mission: " + stopDto.getBinId());
    	}

    	/*
    	 * The original mission keeps the historical bin assignment,
    	 * but it must no longer appear as an active pickup for the affected truck.
    	 */
    	originalMissionBin.setAssignmentStatus(MissionBin.AssignmentStatus.REASSIGNED);
    	originalMissionBin.setReassignedFromTruck(originalMission.getTruck());
    	originalMissionBin.setReassignedToTruck(targetTruck);
    	originalMissionBin.setSkippedReason("REASSIGNED_TO_TRUCK_" + targetTruck.getId());
    	missionBinRepository.save(originalMissionBin);

    	MissionBin missionBin = new MissionBin();
        missionBin.setMission(mission);
        missionBin.setBin(originalMissionBin.getBin());
        missionBin.setVisitOrder(visitOrder);
        missionBin.setAssignedReason("PREDICTION");
        missionBin.setCollected(false);
        missionBin.setAssignmentStatus(MissionBin.AssignmentStatus.PLANNED);
        missionBin.setTargetFillThreshold(originalMissionBin.getTargetFillThreshold());
        missionBin.setReassignedFromTruck(originalMission.getTruck() != null ? originalMission.getTruck() : null);
        missionBin.setReassignedToTruck(targetTruck);

        saveMissionReassignment(
                originalMission,
                originalMission.getTruck(),
                targetTruck,
                originalMissionBin.getBin(),
                request
        );

        return missionBin;
    }

    private void saveMissionReassignment(Mission originalMission,
                                         Truck sourceTruck,
                                         Truck targetTruck,
                                         Bin bin,
                                         ReplanRequestDto request) {
        if (originalMission == null || targetTruck == null || bin == null) {
            return;
        }

        Long originalMissionId = originalMission.getId();
        Long sourceTruckId = sourceTruck != null ? sourceTruck.getId() : null;
        Long targetTruckId = targetTruck.getId();
        Long binId = bin.getId();

        if (originalMissionId == null || targetTruckId == null || binId == null) {
            return;
        }

        boolean alreadyExists = missionReassignmentRepository
                .existsByOriginalMissionIdAndSourceTruckIdAndTargetTruckIdAndBinId(
                        originalMissionId,
                        sourceTruckId,
                        targetTruckId,
                        binId
                );

        if (alreadyExists) {
            return;
        }

        MissionReassignment reassignment = new MissionReassignment();
        reassignment.setOriginalMission(originalMission);
        reassignment.setSourceTruck(sourceTruck);
        reassignment.setTargetTruck(targetTruck);
        reassignment.setBin(bin);
        reassignment.setReason(resolveReassignmentReason(request));
        reassignment.setReassignedAt(OffsetDateTime.now());
        reassignment.setAlgorithmVersion(REPLAN_ALGORITHM_VERSION);
        reassignment.setNotes(buildReassignmentNotes(originalMission, sourceTruck, targetTruck, request, bin));

        missionReassignmentRepository.save(reassignment);
    }

    private MissionReassignment.ReassignmentReason resolveReassignmentReason(ReplanRequestDto request) {
        if (request == null || request.getIncidentType() == null) {
            return MissionReassignment.ReassignmentReason.OTHER;
        }

        String incidentType = request.getIncidentType().trim().toUpperCase();

        return switch (incidentType) {
            case "BREAKDOWN" -> MissionReassignment.ReassignmentReason.BREAKDOWN;
            case "TRAFFIC", "TRAFFIC_BLOCK" -> MissionReassignment.ReassignmentReason.TRAFFIC;
            case "FUEL_LOW", "REFUEL_REQUIRED" -> MissionReassignment.ReassignmentReason.FUEL_LOW;
            case "DELAY" -> MissionReassignment.ReassignmentReason.DELAY;
            case "MANUAL" -> MissionReassignment.ReassignmentReason.MANUAL;
            default -> MissionReassignment.ReassignmentReason.OTHER;
        };
    }

    private String buildReassignmentNotes(Mission originalMission,
                                          Truck sourceTruck,
                                          Truck targetTruck,
                                          ReplanRequestDto request,
                                          Bin bin) {
        String incidentType = request != null && request.getIncidentType() != null
                ? request.getIncidentType()
                : "UNKNOWN";

        String reason = request != null && request.getReason() != null
                ? request.getReason()
                : "Aucune raison detaillee fournie";

        Long sourceTruckId = sourceTruck != null ? sourceTruck.getId() : null;
        Long targetTruckId = targetTruck != null ? targetTruck.getId() : null;

        return "Reaffectation de mission | missionInitialeId=" + originalMission.getId()
                + " | camionSourceId=" + sourceTruckId
                + " | camionCibleId=" + targetTruckId
                + " | bacId=" + (bin != null ? bin.getId() : null)
                + " | typeIncident=" + incidentType
                + " | motif=" + reason;
    }

    private int resolveVisitOrder(RoutingStopDto stopDto, int fallbackOrder) {
        if (stopDto != null && stopDto.getOrderIndex() != null && stopDto.getOrderIndex() > 0) {
            return stopDto.getOrderIndex();
        }
        return fallbackOrder;
    }

    private String buildReplanNotes(Mission originalMission, ReplanRequestDto request) {
        String incidentType = request != null && request.getIncidentType() != null
                ? request.getIncidentType().trim().toUpperCase()
                : "INCIDENT_INCONNU";

        String reason = request != null && request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason()
                : "Aucune raison detaillee fournie";

        String incidentLabel = switch (incidentType) {
            case "BREAKDOWN" -> "panne du camion";
            case "TRAFFIC", "TRAFFIC_BLOCK" -> "blocage ou perturbation du trafic";
            case "FUEL_LOW" -> "niveau de carburant faible";
            case "REFUEL_REQUIRED" -> "ravitaillement requis";
            case "DELAY" -> "retard operationnel";
            case "MANUAL" -> "replanification manuelle";
            default -> "incident operationnel";
        };

        return "Mission replanifiee a partir de la mission "
                + originalMission.getId()
                + " suite a un incident : "
                + incidentLabel
                + ". Motif : "
                + reason;
    }

    private String generateMissionCode() {
        return "MIS-" + System.currentTimeMillis();
    }

    private boolean isValidCoordinatePair(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180
                && !(lat == 0.0 && lng == 0.0);
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

        if (mission.getMissionStatusDetail() != null) {
            dto.setMissionStatusDetail(mission.getMissionStatusDetail().name());
        }

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
}