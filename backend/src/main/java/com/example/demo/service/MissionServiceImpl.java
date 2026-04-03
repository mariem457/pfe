package com.example.demo.service;

import com.example.demo.dto.MissionBinActionRequest;
import com.example.demo.dto.MissionBinResponse;
import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.MissionRouteResponse;
import com.example.demo.dto.MissionRouteStopDto;
import com.example.demo.dto.RouteCoordinateDto;
import com.example.demo.entity.Driver;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.MissionBin.AssignmentStatus;
import com.example.demo.entity.RoutePlan;
import com.example.demo.entity.RouteStop;
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

    private final MissionRepository missionRepository;
    private final MissionBinRepository missionBinRepository;
    private final DriverRepository driverRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteStopRepository routeStopRepository;
    private final WebClient.Builder webClientBuilder;

    public MissionServiceImpl(MissionRepository missionRepository,
                              MissionBinRepository missionBinRepository,
                              DriverRepository driverRepository,
                              RoutePlanRepository routePlanRepository,
                              RouteStopRepository routeStopRepository,
                              WebClient.Builder webClientBuilder) {
        this.missionRepository = missionRepository;
        this.missionBinRepository = missionBinRepository;
        this.driverRepository = driverRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeStopRepository = routeStopRepository;
        this.webClientBuilder = webClientBuilder;
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

                if (mission.getStartedAt() == null) {
                    mission.setStartedAt(Instant.now());
                }

                missionRepository.save(mission);
            }
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

        List<RouteStop> stops = routeStopRepository.findByRoutePlanOrderByStopOrderAsc(latestPlan);
        if (stops.isEmpty()) {
            throw new ResourceNotFoundException("No route stops found for mission: " + missionId);
        }

        List<RouteStop> usableStops = stops.stream()
                .filter(stop -> stop.getLat() != null && stop.getLng() != null)
                .sorted(Comparator.comparing(RouteStop::getStopOrder))
                .toList();

        if (usableStops.size() < 2) {
            throw new ResourceNotFoundException("Not enough route stops to build route for mission: " + missionId);
        }

        List<MissionRouteStopDto> routeStops = usableStops.stream().map(stop -> {
            MissionRouteStopDto stopDto = new MissionRouteStopDto();
            stopDto.setStopOrder(stop.getStopOrder());
            stopDto.setStopType(stop.getStopType() != null ? stop.getStopType().name() : null);
            stopDto.setBinId(stop.getBin() != null ? stop.getBin().getId() : null);
            stopDto.setFuelStationId(stop.getFuelStation() != null ? stop.getFuelStation().getId() : null);
            stopDto.setFuelStationName(stop.getFuelStation() != null ? stop.getFuelStation().getName() : null);
            stopDto.setLat(stop.getLat());
            stopDto.setLng(stop.getLng());
            return stopDto;
        }).toList();
        String coordinates = usableStops.stream()
                .map(stop -> stop.getLng() + "," + stop.getLat())
                .reduce((a, b) -> a + ";" + b)
                .orElseThrow();

        WebClient osrmClient = webClientBuilder
                .baseUrl("http://localhost:5000")
                .build();

        Map<String, Object> response = osrmClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/v1/driving/" + coordinates)
                        .queryParam("overview", "full")
                        .queryParam("geometries", "geojson")
                        .queryParam("steps", "false")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<RouteCoordinateDto> routeCoordinates = new ArrayList<>();
        List<RouteCoordinateDto> snappedWaypoints = new ArrayList<>();

        if (response != null) {
            Object routesObj = response.get("routes");
            if (routesObj instanceof List<?> routes && !routes.isEmpty()) {
                Object firstRouteObj = routes.get(0);
                if (firstRouteObj instanceof Map<?, ?> firstRoute) {
                    Object geometryObj = firstRoute.get("geometry");
                    if (geometryObj instanceof Map<?, ?> geometry) {
                        Object coordsObj = geometry.get("coordinates");
                        if (coordsObj instanceof List<?> coordsList) {
                            for (Object coordObj : coordsList) {
                                if (coordObj instanceof List<?> pair && pair.size() >= 2) {
                                    Object lngObj = pair.get(0);
                                    Object latObj = pair.get(1);

                                    if (lngObj instanceof Number lng && latObj instanceof Number lat) {
                                        routeCoordinates.add(
                                                new RouteCoordinateDto(lat.doubleValue(), lng.doubleValue())
                                        );
                                    }
                                }
                            }
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
                                snappedWaypoints.add(
                                        new RouteCoordinateDto(lat.doubleValue(), lng.doubleValue())
                                );
                            }
                        }
                    }
                }
            }
        }

        MissionRouteResponse dto = new MissionRouteResponse();
        dto.setMissionId(mission.getId());
        dto.setRoutePlanId(latestPlan.getId());

        if (latestPlan.getTruck() != null) {
            dto.setTruckId(latestPlan.getTruck().getId());
        }

        if (latestPlan.getTotalDistanceKm() != null) {
            dto.setTotalDistanceKm(
                    latestPlan.getTotalDistanceKm().setScale(2, RoundingMode.HALF_UP).doubleValue()
            );
        }

        dto.setEstimatedDurationMin(latestPlan.getEstimatedDurationMin());
        dto.setRouteCoordinates(routeCoordinates);
        dto.setRouteStops(routeStops);
        dto.setSnappedWaypoints(snappedWaypoints);

        // affichage front
        dto.setMatrixSource("TOMTOM");
        dto.setGeometrySource("OSRM");

        return dto;
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
        dto.setMissionId(missionBin.getMission().getId());
        dto.setBinId(missionBin.getBin().getId());
        dto.setBinCode(missionBin.getBin().getBinCode());
        dto.setLat(missionBin.getBin().getLat());
        dto.setLng(missionBin.getBin().getLng());
        dto.setVisitOrder(missionBin.getVisitOrder());
        dto.setTargetFillThreshold(missionBin.getTargetFillThreshold());
        dto.setAssignedReason(missionBin.getAssignedReason());
        dto.setCollected(missionBin.isCollected());
        dto.setCollectedAt(missionBin.getCollectedAt());

        if (missionBin.getCollectedBy() != null) {
            dto.setCollectedByDriverId(missionBin.getCollectedBy().getId());
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
}