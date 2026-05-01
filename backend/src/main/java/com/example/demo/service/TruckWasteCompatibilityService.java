package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.dto.routing.RoutingTruckDto;

import java.util.List;

public interface TruckWasteCompatibilityService {

    boolean canTruckCollectBin(RoutingTruckDto truck, RoutingBinDto bin);

    boolean hasAtLeastOneCompatibleTruck(List<RoutingTruckDto> trucks, RoutingBinDto bin);

    String explainTruckCompatibility(RoutingTruckDto truck, RoutingBinDto bin);
}