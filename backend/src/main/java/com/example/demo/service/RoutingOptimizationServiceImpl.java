package com.example.demo.service;

import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.ExcludedTruckDto;
import com.example.demo.dto.routing.RecommendedFuelStationDto;
import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.dto.routing.RoutingMissionDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingResponseDto;
import com.example.demo.dto.routing.RoutingStopDto;
import com.example.demo.entity.Depot;
import com.example.demo.entity.Driver;
import com.example.demo.entity.FuelStation;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.PostponedBin;
import com.example.demo.entity.RoutePlan;
import com.example.demo.entity.RouteStop;
import com.example.demo.entity.RoutingExecutionLog;
import com.example.demo.entity.Truck;
import com.example.demo.exception.BadRequestException;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.DepotRepository;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.FuelStationRepository;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.PostponedBinRepository;
import com.example.demo.repository.RoutePlanRepository;
import com.example.demo.repository.RouteStopRepository;
import com.example.demo.repository.RoutingExecutionLogRepository;
import com.example.demo.repository.TruckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final PostponedBinRepository postponedBinRepository;
    private final SmartRoutingDecisionService smartRoutingDecisionService;
    private final RoutingExecutionLogRepository routingExecutionLogRepository;
    private final DriverRepository driverRepository;

    public RoutingOptimizationServiceImpl(
            TruckRepository truckRepository,
            BinRepository binRepository,
            DepotRepository depotRepository,
            MissionRepository missionRepository,
            MissionBinRepository missionBinRepository,
            RoutePlanRepository routePlanRepository,
            RouteStopRepository routeStopRepository,
            RoutingPayloadBuilderService routingPayloadBuilderService,
            PythonRoutingClient pythonRoutingClient,
            FuelStationRepository fuelStationRepository,
            PostponedBinRepository postponedBinRepository,
            SmartRoutingDecisionService smartRoutingDecisionService,
            RoutingExecutionLogRepository routingExecutionLogRepository,
            DriverRepository driverRepository
    ) {
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
        this.postponedBinRepository = postponedBinRepository;
        this.smartRoutingDecisionService = smartRoutingDecisionService;
        this.routingExecutionLogRepository = routingExecutionLogRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public RoutingResponseDto prepareInitialRouting() {
        List<Truck> activeTrucks = truckRepository.findByIsActiveTrue();

        List<Truck> routingCandidateTrucks = activeTrucks.stream()
                .filter(this::isRoutingCandidate)
                .toList();

        if (routingCandidateTrucks.isEmpty()) {
            RoutingResponseDto empty = new RoutingResponseDto();
            empty.setMatrixSource("NONE");
            saveExecutionLog(RoutingDecision.skip("No routing candidate trucks"), null, empty, 0);
            return empty;
        }

        RoutingDecision decision = smartRoutingDecisionService.makeDecision(routingCandidateTrucks);

        System.out.println("Routing decision => shouldOptimize=" + decision.isShouldOptimize()
                + ", refuelOnly=" + decision.isRefuelOnly()
                + ", includeOpportunistic=" + decision.isIncludeOpportunistic()
                + ", strategy=" + decision.getStrategy()
                + ", reason=" + decision.getReason());

        if (decision.isRefuelOnly()) {
            RoutingRequestDto refuelRoutingRequest =
                    routingPayloadBuilderService.buildRoutingRequest(routingCandidateTrucks, decision);

            RoutingResponseDto refuelResponse = new RoutingResponseDto();
            refuelResponse.setMatrixSource("REFUEL_ONLY");
            refuelResponse.setExcludedTrucks(buildRefuelOnlyExcludedTrucks(routingCandidateTrucks));
            refuelResponse.setRecommendedFuelStations(
                    routingPayloadBuilderService.getLastRecommendedFuelStations()
            );

            saveExecutionLog(decision, refuelRoutingRequest, refuelResponse, 0);
            return refuelResponse;
        }

        if (!decision.isShouldOptimize()) {
            RoutingResponseDto skipped = new RoutingResponseDto();
            skipped.setMatrixSource("SKIPPED");
            saveExecutionLog(decision, null, skipped, 0);
            return skipped;
        }

        RoutingRequestDto routingRequest =
                routingPayloadBuilderService.buildRoutingRequest(routingCandidateTrucks, decision);

        RoutingResponseDto routingResponse =
                pythonRoutingClient.optimizeRoutes(routingRequest);

        if (routingResponse != null) {
            routingResponse.setRecommendedFuelStations(
                    routingPayloadBuilderService.getLastRecommendedFuelStations()
            );
        }

        saveExecutionLog(
                decision,
                routingRequest,
                routingResponse,
                routingResponse != null && routingResponse.getMissions() != null
                        ? routingResponse.getMissions().size()
                        : 0
        );

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
            RoutingDecision decision = RoutingDecision.skip("No routing candidate trucks available");
            saveExecutionLog(decision, null, null, 0);
            throw new BadRequestException("No routing candidate trucks available");
        }

        RoutingDecision decision = smartRoutingDecisionService.makeDecision(routingCandidateTrucks);

        System.out.println("Routing decision => shouldOptimize=" + decision.isShouldOptimize()
                + ", refuelOnly=" + decision.isRefuelOnly()
                + ", includeOpportunistic=" + decision.isIncludeOpportunistic()
                + ", strategy=" + decision.getStrategy()
                + ", reason=" + decision.getReason());

        List<MissionResponse> savedMissions = new ArrayList<>();

        if (decision.isRefuelOnly()) {
            RoutingRequestDto refuelRoutingRequest =
                    routingPayloadBuilderService.buildRoutingRequest(routingCandidateTrucks, decision);

            RoutingResponseDto refuelResponse = new RoutingResponseDto();
            refuelResponse.setMatrixSource("REFUEL_ONLY");
            refuelResponse.setExcludedTrucks(buildRefuelOnlyExcludedTrucks(routingCandidateTrucks));
            refuelResponse.setRecommendedFuelStations(
                    routingPayloadBuilderService.getLastRecommendedFuelStations()
            );

            saveExcludedRefuelMissions(
                    refuelResponse,
                    routingCandidateTrucks,
                    refuelRoutingRequest,
                    savedMissions
            );

            saveExecutionLog(
                    decision,
                    refuelRoutingRequest,
                    refuelResponse,
                    savedMissions.size()
            );

            if (savedMissions.isEmpty()) {
                throw new BadRequestException("Refuel-only decision produced no missions");
            }

            return savedMissions;
        }

        if (!decision.isShouldOptimize()) {
            saveExecutionLog(decision, null, null, 0);
            throw new BadRequestException("Routing skipped: " + decision.getReason());
        }

        RoutingRequestDto routingRequest =
                routingPayloadBuilderService.buildRoutingRequest(routingCandidateTrucks, decision);

        if (routingRequest.getBins() == null || routingRequest.getBins().isEmpty()) {
            saveExecutionLog(decision, routingRequest, null, 0);
            throw new BadRequestException("No bins selected for routing after decision filtering");
        }

        RoutingResponseDto routingResponse =
                pythonRoutingClient.optimizeRoutes(routingRequest);

        if (routingResponse == null) {
            saveExecutionLog(decision, routingRequest, null, 0);
            throw new BadRequestException("Routing engine returned null response");
        }

        routingResponse.setRecommendedFuelStations(
                routingPayloadBuilderService.getLastRecommendedFuelStations()
        );

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

        saveDroppedBins(routingRequest, routingResponse);

        saveExecutionLog(
                decision,
                routingRequest,
                routingResponse,
                savedMissions.size()
        );

        if (savedMissions.isEmpty()) {
            throw new BadRequestException("No missions saved");
        }

        return savedMissions;
    }

    private void saveExecutionLog(
            RoutingDecision decision,
            RoutingRequestDto routingRequest,
            RoutingResponseDto routingResponse,
            int missionsCreatedCount
    ) {
        RoutingExecutionLog log = new RoutingExecutionLog();

        if (decision != null) {
            log.setStrategy(decision.getStrategy() != null ? decision.getStrategy().name() : null);
            log.setReason(decision.getReason());
            log.setShouldOptimize(decision.isShouldOptimize());
            log.setRefuelOnly(decision.isRefuelOnly());
        } else {
            log.setShouldOptimize(false);
            log.setRefuelOnly(false);
        }

        int trucksCount = routingRequest != null && routingRequest.getTrucks() != null
                ? routingRequest.getTrucks().size()
                : 0;
        log.setTrucksCount(trucksCount);

        int binsSentCount = routingRequest != null && routingRequest.getBins() != null
                ? routingRequest.getBins().size()
                : 0;
        log.setBinsSentCount(binsSentCount);

        int mandatoryBinsCount = routingRequest != null && routingRequest.getBins() != null
                ? (int) routingRequest.getBins().stream().filter(b -> Boolean.TRUE.equals(b.getMandatory())).count()
                : 0;
        log.setMandatoryBinsCount(mandatoryBinsCount);

        log.setMissionsCreatedCount(missionsCreatedCount);

        int droppedBinsCount = routingResponse != null && routingResponse.getDroppedBinIds() != null
                ? routingResponse.getDroppedBinIds().size()
                : 0;
        log.setDroppedBinsCount(droppedBinsCount);

        log.setMatrixSource(routingResponse != null ? routingResponse.getMatrixSource() : null);

        routingExecutionLogRepository.save(log);
    }

    private List<ExcludedTruckDto> buildRefuelOnlyExcludedTrucks(List<Truck> trucks) {
        List<ExcludedTruckDto> excluded = new ArrayList<>();

        for (Truck truck : trucks) {
            if (truck == null || truck.getId() == null) {
                continue;
            }

            ExcludedTruckDto dto = new ExcludedTruckDto();
            dto.setTruckId(truck.getId());
            dto.setReason("REFUEL_ONLY_DECISION");
            excluded.add(dto);
        }

        return excluded;
    }

    private void saveExcludedRefuelMissions(
            RoutingResponseDto routingResponse,
            List<Truck> routingCandidateTrucks,
            RoutingRequestDto routingRequest,
            List<MissionResponse> savedMissions
    ) {
        if (routingResponse.getExcludedTrucks() == null) {
            return;
        }

        for (ExcludedTruckDto excludedTruck : routingResponse.getExcludedTrucks()) {
            if (excludedTruck.getTruckId() == null) {
                continue;
            }

            if (excludedTruck.getReason() == null || !excludedTruck.getReason().contains("REFUEL")) {
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

            if (stationDto == null) {
                continue;
            }

            Mission mission = saveRefuelOnlyMission(truck, routingRequest, stationDto);
            savedMissions.add(mapMissionToResponse(mission));
        }
    }

    private Mission saveRefuelOnlyMission(
            Truck truck,
            RoutingRequestDto routingRequest,
            RecommendedFuelStationDto stationDto
    ) {
        Driver driver = resolveDriverForTruck(truck);
        Depot depot = resolveActiveDepot();

        FuelStation fuelStation = fuelStationRepository.findById(stationDto.getStationId())
                .orElseThrow();

        Mission mission = new Mission();
        mission.setMissionCode(generateMissionCode());
        mission.setDriver(driver);
        mission.setTruck(truck);
        mission.setDepot(depot);
        mission.setZone(truck.getZone());
        mission.setStatus("CREATED");
        mission.setMissionStatusDetail(Mission.MissionStatusDetail.PLANNED);
        mission.setPriority("HIGH");
        mission.setPlannedDate(LocalDate.now());
        mission.setCreatedAt(Instant.now());
        mission.setNotes("Auto-generated refuel mission");

        Mission savedMission = missionRepository.save(mission);

        RoutePlan routePlan = new RoutePlan();
        routePlan.setMission(savedMission);
        routePlan.setTruck(truck);
        routePlan.setDepot(depot);
        routePlan.setPlanType(RoutePlan.PlanType.EMERGENCY);
        routePlan.setPlanStatus(RoutePlan.PlanStatus.PLANNED);
        RoutePlan savedPlan = routePlanRepository.save(routePlan);

        RouteStop fuelStop = new RouteStop();
        fuelStop.setRoutePlan(savedPlan);
        fuelStop.setStopOrder(1);
        fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
        fuelStop.setFuelStation(fuelStation);
        fuelStop.setLat(fuelStation.getLat());
        fuelStop.setLng(fuelStation.getLng());
        fuelStop.setStatus(RouteStop.StopStatus.PLANNED);
        fuelStop.setNotes("Auto-generated refuel stop");
        routeStopRepository.save(fuelStop);

        return savedMission;
    }

    private boolean isRoutingCandidate(Truck truck) {
        return truck != null
                && truck.getIsActive() != null
                && truck.getIsActive()
                && truck.getStatus() == Truck.TruckStatus.AVAILABLE;
    }

    private Mission saveMissionFromRouting(
            RoutingMissionDto routingMissionDto,
            List<Truck> trucks,
            RoutingRequestDto routingRequest,
            RoutingResponseDto routingResponse
    ) {
        Truck truck = trucks.stream()
                .filter(t -> t.getId().equals(routingMissionDto.getTruckId()))
                .findFirst()
                .orElseThrow();

        Driver driver = resolveDriverForTruck(truck);

        Mission mission = new Mission();
        mission.setMissionCode(generateMissionCode());
        mission.setDriver(driver);
        mission.setTruck(truck);
        mission.setDepot(resolveActiveDepot());
        mission.setZone(truck.getZone());
        mission.setStatus("CREATED");
        mission.setMissionStatusDetail(Mission.MissionStatusDetail.PLANNED);
        mission.setPriority("NORMAL");
        mission.setPlannedDate(LocalDate.now());
        mission.setCreatedAt(Instant.now());

        if (routingMissionDto.getTotalDistanceKm() != null) {
            mission.setEstimatedDistanceKm(
                    BigDecimal.valueOf(routingMissionDto.getTotalDistanceKm())
            );
        }

        if (routingMissionDto.getTotalDurationMinutes() != null) {
            mission.setEstimatedDurationMin(
                    (int) Math.round(routingMissionDto.getTotalDurationMinutes())
            );
        }

        Mission saved = missionRepository.save(mission);

        saveMissionBins(saved, truck, routingMissionDto);
        saveRoutePlanAndStops(saved, truck, routingMissionDto, routingRequest, routingResponse);
        resolvePostponedHistoryForServedBins(routingMissionDto);

        return saved;
    }

    private void resolvePostponedHistoryForServedBins(RoutingMissionDto routingMissionDto) {
        if (routingMissionDto == null || routingMissionDto.getStops() == null) {
            return;
        }

        for (RoutingStopDto stopDto : routingMissionDto.getStops()) {
            if (stopDto.getBinId() == null) {
                continue;
            }

            List<PostponedBin> activePostponedBins =
                    postponedBinRepository.findByBinIdAndResolvedFalse(stopDto.getBinId());

            for (PostponedBin postponedBin : activePostponedBins) {
                postponedBin.setResolved(true);
                postponedBin.setResolvedAt(Instant.now());
            }

            postponedBinRepository.saveAll(activePostponedBins);
        }
    }

    private void saveMissionBins(
            Mission mission,
            Truck truck,
            RoutingMissionDto routingMissionDto
    ) {
        if (routingMissionDto.getStops() == null || routingMissionDto.getStops().isEmpty()) {
            return;
        }

        for (RoutingStopDto stopDto : routingMissionDto.getStops()) {
            if (stopDto.getBinId() == null) {
                continue;
            }

            binRepository.findById(stopDto.getBinId()).ifPresent(bin -> {
                MissionBin missionBin = new MissionBin();
                missionBin.setMission(mission);
                missionBin.setBin(bin);
                missionBin.setVisitOrder(stopDto.getOrderIndex() != null ? stopDto.getOrderIndex() : 999);
                missionBin.setAssignedReason("PREDICTION");
                missionBin.setCollected(false);
                missionBin.setAssignmentStatus(MissionBin.AssignmentStatus.PLANNED);
                missionBin.setReassignedToTruck(truck);
                missionBin.setTargetFillThreshold((short) 80);
                missionBinRepository.save(missionBin);
            });
        }
    }

    private void saveRoutePlanAndStops(
            Mission mission,
            Truck truck,
            RoutingMissionDto routingMissionDto,
            RoutingRequestDto routingRequest,
            RoutingResponseDto routingResponse
    ) {
        RoutePlan routePlan = new RoutePlan();
        routePlan.setMission(mission);
        routePlan.setTruck(truck);
        routePlan.setDepot(resolveActiveDepot());
        routePlan.setPlanType(RoutePlan.PlanType.INITIAL);
        routePlan.setPlanStatus(RoutePlan.PlanStatus.PLANNED);

        if (routingMissionDto.getTotalDistanceKm() != null) {
            routePlan.setTotalDistanceKm(BigDecimal.valueOf(routingMissionDto.getTotalDistanceKm()));
        }

        if (routingMissionDto.getTotalDurationMinutes() != null) {
            routePlan.setEstimatedDurationMin((int) Math.round(routingMissionDto.getTotalDurationMinutes()));
        }

        RoutePlan savedPlan = routePlanRepository.save(routePlan);

        Depot activeDepot = resolveActiveDepot();
        Double depotLat = routingRequest != null && routingRequest.getDepot() != null
                ? routingRequest.getDepot().getLat()
                : activeDepot.getLat();
        Double depotLng = routingRequest != null && routingRequest.getDepot() != null
                ? routingRequest.getDepot().getLng()
                : activeDepot.getLng();

        if (!isValidCoordinatePair(depotLat, depotLng)) {
            throw new BadRequestException("Depot coordinates are invalid while saving route plan");
        }

        int nextOrder = 1;

        RouteStop depotStart = new RouteStop();
        depotStart.setRoutePlan(savedPlan);
        depotStart.setStopOrder(nextOrder++);
        depotStart.setStopType(RouteStop.StopType.DEPOT_START);
        depotStart.setLat(depotLat);
        depotStart.setLng(depotLng);
        depotStart.setStatus(RouteStop.StopStatus.PLANNED);
        depotStart.setNotes("Route start from depot");
        routeStopRepository.save(depotStart);

        RecommendedFuelStationDto stationDto =
                findRecommendedFuelStationForTruck(routingResponse, truck.getId());

        if (stationDto != null) {
            FuelStation fs = fuelStationRepository.findById(stationDto.getStationId()).orElseThrow();

            RouteStop fuelStop = new RouteStop();
            fuelStop.setRoutePlan(savedPlan);
            fuelStop.setStopOrder(nextOrder++);
            fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
            fuelStop.setFuelStation(fs);
            fuelStop.setLat(fs.getLat());
            fuelStop.setLng(fs.getLng());
            fuelStop.setStatus(RouteStop.StopStatus.PLANNED);
            fuelStop.setNotes("Recommended fuel stop");
            routeStopRepository.save(fuelStop);
        }

        if (routingMissionDto.getStops() != null) {
            List<RoutingStopDto> orderedStops = routingMissionDto.getStops().stream()
                    .filter(s -> s.getBinId() != null)
                    .sorted(Comparator.comparing(
                            s -> s.getOrderIndex() != null ? s.getOrderIndex() : Integer.MAX_VALUE
                    ))
                    .toList();

            for (RoutingStopDto stopDto : orderedStops) {
                int currentOrder = nextOrder++;

                binRepository.findById(stopDto.getBinId()).ifPresent(bin -> {
                    Double lat = bin.getAccessLat() != null ? bin.getAccessLat() : bin.getLat();
                    Double lng = bin.getAccessLng() != null ? bin.getAccessLng() : bin.getLng();

                    if (!isValidCoordinatePair(lat, lng)) {
                        throw new BadRequestException("Bin " + bin.getId() + " has invalid routing coordinates");
                    }

                    RouteStop stop = new RouteStop();
                    stop.setRoutePlan(savedPlan);
                    stop.setStopOrder(currentOrder);
                    stop.setStopType(RouteStop.StopType.BIN_PICKUP);
                    stop.setBin(bin);
                    stop.setLat(lat);
                    stop.setLng(lng);
                    stop.setStatus(RouteStop.StopStatus.PLANNED);
                    stop.setNotes("Optimized route bin pickup");
                    routeStopRepository.save(stop);
                });
            }
        }

        RouteStop depotReturn = new RouteStop();
        depotReturn.setRoutePlan(savedPlan);
        depotReturn.setStopOrder(nextOrder);
        depotReturn.setStopType(RouteStop.StopType.DEPOT_RETURN);
        depotReturn.setLat(depotLat);
        depotReturn.setLng(depotLng);
        depotReturn.setStatus(RouteStop.StopStatus.PLANNED);
        depotReturn.setNotes("Return to depot");
        routeStopRepository.save(depotReturn);
    }

    private void saveDroppedBins(RoutingRequestDto routingRequest, RoutingResponseDto routingResponse) {
        if (routingResponse.getDroppedBinIds() == null || routingResponse.getDroppedBinIds().isEmpty()) {
            return;
        }

        for (Long droppedBinId : routingResponse.getDroppedBinIds()) {
            if (droppedBinId == null) {
                continue;
            }

            binRepository.findById(droppedBinId).ifPresent(bin -> {
                PostponedBin postponedBin = new PostponedBin();
                postponedBin.setBin(bin);
                postponedBin.setReason("DROPPED_BY_ROUTING_ENGINE");
                postponedBin.setResolved(false);
                postponedBin.setResolvedAt(null);

                RoutingBinDto payloadBin = null;
                if (routingRequest.getBins() != null) {
                    payloadBin = routingRequest.getBins().stream()
                            .filter(b -> droppedBinId.equals(b.getId()))
                            .findFirst()
                            .orElse(null);
                }

                if (payloadBin != null) {
                    postponedBin.setPredictedPriority(payloadBin.getPredictedPriority());
                    postponedBin.setPredictedHoursToFull(payloadBin.getPredictedHoursToFull());
                    postponedBin.setFillLevel(payloadBin.getFillLevel());
                    postponedBin.setEstimatedLoadKg(payloadBin.getEstimatedLoadKg());
                }

                postponedBinRepository.save(postponedBin);
            });
        }
    }

    private RecommendedFuelStationDto findRecommendedFuelStationForTruck(
            RoutingResponseDto routingResponse,
            Long truckId
    ) {
        if (routingResponse != null && routingResponse.getRecommendedFuelStations() != null) {
            RecommendedFuelStationDto dto = routingResponse.getRecommendedFuelStations()
                    .stream()
                    .filter(s -> truckId.equals(s.getTruckId()))
                    .findFirst()
                    .orElse(null);

            if (dto != null) {
                return dto;
            }
        }

        return routingPayloadBuilderService.getLastRecommendedFuelStations()
                .stream()
                .filter(s -> truckId.equals(s.getTruckId()))
                .findFirst()
                .orElse(null);
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

    private Depot resolveActiveDepot() {
        return depotRepository.findByIsActiveTrue()
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No active depot found"));
    }

    private boolean isValidCoordinatePair(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180
                && !(lat == 0.0 && lng == 0.0);
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