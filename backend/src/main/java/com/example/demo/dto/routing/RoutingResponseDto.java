package com.example.demo.dto.routing;

import java.util.List;

public class RoutingResponseDto {

    private List<RoutingMissionDto> missions;

    public RoutingResponseDto() {
    }

    public List<RoutingMissionDto> getMissions() {
        return missions;
    }

    public void setMissions(List<RoutingMissionDto> missions) {
        this.missions = missions;
    }
}