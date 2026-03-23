package com.example.demo.service;

import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.entity.Truck;

import java.util.List;

public interface RoutingPayloadBuilderService {

    RoutingRequestDto buildRoutingRequest(List<Truck> trucks);
}