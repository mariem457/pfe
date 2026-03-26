package com.example.demo.service;

import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingResponseDto;

public interface PythonRoutingClient {

    RoutingResponseDto optimizeRoutes(RoutingRequestDto request);
}