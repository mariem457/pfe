package com.example.demo.service;

import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.ExcludedTruckDto;
import com.example.demo.dto.routing.RecommendedFuelStationDto;
import com.example.demo.dto.routing.RoutingMissionDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingResponseDto;
import com.example.demo.dto.routing.RoutingStopDto;
import com.example.demo.entity.Bin;
import com.example.demo.entity.Depot;
import com.example.demo.entity.Driver;
import com.example.demo.entity.FuelStation;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.RoutePlan;
import com.example.demo.entity.RouteStop;
import com.example.demo.entity.Truck;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.DepotRepository;
import com.example.demo.repository.FuelStationRepository;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.RoutePlanRepository;
import com.example.demo.repository.RouteStopRepository;
import com.example.demo.repository.TruckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingOptimizationServiceImpl implements RoutingOptimizationService {

    private final TruckRepository truckRepository;
    private final BinRepository binRepository;
    private final DepotRepository depotRepository;
    private final MissionRepository missionRepository;
    private final MissionBinRepository missionBinRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteStopRepository routeStopRepository;
    private final RoutingPayloadBuilderService routingPayloadBuilderService;
    private final PythonRoutingClient pythonRoutingClient;
    private final FuelStationRepository fuelStationRepository;

    public RoutingOptimizationServiceImpl(TruckRepository truckRepository,
                                          BinRepository binRepository,
                                          DepotRepository depotRepository,
                                          MissionRepository missionRepository,
                                          MissionBinRepository missionBinRepository,
                                          RoutePlanRepository routePlanRepository,
                                          RouteStopRepository routeStopRepository,
                                          RoutingPayloadBuilderService routingPayloadBuilderService,
                                          PythonRoutingClient pythonRoutingClient,
                                          FuelStationRepository fuelStationRepository) {
        this.truckRepository = truckRepository;
        this.binRepository = binRepository;
        this.depotRepository = depotRepository;
        this.missionRepository = missionRepository;
        this.missionBinRepository = missionBinRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeStopRepository = routeStopRepository;
        this.routingPayloadBuilderService = routingPayloadBuilderService;
        this.pythonRoutingClient = pythonRoutingClient;
        this.fuelStationRepository = fuelStationRepository;
    }

    @Override
    public RoutingResponseDto prepareInitialRouting() {
        List<Truck> activeTrucks = truckRepository.findByIsActiveTrue();
        System.out.println("Active trucks count: " + activeTrucks.size());

        List<Truck> routingCandidateTrucks = activeTrucks.stream()
                .filter(this::isRoutingCandidate)
                .toList();

        System.out.println("Routing candidate trucks count: " + routingCandidateTrucks.size());

        for (Truck truck : routingCandidateTrucks) {
            System.out.println(
                    "Routing candidate truck -> id=" + truck.getId()
                            + ", code=" + truck.getTruckCode()
                            + ", status=" + truck.getStatus()
                            + ", fuelLevel=" + truck.getFuelLevelLiters()
                            + ", fuelConsumptionPerKm=" + truck.getFuelConsumptionPerKm()
            );
        }

        RoutingRequestDto routingRequest = routingPayloadBuilderService.buildRoutingRequest(routingCandidateTrucks);

        int binsCount = routingRequest.getBins() != null ? routingRequest.getBins().size() : 0;
        int trucksCount = routingRequest.getTrucks() != null ? routingRequest.getTrucks().size() : 0;
        int incidentsCount = routingRequest.getActiveIncidents() != null ? routingRequest.getActiveIncidents().size() : 0;

        System.out.println("Routing payload built successfully");
        System.out.println("Routing bins count: " + binsCount);
        System.out.println("Routing trucks count: " + trucksCount);
        System.out.println("Routing incidents count: " + incidentsCount);

        if (routingRequest.getBins() != null) {
            routingRequest.getBins().forEach(bin ->
                    System.out.println(
                            "Priority bin -> id=" + bin.getId()
                                    + ", fillLevel=" + bin.getFillLevel()
                                    + ", priority=" + bin.getPredictedPriority()
                                    + ", estimatedLoadKg=" + bin.getEstimatedLoadKg()
                    )
            );
        }

        RoutingResponseDto routingResponse = pythonRoutingClient.optimizeRoutes(routingRequest);

        if (routingResponse != null) {
            routingResponse.setRecommendedFuelStations(
                    routingPayloadBuilderService.getLastRecommendedFuelStations()
            );
        }

        int missionsCount = routingResponse != null && routingResponse.getMissions() != null
                ? routingResponse.getMissions().size()
                : 0;

        System.out.println("Routing response received");
        System.out.println("Returned missions count: " + missionsCount);
        System.out.println("Matrix source: " + (routingResponse != null ? routingResponse.getMatrixSource() : "NULL"));

        if (routingResponse != null && routingResponse.getMissions() != null) {
            routingResponse.getMissions().forEach(mission ->
                    System.out.println(
                            "Mission -> truckId=" + mission.getTruckId()
                                    + ", totalDistanceKm=" + mission.getTotalDistanceKm()
                                    + ", totalDurationMinutes=" + mission.getTotalDurationMinutes()
                                    + ", stopsCount=" + (mission.getStops() != null ? mission.getStops().size() : 0)
                                    + ", routeCoordinatesCount=" + (mission.getRouteCoordinates() != null ? mission.getRouteCoordinates().size() : 0)
                    )
            );
        }

        return routingResponse;
    }

    @Override
    @Transactional
    public List<MissionResponse> planAndSaveMissions() {
        List<Truck> activeTrucks = truckRepository.findByIsActiveTrue();

        List<Truck> routingCandidateTrucks = activeTrucks.stream()
                .filter(this::isRoutingCandidate)
                .toList();

        if (routingCandidateTrucks.isEmpty()) {
            throw new BadRequestException("No routing candidate trucks available");
        }

        RoutingRequestDto routingRequest = routingPayloadBuilderService.buildRoutingRequest(routingCandidateTrucks);
        RoutingResponseDto routingResponse = pythonRoutingClient.optimizeRoutes(routingRequest);

        if (routingResponse != null) {
            routingResponse.setRecommendedFuelStations(
                    routingPayloadBuilderService.getLastRecommendedFuelStations()
            );
        }

        if (routingResponse == null) {
            throw new BadRequestException("Routing engine returned null response");
        }

        List<MissionResponse> savedMissions = new ArrayList<>();

        if (routingResponse.getMissions() != null) {
            for (RoutingMissionDto routingMissionDto : routingResponse.getMissions()) {
                Mission savedMission = saveMissionFromRouting(
                        routingMissionDto,
                        routingCandidateTrucks,
                        routingRequest,
                        routingResponse
                );
                savedMissions.add(mapMissionToResponse(savedMission));
            }
        }

        saveExcludedRefuelMissions(
                routingResponse,
                routingCandidateTrucks,
                routingRequest,
                savedMissions
        );

        if (savedMissions.isEmpty()) {
            throw new BadRequestException(
                    "No missions saved. Matrix source = " + routingResponse.getMatrixSource()
            );
        }

        return savedMissions;
    }

    private void saveExcludedRefuelMissions(RoutingResponseDto routingResponse,
                                            List<Truck> routingCandidateTrucks,
                                            RoutingRequestDto routingRequest,
                                            List<MissionResponse> savedMissions) {
        if (routingResponse == null || routingResponse.getExcludedTrucks() == null) {
            return;
        }

        for (ExcludedTruckDto excludedTruck : routingResponse.getExcludedTrucks()) {
            if (excludedTruck == null || excludedTruck.getTruckId() == null) {
                continue;
            }

            if (!"REFUEL_REQUIRED:CRITICAL".equalsIgnoreCase(excludedTruck.getReason())
                    && !"REFUEL_REQUIRED".equalsIgnoreCase(excludedTruck.getReason())) {
                continue;
            }

            Truck truck = routingCandidateTrucks.stream()
                    .filter(t -> excludedTruck.getTruckId().equals(t.getId()))
                    .findFirst()
                    .orElse(null);

            if (truck == null) {
                continue;
            }

            RecommendedFuelStationDto stationDto =
                    findRecommendedFuelStationForTruck(routingResponse, truck.getId());

            if (stationDto == null || stationDto.getStationId() == null) {
                continue;
            }

            Mission refuelMission = saveRefuelOnlyMission(truck, routingRequest, stationDto);
            savedMissions.add(mapMissionToResponse(refuelMission));
        }
    }

    private Mission saveRefuelOnlyMission(Truck truck,
                                          RoutingRequestDto routingRequest,
                                          RecommendedFuelStationDto stationDto) {
        Driver driver = truck.getAssignedDriver();
        if (driver == null) {
            throw new BadRequestException("Truck " + truck.getId() + " has no assigned driver");
        }

        Depot depot = resolveActiveDepot();

        FuelStation fuelStation = fuelStationRepository.findById(stationDto.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fuel station not found: " + stationDto.getStationId()
                ));

        Mission mission = new Mission();
        mission.setMissionCode(generateMissionCode());
        mission.setDriver(driver);
        mission.setTruck(truck);
        mission.setStatus("CREATED");
        mission.setMissionStatusDetail(Mission.MissionStatusDetail.PLANNED);
        mission.setPriority("HIGH");
        mission.setPlannedDate(LocalDate.now());
        mission.setCreatedAt(Instant.now());
        mission.setNotes("Auto-generated refuel mission before collection routing");

        Mission savedMission = missionRepository.save(mission);

        RoutePlan routePlan = new RoutePlan();
        routePlan.setMission(savedMission);
        routePlan.setTruck(truck);
        routePlan.setDepot(depot);
        routePlan.setPlanType(RoutePlan.PlanType.EMERGENCY);
        routePlan.setPlanStatus(RoutePlan.PlanStatus.PLANNED);
        routePlan.setOptimizationAlgorithm("REFUEL_FALLBACK");
        routePlan.setOptimizationVersion("v1");
        routePlan.setTrafficMode(RoutePlan.TrafficMode.REAL);

        RoutePlan savedRoutePlan = routePlanRepository.save(routePlan);

        int stopOrder = 1;

        RouteStop depotStart = new RouteStop();
        depotStart.setRoutePlan(savedRoutePlan);
        depotStart.setStopOrder(stopOrder++);
        depotStart.setStopType(RouteStop.StopType.DEPOT_START);
        depotStart.setLat(resolveDepotLat(routingRequest, depot));
        depotStart.setLng(resolveDepotLng(routingRequest, depot));
        depotStart.setStatus(RouteStop.StopStatus.PLANNED);
        depotStart.setNotes("Start from depot for refueling");
        routeStopRepository.save(depotStart);

        RouteStop fuelStop = new RouteStop();
        fuelStop.setRoutePlan(savedRoutePlan);
        fuelStop.setStopOrder(stopOrder++);
        fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
        fuelStop.setFuelStation(fuelStation);
        fuelStop.setLat(fuelStation.getLat());
        fuelStop.setLng(fuelStation.getLng());
        fuelStop.setStatus(RouteStop.StopStatus.PLANNED);
        fuelStop.setNotes("Refuel at station: " + fuelStation.getName());
        routeStopRepository.save(fuelStop);

        RouteStop depotReturn = new RouteStop();
        depotReturn.setRoutePlan(savedRoutePlan);
        depotReturn.setStopOrder(stopOrder);
        depotReturn.setStopType(RouteStop.StopType.DEPOT_RETURN);
        depotReturn.setLat(resolveDepotLat(routingRequest, depot));
        depotReturn.setLng(resolveDepotLng(routingRequest, depot));
        depotReturn.setStatus(RouteStop.StopStatus.PLANNED);
        depotReturn.setNotes("Return to depot after refueling");
        routeStopRepository.save(depotReturn);

        return savedMission;
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

    private Mission saveMissionFromRouting(RoutingMissionDto routingMissionDto,
                                           List<Truck> routingCandidateTrucks,
                                           RoutingRequestDto routingRequest,
                                           RoutingResponseDto routingResponse) {
        Truck truck = routingCandidateTrucks.stream()
                .filter(t -> t.getId().equals(routingMissionDto.getTruckId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Truck not found for routing mission: " + routingMissionDto.getTruckId()
                ));

        Driver driver = truck.getAssignedDriver();
        if (driver == null) {
            throw new BadRequestException("Truck " + truck.getId() + " has no assigned driver");
        }

        Depot depot = resolveActiveDepot();

        Mission mission = new Mission();
        mission.setMissionCode(generateMissionCode());
        mission.setDriver(driver);
        mission.setTruck(truck);

        mission.setStatus("CREATED");
        mission.setMissionStatusDetail(Mission.MissionStatusDetail.PLANNED);
        mission.setPriority(resolveMissionPriority(routingMissionDto));
        mission.setPlannedDate(LocalDate.now());
        mission.setCreatedAt(Instant.now());
        mission.setNotes("Generated automatically from routing engine");

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
                MissionBin missionBin = buildMissionBin(savedMission, stopDto, resolveVisitOrder(stopDto, fallbackOrder++));
                missionBinRepository.save(missionBin);
            }
        }

        saveRoutePlanAndStops(savedMission, truck, depot, routingMissionDto, routingRequest, routingResponse);

        return savedMission;
    }

    private void saveRoutePlanAndStops(Mission mission,
                                       Truck truck,
                                       Depot depot,
                                       RoutingMissionDto routingMissionDto,
                                       RoutingRequestDto routingRequest,
                                       RoutingResponseDto routingResponse) {
        RoutePlan routePlan = new RoutePlan();
        routePlan.setMission(mission);
        routePlan.setTruck(truck);
        routePlan.setDepot(depot);
        routePlan.setPlanType(RoutePlan.PlanType.INITIAL);
        routePlan.setPlanStatus(RoutePlan.PlanStatus.PLANNED);
        routePlan.setOptimizationAlgorithm("OR_TOOLS_OSRM");
        routePlan.setOptimizationVersion("v1");
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
        depotStart.setNotes("Start from depot");
        routeStopRepository.save(depotStart);

        RecommendedFuelStationDto stationDto =
                findRecommendedFuelStationForTruck(routingResponse, truck.getId());

        if (stationDto != null && stationDto.getStationId() != null) {
            FuelStation fuelStation = fuelStationRepository.findById(stationDto.getStationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Fuel station not found: " + stationDto.getStationId()
                    ));

            RouteStop fuelStop = new RouteStop();
            fuelStop.setRoutePlan(savedRoutePlan);
            fuelStop.setStopOrder(stopOrder++);
            fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
            fuelStop.setFuelStation(fuelStation);
            fuelStop.setLat(fuelStation.getLat());
            fuelStop.setLng(fuelStation.getLng());
            fuelStop.setStatus(RouteStop.StopStatus.PLANNED);
            fuelStop.setNotes("Refuel at station: " + fuelStation.getName());
            routeStopRepository.save(fuelStop);
        }

        if (routingMissionDto.getStops() != null) {
            for (RoutingStopDto stopDto : routingMissionDto.getStops()) {
                Bin bin = binRepository.findById(stopDto.getBinId())
                        .orElseThrow(() -> new ResourceNotFoundException("Bin not found: " + stopDto.getBinId()));

                RouteStop routeStop = new RouteStop();
                routeStop.setRoutePlan(savedRoutePlan);
                routeStop.setStopOrder(stopOrder++);
                routeStop.setStopType(RouteStop.StopType.BIN_PICKUP);
                routeStop.setBin(bin);

                routeStop.setLat(resolveBinRoutingLat(bin));
                routeStop.setLng(resolveBinRoutingLng(bin));

                routeStop.setStatus(RouteStop.StopStatus.PLANNED);
                routeStop.setNotes("Mission bin pickup");
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
        depotReturn.setNotes("Return to depot");
        routeStopRepository.save(depotReturn);
    }

    private RecommendedFuelStationDto findRecommendedFuelStationForTruck(
            RoutingResponseDto routingResponse,
            Long truckId
    ) {
        if (routingResponse == null || routingResponse.getRecommendedFuelStations() == null || truckId == null) {
            return null;
        }

        return routingResponse.getRecommendedFuelStations()
                .stream()
                .filter(s -> truckId.equals(s.getTruckId()))
                .findFirst()
                .orElse(null);
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

    private MissionBin buildMissionBin(Mission mission, RoutingStopDto stopDto, int visitOrder) {
        Bin bin = binRepository.findById(stopDto.getBinId())
                .orElseThrow(() -> new ResourceNotFoundException("Bin not found: " + stopDto.getBinId()));

        MissionBin missionBin = new MissionBin();
        missionBin.setMission(mission);
        missionBin.setBin(bin);
        missionBin.setVisitOrder(visitOrder);
        missionBin.setAssignedReason("PREDICTION");
        missionBin.setCollected(false);
        missionBin.setAssignmentStatus(MissionBin.AssignmentStatus.PLANNED);
        missionBin.setTargetFillThreshold((short) 80);

        return missionBin;
    }

    private int resolveVisitOrder(RoutingStopDto stopDto, int fallbackOrder) {
        if (stopDto != null && stopDto.getOrderIndex() != null && stopDto.getOrderIndex() > 0) {
            return stopDto.getOrderIndex();
        }
        return fallbackOrder;
    }

    private String generateMissionCode() {
        return "MIS-" + System.currentTimeMillis();
    }

    private String resolveMissionPriority(RoutingMissionDto routingMissionDto) {
        if (routingMissionDto.getStops() != null && routingMissionDto.getStops().size() >= 8) {
            return "HIGH";
        }
        return "NORMAL";
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

        long totalBins = missionBinRepository.countByMissionId(mission.getId());
        dto.setNotes(mission.getNotes() + " | totalBins=" + totalBins);

        return dto;
    }
}