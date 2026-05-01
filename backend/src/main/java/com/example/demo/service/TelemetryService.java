package com.example.demo.service;

import com.example.demo.dto.TelemetryResponse;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class TelemetryService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository telemetryRepository;
    private final BinTimePredictionService binTimePredictionService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final AlertRuleService alertRuleService;
    private final BinPredictionService binPredictionService;
    private final PythonPredictionService pythonPredictionService;

    public TelemetryService(
            BinRepository binRepository,
            BinTelemetryRepository telemetryRepository,
            BinTimePredictionService binTimePredictionService,
            AnomalyDetectionService anomalyDetectionService,
            AlertRuleService alertRuleService,
            BinPredictionService binPredictionService,
            PythonPredictionService pythonPredictionService
    ) {
        this.binRepository = binRepository;
        this.telemetryRepository = telemetryRepository;
        this.binTimePredictionService = binTimePredictionService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.alertRuleService = alertRuleService;
        this.binPredictionService = binPredictionService;
        this.pythonPredictionService = pythonPredictionService;
    }

    @Transactional
    public TelemetryResponse saveTelemetry(
            String binCode,
            short fillLevel,
            Short batteryLevel,
            BigDecimal weightKg,
            String status,
            String source,
            Short rssi,
            Boolean collected
    ) {
        System.out.println("INSIDE TelemetryService.saveTelemetry");

        Bin bin = binRepository.findByBinCode(binCode)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + binCode));

        Optional<BinTelemetry> previousOpt =
                telemetryRepository.findTopByBinOrderByTimestampDesc(bin);

        BinTelemetry telemetry = new BinTelemetry();
        telemetry.setBin(bin);
        telemetry.setTimestamp(Instant.now());
        telemetry.setFillLevel(fillLevel);

        if (batteryLevel != null) telemetry.setBatteryLevel(batteryLevel);
        if (weightKg != null)     telemetry.setWeightKg(weightKg);
        if (rssi != null)         telemetry.setRssi(rssi);

        telemetry.setCollected(collected != null ? collected : false);
        telemetry.setStatus(status);
        telemetry.setSource(source);

        BinTelemetry saved = telemetryRepository.save(telemetry);
        System.out.println("AFTER telemetry save id=" + saved.getId());

        // ── fill rate ────────────────────────────────────────────
        double fillRate = 0.0;
        if (previousOpt.isPresent()) {
            BinTelemetry previous = previousOpt.get();
            long seconds = saved.getTimestamp().getEpochSecond()
                    - previous.getTimestamp().getEpochSecond();
            double hoursDiff = seconds / 3600.0;
            if (hoursDiff > 0) {
                fillRate = (saved.getFillLevel() - previous.getFillLevel()) / hoursDiff;
            }
        }

        double hour = saved.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .getHour();

        // ── MODEL 2 : bin_time_predictions ───────────────────────
        try {
            System.out.println("BEFORE model2 prediction");
            binTimePredictionService.predictAndSave(
                    bin.getId(),
                    saved.getId(),
                    hour,
                    saved.getFillLevel(),
                    fillRate,
                    saved.getBatteryLevel() != null ? saved.getBatteryLevel() : 0,
                    saved.getWeightKg() != null ? saved.getWeightKg().doubleValue() : 0.0,
                    saved.getRssi() != null ? saved.getRssi() : 0,
                    Boolean.TRUE.equals(saved.getCollected())
            );
            System.out.println("AFTER model2 prediction");
        } catch (Exception e) {
            System.err.println("Model2 prediction failed: " + e.getMessage());
        }

        // ── MODEL 1 : bin_predictions ─────────────────────────────
        try {
            System.out.println("BEFORE model1 prediction");

            List<BinTelemetry> history = telemetryRepository
                    .findByBinIdOrderByTimestampDesc(bin.getId(), PageRequest.of(0, 3));

            double fillLevelLag1 = history.size() > 1 ? history.get(1).getFillLevel() : fillLevel;
            double fillLevelLag2 = history.size() > 2 ? history.get(2).getFillLevel() : fillLevel;
            double fillRateLag1  = fillRate;
            double weightKgLag1  = saved.getWeightKg() != null ? saved.getWeightKg().doubleValue() : 0.0;
            double rssiLag1      = saved.getRssi() != null ? saved.getRssi() : 0;

            PredictionResult result = pythonPredictionService.runPrediction(
                    hour,
                    fillLevel,
                    fillRate,
                    saved.getBatteryLevel() != null ? saved.getBatteryLevel() : 0,
                    saved.getWeightKg() != null ? saved.getWeightKg().doubleValue() : 0.0,
                    saved.getRssi() != null ? saved.getRssi() : 0,
                    Boolean.TRUE.equals(saved.getCollected()),
                    fillLevelLag1,
                    fillLevelLag2,
                    fillRateLag1,
                    weightKgLag1,
                    rssiLag1
            );

            binPredictionService.save(bin.getId(), saved, result);
            System.out.println("AFTER model1 prediction");

        } catch (Exception e) {
            System.err.println("Model1 prediction failed: " + e.getMessage());
        }

        // ── anomaly + alerts ──────────────────────────────────────
        anomalyDetectionService.evaluateAndPersist(bin, saved);
        alertRuleService.evaluateAndCreateAlerts(bin, saved);

        // ── response ──────────────────────────────────────────────
        TelemetryResponse res = new TelemetryResponse();
        res.id          = saved.getId();
        res.binCode     = bin.getBinCode();
        res.fillLevel   = saved.getFillLevel();
        res.batteryLevel = saved.getBatteryLevel();
        res.status      = saved.getStatus();
        res.source      = saved.getSource();
        res.timestamp   = saved.getTimestamp();

        if (bin.getZone() != null) {
            res.zoneId   = bin.getZone().getId();
            res.zoneName = bin.getZone().getShapeName();
        }

        return res;
    }
}