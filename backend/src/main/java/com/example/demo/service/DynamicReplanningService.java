package com.example.demo.service;

import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.ReplanRequestDto;

import java.util.List;

public interface DynamicReplanningService {

    List<MissionResponse> replanMission(Long missionId, ReplanRequestDto request);
}