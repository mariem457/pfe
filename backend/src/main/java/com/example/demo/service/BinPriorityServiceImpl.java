package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinPrediction;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinPredictionRepository;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    private final BinPredictionRepository binPredictionRepository;
    private final PythonPredictionService pythonPredictionService;

    public BinPriorityServiceImpl(
            BinRepository binRepository,
            BinTelemetryRepository binTelemetryRepository,
            BinPredictionRepository binPredictionRepository,
            PythonPredictionService pythonPredictionService
    ) {
        this.binRepository = binRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.binPredictionRepository = binPredictionRepository;
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

    @Override
    @Transactional
    public BinPrediction predictAndSave(
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
    ) {
        PredictionResult result = pythonPredictionService.runPrediction(
                hour,
                fillLevel,
                fillRate,
                batteryLevel,
                weightKg,
                rssi,
                collected,
                fillLevelLag1,
                fillLevelLag2,
                fillRateLag1,
                weightKgLag1,
                rssiLag1
        );

        BinPrediction prediction = new BinPrediction();
        prediction.setBinId(binId);
        prediction.setTelemetry(telemetry);
        prediction.setPredictedFillNext(BigDecimal.valueOf(result.getPredictedFillNext()));
        prediction.setAlertStatus(result.getAlertStatus());
        prediction.setPriorityScore(BigDecimal.valueOf(result.getPriorityScore()));
        prediction.setShouldCollect(result.isShouldCollect());
        prediction.setActualFillNext(null);
        prediction.setErrorValue(null);
        prediction.setCreatedAt(OffsetDateTime.now());

        return binPredictionRepository.save(prediction);
    }

    private RoutingBinDto buildRoutingBin(Bin bin, BinTelemetry telemetry) {
        if (telemetry == null) {
            return null;
        }

        BinTelemetry previousTelemetry = binTelemetryRepository
                .findTopByBinIdAndIdNotOrderByTimestampDesc(bin.getId(), telemetry.getId())
                .orElse(null);

        BinTelemetry secondPreviousTelemetry = previousTelemetry != null
                ? binTelemetryRepository
                        .findTopByBinIdAndIdNotOrderByTimestampDesc(bin.getId(), previousTelemetry.getId())
                        .orElse(null)
                : null;

        double hour = telemetry.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .getHour();

        double fillLevel = telemetry.getFillLevel();
        double fillRate = previousTelemetry != null
                ? Math.max(0.0, telemetry.getFillLevel() - previousTelemetry.getFillLevel())
                : 0.0;

        double batteryLevel = telemetry.getBatteryLevel();
        double weightKg = telemetry.getWeightKg() != null ? telemetry.getWeightKg().doubleValue() : 0.0;
        double rssi = telemetry.getRssi();
        boolean collected = Boolean.TRUE.equals(telemetry.getCollected());

        double fillLevelLag1 = previousTelemetry != null ? previousTelemetry.getFillLevel() : fillLevel;
        double fillLevelLag2 = secondPreviousTelemetry != null ? secondPreviousTelemetry.getFillLevel() : fillLevelLag1;

        double fillRateLag1 = 0.0;
        if (previousTelemetry != null && secondPreviousTelemetry != null) {
            fillRateLag1 = Math.max(
                    0.0,
                    previousTelemetry.getFillLevel() - secondPreviousTelemetry.getFillLevel()
            );
        }

        double weightKgLag1 = previousTelemetry != null && previousTelemetry.getWeightKg() != null
                ? previousTelemetry.getWeightKg().doubleValue()
                : weightKg;

        double rssiLag1 = previousTelemetry != null ? previousTelemetry.getRssi() : rssi;

        PredictionResult predictionResult = pythonPredictionService.runPrediction(
                hour,
                fillLevel,
                fillRate,
                batteryLevel,
                weightKg,
                rssi,
                collected,
                fillLevelLag1,
                fillLevelLag2,
                fillRateLag1,
                weightKgLag1,
                rssiLag1
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
}