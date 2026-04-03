package com.example.demo.dto.routing;


import java.util.ArrayList;

import java.util.List;

public class RoutingResponseDto {

    private List<RoutingMissionDto> missions;
    private String matrixSource;

    private List<ExcludedTruckDto> excludedTrucks = new ArrayList<>();
    private List<WarningTruckDto> warningTrucks = new ArrayList<>();
    private List<RecommendedFuelStationDto> recommendedFuelStations = new ArrayList<>();


    public RoutingResponseDto() {
    }

    public List<RoutingMissionDto> getMissions() {
        return missions;
    }

    public void setMissions(List<RoutingMissionDto> missions) {
        this.missions = missions;
    }

    public String getMatrixSource() {
        return matrixSource;
    }

    public void setMatrixSource(String matrixSource) {
        this.matrixSource = matrixSource;
    }


    public List<ExcludedTruckDto> getExcludedTrucks() {
        return excludedTrucks;
    }

    public void setExcludedTrucks(List<ExcludedTruckDto> excludedTrucks) {
        this.excludedTrucks = excludedTrucks;
    }

    public List<WarningTruckDto> getWarningTrucks() {
        return warningTrucks;
    }

    public void setWarningTrucks(List<WarningTruckDto> warningTrucks) {
        this.warningTrucks = warningTrucks;
    }

    public List<RecommendedFuelStationDto> getRecommendedFuelStations() {
        return recommendedFuelStations;
    }

    public void setRecommendedFuelStations(List<RecommendedFuelStationDto> recommendedFuelStations) {
        this.recommendedFuelStations = recommendedFuelStations;
    }

}