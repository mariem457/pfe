package com.example.demo.controller;

import com.example.demo.dto.MissionBinActionRequest;
import com.example.demo.dto.MissionBinResponse;
import com.example.demo.dto.MissionFuelStatusResponse;
import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.MissionRouteResponse;
import com.example.demo.service.MissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping
    public ResponseEntity<List<MissionResponse>> getAllMissions() {
        return ResponseEntity.ok(missionService.getAllMissions());
    }

    @GetMapping("/{missionId}")
    public ResponseEntity<MissionResponse> getMissionById(@PathVariable Long missionId) {
        return ResponseEntity.ok(missionService.getMissionById(missionId));
    }

    @GetMapping("/{missionId}/bins")
    public ResponseEntity<List<MissionBinResponse>> getMissionBins(@PathVariable Long missionId) {
        return ResponseEntity.ok(missionService.getMissionBins(missionId));
    }

    @GetMapping("/{missionId}/route")
    public ResponseEntity<MissionRouteResponse> getMissionRoute(@PathVariable Long missionId) {
        return ResponseEntity.ok(missionService.getMissionRoute(missionId));
    }

    @GetMapping("/{missionId}/fuel-status")
    public ResponseEntity<MissionFuelStatusResponse> getMissionFuelStatus(@PathVariable Long missionId) {
        return ResponseEntity.ok(missionService.getMissionFuelStatus(missionId));
    }

    @PostMapping("/{missionId}/refuel")
    public ResponseEntity<MissionFuelStatusResponse> insertRefuelStop(@PathVariable Long missionId) {
        return ResponseEntity.ok(missionService.insertRefuelStop(missionId));
    }

    @PostMapping("/{missionId}/start")
    public ResponseEntity<MissionResponse> startMission(@PathVariable Long missionId) {
        return ResponseEntity.ok(missionService.startMission(missionId));
    }

    @PostMapping("/{missionId}/complete")
    public ResponseEntity<MissionResponse> completeMission(@PathVariable Long missionId) {
        return ResponseEntity.ok(missionService.completeMission(missionId));
    }

    @PostMapping("/{missionId}/bins/{missionBinId}/collect")
    public ResponseEntity<MissionResponse> collectMissionBin(@PathVariable Long missionId,
                                                             @PathVariable Long missionBinId,
                                                             @RequestBody(required = false) MissionBinActionRequest request) {
        return ResponseEntity.ok(missionService.collectMissionBin(missionId, missionBinId, request));
    }
}