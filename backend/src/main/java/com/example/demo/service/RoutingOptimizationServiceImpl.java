package com.example.demo.service;

import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.ExcludedTruckDto;
import com.example.demo.dto.routing.RecommendedFuelStationDto;
import com.example.demo.dto.routing.RoutingMissionDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingResponseDto;
import com.example.demo.dto.routing.RoutingStopDto;
import com.example.demo.entity.*;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
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

        List<Truck> routingCandidateTrucks = activeTrucks.stream()
                .filter(this::isRoutingCandidate)
                .toList();

        RoutingRequestDto routingRequest =
                routingPayloadBuilderService.buildRoutingRequest(routingCandidateTrucks);

        RoutingResponseDto routingResponse =
                pythonRoutingClient.optimizeRoutes(routingRequest);

        if (routingResponse != null) {
            routingResponse.setRecommendedFuelStations(
                    routingPayloadBuilderService.getLastRecommendedFuelStations()
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

        RoutingRequestDto routingRequest =
                routingPayloadBuilderService.buildRoutingRequest(routingCandidateTrucks);

        RoutingResponseDto routingResponse =
                pythonRoutingClient.optimizeRoutes(routingRequest);

        if (routingResponse == null) {
            throw new BadRequestException("Routing engine returned null response");
        }

        routingResponse.setRecommendedFuelStations(
                routingPayloadBuilderService.getLastRecommendedFuelStations()
        );

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
            throw new BadRequestException("No missions saved");
        }

        return savedMissions;
    }

    private void saveExcludedRefuelMissions(
            RoutingResponseDto routingResponse,
            List<Truck> routingCandidateTrucks,
            RoutingRequestDto routingRequest,
            List<MissionResponse> savedMissions) {

        if (routingResponse.getExcludedTrucks() == null) return;

        for (ExcludedTruckDto excludedTruck : routingResponse.getExcludedTrucks()) {

            if (excludedTruck.getTruckId() == null) continue;

            if (!excludedTruck.getReason().contains("REFUEL")) continue;

            Truck truck = routingCandidateTrucks.stream()
                    .filter(t -> excludedTruck.getTruckId().equals(t.getId()))
                    .findFirst()
                    .orElse(null);

            if (truck == null) continue;

            RecommendedFuelStationDto stationDto =
                    findRecommendedFuelStationForTruck(routingResponse, truck.getId());

            if (stationDto == null) continue;

            Mission mission = saveRefuelOnlyMission(truck, routingRequest, stationDto);
            savedMissions.add(mapMissionToResponse(mission));
        }
    }

    private Mission saveRefuelOnlyMission(
            Truck truck,
            RoutingRequestDto routingRequest,
            RecommendedFuelStationDto stationDto) {

        Driver driver = truck.getAssignedDriver();

        Depot depot = resolveActiveDepot();

        FuelStation fuelStation = fuelStationRepository.findById(stationDto.getStationId())
                .orElseThrow();

        Mission mission = new Mission();
        mission.setMissionCode(generateMissionCode());
        mission.setDriver(driver);
        mission.setTruck(truck);
        mission.setStatus("CREATED");
        mission.setPriority("HIGH");
        mission.setPlannedDate(LocalDate.now());
        mission.setCreatedAt(Instant.now());

        Mission savedMission = missionRepository.save(mission);

        RoutePlan routePlan = new RoutePlan();
        routePlan.setMission(savedMission);
        routePlan.setTruck(truck);
        routePlan.setDepot(depot);
        routePlan.setPlanType(RoutePlan.PlanType.EMERGENCY);

        RoutePlan savedPlan = routePlanRepository.save(routePlan);

        RouteStop fuelStop = new RouteStop();
        fuelStop.setRoutePlan(savedPlan);
        fuelStop.setStopOrder(1);
        fuelStop.setStopType(RouteStop.StopType.FUEL_STATION);
        fuelStop.setFuelStation(fuelStation);
        fuelStop.setLat(fuelStation.getLat());
        fuelStop.setLng(fuelStation.getLng());

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
            RoutingResponseDto routingResponse) {

        Truck truck = trucks.stream()
                .filter(t -> t.getId().equals(routingMissionDto.getTruckId()))
                .findFirst()
                .orElseThrow();

        Mission mission = new Mission();
        mission.setMissionCode(generateMissionCode());
        mission.setDriver(truck.getAssignedDriver());
        mission.setTruck(truck);
        mission.setStatus("CREATED");
        mission.setCreatedAt(Instant.now());

        Mission saved = missionRepository.save(mission);

        saveRoutePlanAndStops(saved, truck, routingMissionDto, routingRequest, routingResponse);

        return saved;
    }

    private void saveRoutePlanAndStops(
            Mission mission,
            Truck truck,
            RoutingMissionDto routingMissionDto,
            RoutingRequestDto routingRequest,
            RoutingResponseDto routingResponse) {

        RoutePlan routePlan = new RoutePlan();
        routePlan.setMission(mission);
        routePlan.setTruck(truck);

        RoutePlan savedPlan = routePlanRepository.save(routePlan);

        int order = 1;

        RecommendedFuelStationDto stationDto =
                findRecommendedFuelStationForTruck(routingResponse, truck.getId());

        if (stationDto != null) {
            FuelStation fs = fuelStationRepository.findById(stationDto.getStationId()).orElseThrow();

            RouteStop stop = new RouteStop();
            stop.setRoutePlan(savedPlan);
            stop.setStopOrder(order++);
            stop.setStopType(RouteStop.StopType.FUEL_STATION);
            stop.setFuelStation(fs);
            stop.setLat(fs.getLat());
            stop.setLng(fs.getLng());

            routeStopRepository.save(stop);
        }
    }

    private RecommendedFuelStationDto findRecommendedFuelStationForTruck(
            RoutingResponseDto routingResponse,
            Long truckId) {

        if (routingResponse.getRecommendedFuelStations() == null) return null;

        return routingResponse.getRecommendedFuelStations()
                .stream()
                .filter(s -> truckId.equals(s.getTruckId()))
                .findFirst()
                .orElse(null);
    }

    private Depot resolveActiveDepot() {
        return depotRepository.findByIsActiveTrue().get(0);
    }

    private String generateMissionCode() {
        return "MIS-" + System.currentTimeMillis();
    }

    private MissionResponse mapMissionToResponse(Mission mission) {
        MissionResponse dto = new MissionResponse();
        dto.setId(mission.getId());
        dto.setMissionCode(mission.getMissionCode());
        return dto;
    }
}