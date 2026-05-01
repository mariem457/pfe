package com.example.demo.dto;

public class DashboardKpiResponse {

    private long totalBins;
    private long fullBins;
    private long activeTrucks;
    private double averageFillLevel;

    public DashboardKpiResponse() {
    }

    public DashboardKpiResponse(long totalBins, long fullBins, long activeTrucks, double averageFillLevel) {
        this.totalBins = totalBins;
        this.fullBins = fullBins;
        this.activeTrucks = activeTrucks;
        this.averageFillLevel = averageFillLevel;
    }

    public long getTotalBins() {
        return totalBins;
    }

    public void setTotalBins(long totalBins) {
        this.totalBins = totalBins;
    }

    public long getFullBins() {
        return fullBins;
    }

    public void setFullBins(long fullBins) {
        this.fullBins = fullBins;
    }

    public long getActiveTrucks() {
        return activeTrucks;
    }

    public void setActiveTrucks(long activeTrucks) {
        this.activeTrucks = activeTrucks;
    }

    public double getAverageFillLevel() {
        return averageFillLevel;
    }

    public void setAverageFillLevel(double averageFillLevel) {
        this.averageFillLevel = averageFillLevel;
    }
}