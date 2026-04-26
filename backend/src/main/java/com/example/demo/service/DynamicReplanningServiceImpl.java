package com.example.demo.service;

import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.ReplanRequestDto;
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

@Service
public class DynamicReplanningServiceImpl implements DynamicReplanningService {

    private static final String REPLAN_ALGORITHM_VERSION = "v4.3";

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
                                        PythonRoutingClient pythonRoutingClient) {
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
    }

    @Override
    @Transactional
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

        System.out.println(
                "REPLAN INCIDENT => missionId=" + missionId
                        + ", affectedTruckId=" + affectedTruckId
                        + ", incidentType=" + request.getIncidentType()
                        + ", reason=" + request.getReason()
        );

        Set<Long> trucksWithActiveBlockingIncidents = findTrucksWithActiveBlockingIncidents();

        List<Truck> availableTrucks = truckRepository.findByIsActiveTrue()
                .stream()
                .filter(this::isRoutingCandidate)
                .filter(t -> !t.getId().equals(affectedTruckId))
                .filter(t -> !trucksWithActiveBlockingIncidents.contains(t.getId()))
                .toList();

        System.out.println(
                "REPLAN ELIGIBILITY => affectedTruckId=" + affectedTruckId
                        + " excluded from replan candidates, trucksWithActiveBlockingIncidents="
                        + trucksWithActiveBlockingIncidents
                        + ", availableTrucks="
                        + availableTrucks.stream().map(Truck::getId).toList()
        );

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
            System.out.println("REPLAN INCIDENT FILTER WARNING => unable to load active incidents: " + e.getMessage());
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
        routePlan.setOptimizationAlgorithm("OR_TOOLS_TOMTOM_REPLAN");
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
        depotStart.setNotes("Départ de la route replanifiée depuis le dépôt");
        routeStopRepository.save(depotStart);

        if (routingMissionDto.getStops() != null) {
            for (RoutingStopDto stopDto : routingMissionDto.getStops()) {
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
                routeStop.setNotes("Collecte du bac dans la mission replanifiée");
                routeStopRepository.save(routeStop);
            }
        }

        RouteStop depotReturn = new RouteStop();
        depotReturn.setRoutePlan(savedRoutePlan);
        depotReturn.setStopOrder(stopOrder);
        depotReturn.setStopType(RouteStop.StopType.DEPOT_RETURN);
        depotReturn.setLat(resolveDepotLat(routingRequest, depot));
        depotReturn.setLng(resolveDepotLng(routingRequest, depot));
        depotReturn.setStatus(RouteStop.StopStatus.PLANNED);
        depotReturn.setNotes("Retour au dépôt après la mission replanifiée");
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
                : "Aucune raison détaillée fournie";

        Long sourceTruckId = sourceTruck != null ? sourceTruck.getId() : null;
        Long targetTruckId = targetTruck != null ? targetTruck.getId() : null;

        return "Réaffectation de mission | missionInitialeId=" + originalMission.getId()
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
                : "Aucune raison détaillée fournie";

        String incidentLabel = switch (incidentType) {
            case "BREAKDOWN" -> "panne du camion";
            case "TRAFFIC", "TRAFFIC_BLOCK" -> "blocage ou perturbation du trafic";
            case "FUEL_LOW" -> "niveau de carburant faible";
            case "REFUEL_REQUIRED" -> "ravitaillement requis";
            case "DELAY" -> "retard opérationnel";
            case "MANUAL" -> "replanification manuelle";
            default -> "incident opérationnel";
        };

        return "Mission replanifiée à partir de la mission "
                + originalMission.getId()
                + " suite à un incident : "
                + incidentLabel
                + ". Motif : "
                + reason;
    }

    private String generateMissionCode() {
        return "MIS-" + System.currentTimeMillis();
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
}