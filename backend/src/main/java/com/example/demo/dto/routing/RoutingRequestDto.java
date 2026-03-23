package com.example.demo.dto.routing;

import java.util.List;

public class RoutingRequestDto {

    private RoutingDepotDto depot;
    private String trafficMode;
    private List<RoutingBinDto> bins;
    private List<RoutingTruckDto> trucks;
    private List<RoutingIncidentDto> activeIncidents;

    public RoutingRequestDto() {
    }

    public RoutingDepotDto getDepot() {
        return depot;
    }

    public void setDepot(RoutingDepotDto depot) {
        this.depot = depot;
    }

    public String getTrafficMode() {
        return trafficMode;
    }

    public void setTrafficMode(String trafficMode) {
        this.trafficMode = trafficMode;
    }

    public List<RoutingBinDto> getBins() {
        return bins;
    }

    public void setBins(List<RoutingBinDto> bins) {
        this.bins = bins;
    }

    public List<RoutingTruckDto> getTrucks() {
        return trucks;
    }

    public void setTrucks(List<RoutingTruckDto> trucks) {
        this.trucks = trucks;
    }

    public List<RoutingIncidentDto> getActiveIncidents() {
        return activeIncidents;
    }

    public void setActiveIncidents(List<RoutingIncidentDto> activeIncidents) {
        this.activeIncidents = activeIncidents;
    }
}