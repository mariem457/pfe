package com.example.demo.controller;

import com.example.demo.dto.MissionReassignmentResponseDto;
import com.example.demo.service.MissionReassignmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mission-reassignments")
public class MissionReassignmentController {

    private final MissionReassignmentService missionReassignmentService;

    public MissionReassignmentController(MissionReassignmentService missionReassignmentService) {
        this.missionReassignmentService = missionReassignmentService;
    }

    @GetMapping("/mission/{missionId}")
    public List<MissionReassignmentResponseDto> getByMissionId(@PathVariable Long missionId) {
        return missionReassignmentService.getByOriginalMissionId(missionId);
    }
}