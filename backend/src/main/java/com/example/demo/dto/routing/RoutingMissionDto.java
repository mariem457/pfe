package com.example.demo.dto.routing;

import java.util.ArrayList;
import java.util.List;

public class RoutingMissionDto {

    private Long truckId;
    private Double totalDistanceKm;
    private Double totalDurationMinutes;
    private List<RoutingStopDto> stops;
    private List<RouteCoordinateDto> routeCoordinates = new ArrayList<>();

    public RoutingMissionDto() {
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

    public Double getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public void setTotalDurationMinutes(Double totalDurationMinutes) {
        this.totalDurationMinutes = totalDurationMinutes;
    }

    public List<RoutingStopDto> getStops() {
        return stops;
    }

    public void setStops(List<RoutingStopDto> stops) {
        this.stops = stops;
    }

    public List<RouteCoordinateDto> getRouteCoordinates() {
        return routeCoordinates;
    }

    public void setRouteCoordinates(List<RouteCoordinateDto> routeCoordinates) {
        this.routeCoordinates = routeCoordinates;
    }
}