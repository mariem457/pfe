package com.example.demo.service;

import com.example.demo.dto.routing.MandatoryBinInsightDto;
import com.example.demo.dto.routing.RecommendedFuelStationDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.Truck;

import java.util.List;

public interface RoutingPayloadBuilderService {

    RoutingRequestDto buildRoutingRequest(List<Truck> trucks);

    RoutingRequestDto buildRoutingRequest(List<Truck> trucks, RoutingDecision decision);

    RoutingRequestDto buildReplanRequest(List<Truck> trucks, List<MissionBin> remainingMissionBins);

    List<RecommendedFuelStationDto> getLastRecommendedFuelStations();
    List<MandatoryBinInsightDto> getMandatoryBinInsights();
   
}