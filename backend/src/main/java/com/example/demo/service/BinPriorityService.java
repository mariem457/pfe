package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.entity.BinPrediction;
import com.example.demo.entity.BinTelemetry;

import java.util.List;

public interface BinPriorityService {

    List<RoutingBinDto> getPriorityBinsForRouting();

    BinPrediction predictAndSave(
            Long binId,
            BinTelemetry telemetry,
            double hour,
            double fillLevel,
            double fillRate,
            double batteryLevel,
            double weightKg,
            double rssi,
            boolean collected,
            double fillLevelLag1,
            double fillLevelLag2,
            double fillRateLag1,
            double weightKgLag1,
            double rssiLag1
    );
}