package com.example.demo.dto;

import java.util.List;

public class TruckDashboardResponse {

    private long activeTrucks;
    private long totalRoutes;
    private int averageProgress;
    private String fuelStatus;
    private List<TruckDashboardItemResponse> trucks;

    public TruckDashboardResponse() {
    }

    public TruckDashboardResponse(long activeTrucks, long totalRoutes, int averageProgress, String fuelStatus,
                                  List<TruckDashboardItemResponse> trucks) {
        this.activeTrucks = activeTrucks;
        this.totalRoutes = totalRoutes;
        this.averageProgress = averageProgress;
        this.fuelStatus = fuelStatus;
        this.trucks = trucks;
    }

    public long getActiveTrucks() { return activeTrucks; }
    public void setActiveTrucks(long activeTrucks) { this.activeTrucks = activeTrucks; }

    public long getTotalRoutes() { return totalRoutes; }
    public void setTotalRoutes(long totalRoutes) { this.totalRoutes = totalRoutes; }

    public int getAverageProgress() { return averageProgress; }
    public void setAverageProgress(int averageProgress) { this.averageProgress = averageProgress; }

    public String getFuelStatus() { return fuelStatus; }
    public void setFuelStatus(String fuelStatus) { this.fuelStatus = fuelStatus; }

    public List<TruckDashboardItemResponse> getTrucks() { return trucks; }
    public void setTrucks(List<TruckDashboardItemResponse> trucks) { this.trucks = trucks; }
}