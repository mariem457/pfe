package com.example.demo.service;

import com.example.demo.dto.TelemetryResponse;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinPrediction;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinPredictionRepository;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class TelemetryService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository telemetryRepository;
    private final BinPredictionRepository predictionRepository;
    private final PythonPredictionService pythonPredictionService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final AlertRuleService alertRuleService;

    public TelemetryService(BinRepository binRepository,
                            BinTelemetryRepository telemetryRepository,
                            BinPredictionRepository predictionRepository,
                            PythonPredictionService pythonPredictionService,
                            AnomalyDetectionService anomalyDetectionService,
                            AlertRuleService alertRuleService) {
        this.binRepository = binRepository;
        this.telemetryRepository = telemetryRepository;
        this.predictionRepository = predictionRepository;
        this.pythonPredictionService = pythonPredictionService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.alertRuleService = alertRuleService;
    }

    @Transactional
    public TelemetryResponse saveTelemetry(String binCode,
                                           short fillLevel,
                                           Short batteryLevel,
                                           BigDecimal weightKg,
                                           String status,
                                           String source) {

        Bin bin = binRepository.findByBinCode(binCode)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + binCode));

        BinTelemetry telemetry = new BinTelemetry();
        telemetry.setBin(bin);
        telemetry.setTimestamp(Instant.now());
        telemetry.setFillLevel(fillLevel);

        if (batteryLevel != null) {
            telemetry.setBatteryLevel(batteryLevel);
        }

        if (weightKg != null) {
            telemetry.setWeightKg(weightKg);
        }

        telemetry.setStatus(status);
        telemetry.setSource(source);

        BinTelemetry saved = telemetryRepository.save(telemetry);

        anomalyDetectionService.evaluateAndPersist(bin, saved);
        alertRuleService.evaluateAndCreateAlerts(bin, saved);

        TelemetryResponse res = new TelemetryResponse();
        res.id = saved.getId();
        res.binCode = bin.getBinCode();
        res.fillLevel = saved.getFillLevel();
        res.batteryLevel = saved.getBatteryLevel();
        res.status = saved.getStatus();
        res.source = saved.getSource();
        res.timestamp = saved.getTimestamp();

        if (bin.getZone() != null) {
            res.zoneId = bin.getZone().getId();
            res.zoneName = bin.getZone().getShapeName();
        }

        return res;
    }
}