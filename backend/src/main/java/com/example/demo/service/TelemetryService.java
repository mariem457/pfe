package com.example.demo.service;

import com.example.demo.dto.TelemetryResponse;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
public class TelemetryService {

    private static final Set<String> ALLOWED_SOURCES = Set.of("MQTT_REAL", "MQTT_SIM", "PY_SIM");
    private static final double MAX_MODEL_FILL_RATE_PER_HOUR = 8.0;

    private final BinRepository binRepository;
    private final BinTelemetryRepository telemetryRepository;
    private final BinTimePredictionService binTimePredictionService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final AlertRuleService alertRuleService;
    private final BinPredictionService binPredictionService;
    private final PythonPredictionService pythonPredictionService;
    private final MunicipalExceptionAlertService municipalExceptionAlertService;
    private final TelemetryAsyncService telemetryAsyncService;

    public TelemetryService(
            BinRepository binRepository,
            BinTelemetryRepository telemetryRepository,
            BinTimePredictionService binTimePredictionService,
            AnomalyDetectionService anomalyDetectionService,
            AlertRuleService alertRuleService,
            BinPredictionService binPredictionService,
            PythonPredictionService pythonPredictionService,
            MunicipalExceptionAlertService municipalExceptionAlertService,
            TelemetryAsyncService telemetryAsyncService
    ) {
        this.binRepository = binRepository;
        this.telemetryRepository = telemetryRepository;
        this.binTimePredictionService = binTimePredictionService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.alertRuleService = alertRuleService;
        this.binPredictionService = binPredictionService;
        this.pythonPredictionService = pythonPredictionService;
        this.municipalExceptionAlertService = municipalExceptionAlertService;
        this.telemetryAsyncService = telemetryAsyncService;
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
        Bin bin = binRepository.findByBinCode(binCode)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + binCode));

        Optional<BinTelemetry> previousOpt = telemetryRepository.findTopByBinOrderByTimestampDesc(bin);

        BinTelemetry telemetry = new BinTelemetry();
        telemetry.setBin(bin);
        telemetry.setTimestamp(Instant.now());
        telemetry.setFillLevel(fillLevel);
        telemetry.setBatteryLevel(batteryLevel);
        telemetry.setWeightKg(weightKg);
        telemetry.setRssi(rssi);
        telemetry.setCollected(collected != null ? collected : false);
        telemetry.setStatus(status);
        telemetry.setSource(normalizeSource(source));

        BinTelemetry saved = telemetryRepository.save(telemetry);

        double fillRate = 0.0;

        if (previousOpt.isPresent()) {
            BinTelemetry previous = previousOpt.get();

            long seconds = saved.getTimestamp().getEpochSecond()
                    - previous.getTimestamp().getEpochSecond();

            double hoursDiff = seconds / 3600.0;

            if (seconds >= 120 && hoursDiff > 0) {
                fillRate = normalizeFillRateForModel(
                        saved.getFillLevel() - previous.getFillLevel(),
                        hoursDiff
                );
            }
        }

        telemetryAsyncService.processAfterTelemetry(bin, saved, fillRate);

        final Long savedTelemetryId = saved.getId();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runXGBoostPredictionSafely(savedTelemetryId);
                }
            });
        } else {
            runXGBoostPredictionSafely(savedTelemetryId);
        }

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

    private void runXGBoostPredictionSafely(Long telemetryId) {
        try {
            pythonPredictionService.runPredictionForTelemetry(telemetryId);
        } catch (Exception e) {
            System.err.println(
                    "XGBoost prediction failed for telemetryId=" + telemetryId
                            + " : " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "MQTT_SIM";
        }

        String normalized = source.trim().toUpperCase();
        return ALLOWED_SOURCES.contains(normalized) ? normalized : "MQTT_SIM";
    }

    private double normalizeFillRateForModel(double fillDelta, double hoursDiff) {
        if (fillDelta <= 0 || hoursDiff <= 0) {
            return 0.0;
        }

        double modelHours = Math.max(hoursDiff, 1.0);
        double rate = fillDelta / modelHours;
        return Math.min(rate, MAX_MODEL_FILL_RATE_PER_HOUR);
    }
}