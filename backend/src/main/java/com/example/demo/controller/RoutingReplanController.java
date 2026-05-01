package com.example.demo.controller;

import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.ReplanRequestDto;
import com.example.demo.service.DynamicReplanningService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routing")
public class RoutingReplanController {

    private final DynamicReplanningService dynamicReplanningService;

    public RoutingReplanController(DynamicReplanningService dynamicReplanningService) {
        this.dynamicReplanningService = dynamicReplanningService;
    }

    @PostMapping("/replan/{missionId}")
    public List<MissionResponse> replanMission(@PathVariable Long missionId,
                                               @RequestBody ReplanRequestDto request) {
        return dynamicReplanningService.replanMission(missionId, request);
    }
}