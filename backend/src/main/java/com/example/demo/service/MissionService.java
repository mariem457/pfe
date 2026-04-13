package com.example.demo.service;

import com.example.demo.dto.MissionBinActionRequest;
import com.example.demo.dto.MissionBinResponse;
import com.example.demo.dto.MissionFuelStatusResponse;
import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.MissionRouteResponse;

import java.util.List;

public interface MissionService {

    MissionResponse startMission(Long missionId);

    MissionResponse completeMission(Long missionId);

    MissionResponse collectMissionBin(Long missionId, Long missionBinId, MissionBinActionRequest request);

    List<MissionResponse> getAllMissions();

    MissionResponse getMissionById(Long missionId);

    List<MissionBinResponse> getMissionBins(Long missionId);

    MissionRouteResponse getMissionRoute(Long missionId);

    MissionFuelStatusResponse getMissionFuelStatus(Long missionId);

    MissionFuelStatusResponse insertRefuelStop(Long missionId);
}