package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class MissionRouteResponse {

    private Long missionId;
    private Long routePlanId;
    private Long truckId;
    private Double totalDistanceKm;
    private Integer estimatedDurationMin;
    private List<RouteCoordinateDto> routeCoordinates = new ArrayList<>();
    private List<MissionRouteStopDto> routeStops = new ArrayList<>();
    private List<RouteCoordinateDto> snappedWaypoints = new ArrayList<>();

    // NEW
    private String matrixSource;
    private String geometrySource;

    public MissionRouteResponse() {
    }

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public Long getRoutePlanId() {
        return routePlanId;
    }

    public void setRoutePlanId(Long routePlanId) {
        this.routePlanId = routePlanId;
    }

    public Long getTruckId() {
        return truckId;
    }

    public void setTruckId(Long truckId) {
        this.truckId = truckId;
    }

    public Double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(Double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public Integer getEstimatedDurationMin() {
        return estimatedDurationMin;
    }

    public void setEstimatedDurationMin(Integer estimatedDurationMin) {
        this.estimatedDurationMin = estimatedDurationMin;
    }

    public List<RouteCoordinateDto> getRouteCoordinates() {
        return routeCoordinates;
    }

    public void setRouteCoordinates(List<RouteCoordinateDto> routeCoordinates) {
        this.routeCoordinates = routeCoordinates;
    }

    public List<MissionRouteStopDto> getRouteStops() {
        return routeStops;
    }

    public void setRouteStops(List<MissionRouteStopDto> routeStops) {
        this.routeStops = routeStops;
    }

    public List<RouteCoordinateDto> getSnappedWaypoints() {
        return snappedWaypoints;
    }

    public void setSnappedWaypoints(List<RouteCoordinateDto> snappedWaypoints) {
        this.snappedWaypoints = snappedWaypoints;
    }

    public String getMatrixSource() {
        return matrixSource;
    }

    public void setMatrixSource(String matrixSource) {
        this.matrixSource = matrixSource;
    }

    public String getGeometrySource() {
        return geometrySource;
    }

    public void setGeometrySource(String geometrySource) {
        this.geometrySource = geometrySource;
    }
}