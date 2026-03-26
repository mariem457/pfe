package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BinPriorityServiceImpl implements BinPriorityService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    private final PythonPredictionService pythonPredictionService;

    public BinPriorityServiceImpl(BinRepository binRepository,
                                  BinTelemetryRepository binTelemetryRepository,
                                  PythonPredictionService pythonPredictionService) {
        this.binRepository = binRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.pythonPredictionService = pythonPredictionService;
    }

    @Override
    public List<RoutingBinDto> getPriorityBinsForRouting() {
        List<Bin> bins = binRepository.findAll().stream()
                .filter(bin -> Boolean.TRUE.equals(bin.getIsActive()))
                .toList();

        Map<Long, BinTelemetry> latestTelemetryByBinId = binTelemetryRepository.findLatestForAllBins()
                .stream()
                .collect(Collectors.toMap(
                        bt -> bt.getBin().getId(),
                        Function.identity(),
                        (a, b) -> a
                ));

        return bins.stream()
                .map(bin -> buildRoutingBin(bin, latestTelemetryByBinId.get(bin.getId())))
                .filter(dto -> dto != null && dto.getPredictedPriority() != null && dto.getPredictedPriority() > 0)
                .filter(this::shouldCollect)
                .sorted(Comparator.comparing(RoutingBinDto::getPredictedPriority).reversed())
                .toList();
    }

    private RoutingBinDto buildRoutingBin(Bin bin, BinTelemetry telemetry) {
        if (telemetry == null) {
            return null;
        }

        double hour = telemetry.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .getHour();

        double day = telemetry.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .getDayOfWeek()
                .getValue();

        double fillLevel = telemetry.getFillLevel();
        double fillRate = estimateFillRate(bin.getId(), telemetry);
        double batteryLevel = telemetry.getBatteryLevel();
        double weightKg = telemetry.getWeightKg() != null ? telemetry.getWeightKg().doubleValue() : 0.0;
        double rssi = telemetry.getRssi();

        PredictionResult predictionResult = pythonPredictionService.runPrediction(
                hour,
                day,
                fillLevel,
                fillRate,
                batteryLevel,
                weightKg,
                rssi
        );

        Double routingLat = bin.getAccessLat() != null ? bin.getAccessLat() : bin.getLat();
        Double routingLng = bin.getAccessLng() != null ? bin.getAccessLng() : bin.getLng();

        RoutingBinDto dto = new RoutingBinDto();
        dto.setId(bin.getId());
        dto.setLat(routingLat);
        dto.setLng(routingLng);
        dto.setFillLevel(fillLevel);
        dto.setPredictedPriority(predictionResult.getPriorityScore());
        dto.setEstimatedLoadKg(weightKg);

        return dto;
    }

    private boolean shouldCollect(RoutingBinDto dto) {
        return dto.getPredictedPriority() >= 0.80 || dto.getFillLevel() >= 80.0;
    }

    private double estimateFillRate(Long binId, BinTelemetry latestTelemetry) {
        BinTelemetry previousTelemetry = binTelemetryRepository
                .findTopByBinIdAndIdNotOrderByTimestampDesc(binId, latestTelemetry.getId())
                .orElse(null);

        if (previousTelemetry == null) {
            return 0.0;
        }

        double currentFill = latestTelemetry.getFillLevel();
        double previousFill = previousTelemetry.getFillLevel();

        return Math.max(0.0, currentFill - previousFill);
    }
}