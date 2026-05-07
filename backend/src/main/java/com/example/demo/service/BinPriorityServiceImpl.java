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
                .filter(dto -> dto != null)
                .sorted(
                        Comparator
                                .comparing(
                                        (RoutingBinDto dto) -> safeDouble(dto.getFillLevel()),
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(
                                        dto -> safeDouble(dto.getPredictedPriority()),
                                        Comparator.reverseOrder()
                                )
                )
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
        if (bin == null || telemetry == null) {
            return null;
        }

        BinPrediction latestPrediction = binPredictionRepository
                .findTopByBinIdOrderByCreatedAtDesc(bin.getId())
                .orElse(null);

        double predictedPriority = 0.0;

        if (latestPrediction != null && latestPrediction.getPriorityScore() != null) {
            predictedPriority = latestPrediction.getPriorityScore().doubleValue();
        }

        Double routingLat = bin.getAccessLat() != null ? bin.getAccessLat() : bin.getLat();
        Double routingLng = bin.getAccessLng() != null ? bin.getAccessLng() : bin.getLng();

        double fillLevel = telemetry.getFillLevel();
        double weightKg = telemetry.getWeightKg() != null
                ? telemetry.getWeightKg().doubleValue()
                : 0.0;

        RoutingBinDto dto = new RoutingBinDto();
        dto.setId(bin.getId());
        dto.setLat(routingLat);
        dto.setLng(routingLng);
        dto.setFillLevel(fillLevel);
        dto.setPredictedPriority(predictedPriority);
        dto.setEstimatedLoadKg(weightKg);
        dto.setWasteType(resolveWasteType(bin));

        return dto;
    }

    private String resolveWasteType(Bin bin) {
        if (bin == null || bin.getWasteType() == null) {
            return "UNKNOWN";
        }
        return bin.getWasteType().name().trim().toUpperCase();
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}