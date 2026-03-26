package com.example.demo.dto.routing;

import java.util.List;

public class RoutingResponseDto {

    private List<RoutingMissionDto> missions;
    private String matrixSource;

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
}