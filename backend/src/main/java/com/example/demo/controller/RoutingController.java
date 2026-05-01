package com.example.demo.controller;

import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.RoutingResponseDto;
import com.example.demo.service.RoutingOptimizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {

    private final RoutingOptimizationService routingOptimizationService;

    public RoutingController(RoutingOptimizationService routingOptimizationService) {
        this.routingOptimizationService = routingOptimizationService;
    }

    @PostMapping("/prepare")
    public ResponseEntity<RoutingResponseDto> prepareInitialRouting() {
        return ResponseEntity.ok(routingOptimizationService.prepareInitialRouting());
    }

    @PostMapping("/plan-and-save")
    public ResponseEntity<List<MissionResponse>> planAndSave() {
        return ResponseEntity.ok(routingOptimizationService.planAndSaveMissions());
    }
}