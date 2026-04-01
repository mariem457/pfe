package com.example.demo.service;

import com.example.demo.dto.MissionResponse;
import com.example.demo.dto.routing.RoutingResponseDto;

import java.util.List;

public interface RoutingOptimizationService {

    RoutingResponseDto prepareInitialRouting();

    List<MissionResponse> planAndSaveMissions();
}