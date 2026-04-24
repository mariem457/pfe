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
    private List<RouteCoordinateDto> collectionRouteCoordinates = new ArrayList<>();
    private List<RouteCoordinateDto> transferRouteCoordinates = new ArrayList<>();

    private List<MissionRouteStopDto> routeStops = new ArrayList<>();

    private List<RouteCoordinateDto> snappedWaypoints = new ArrayList<>();
    private List<RouteCoordinateDto> collectionSnappedWaypoints = new ArrayList<>();
    private List<RouteCoordinateDto> transferSnappedWaypoints = new ArrayList<>();

    private List<Double> stopLegDistancesKm = new ArrayList<>();

    private Double collectionDistanceKm;
    private Double transferDistanceKm;

    private String matrixSource;
    private String geometrySource;

    private List<String> validationWarnings = new ArrayList<>();

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

    public List<RouteCoordinateDto> getCollectionRouteCoordinates() {
        return collectionRouteCoordinates;
    }

    public void setCollectionRouteCoordinates(List<RouteCoordinateDto> collectionRouteCoordinates) {
        this.collectionRouteCoordinates = collectionRouteCoordinates;
    }

    public List<RouteCoordinateDto> getTransferRouteCoordinates() {
        return transferRouteCoordinates;
    }

    public void setTransferRouteCoordinates(List<RouteCoordinateDto> transferRouteCoordinates) {
        this.transferRouteCoordinates = transferRouteCoordinates;
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

    public List<RouteCoordinateDto> getCollectionSnappedWaypoints() {
        return collectionSnappedWaypoints;
    }

    public void setCollectionSnappedWaypoints(List<RouteCoordinateDto> collectionSnappedWaypoints) {
        this.collectionSnappedWaypoints = collectionSnappedWaypoints;
    }

    public List<RouteCoordinateDto> getTransferSnappedWaypoints() {
        return transferSnappedWaypoints;
    }

    public void setTransferSnappedWaypoints(List<RouteCoordinateDto> transferSnappedWaypoints) {
        this.transferSnappedWaypoints = transferSnappedWaypoints;
    }

    public List<Double> getStopLegDistancesKm() {
        return stopLegDistancesKm;
    }

    public void setStopLegDistancesKm(List<Double> stopLegDistancesKm) {
        this.stopLegDistancesKm = stopLegDistancesKm;
    }

    public Double getCollectionDistanceKm() {
        return collectionDistanceKm;
    }

    public void setCollectionDistanceKm(Double collectionDistanceKm) {
        this.collectionDistanceKm = collectionDistanceKm;
    }

    public Double getTransferDistanceKm() {
        return transferDistanceKm;
    }

    public void setTransferDistanceKm(Double transferDistanceKm) {
        this.transferDistanceKm = transferDistanceKm;
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

    public List<String> getValidationWarnings() {
        return validationWarnings;
    }

    public void setValidationWarnings(List<String> validationWarnings) {
        this.validationWarnings = validationWarnings;
    }
}