package com.example.demo.service;

import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

@Service
public class TelemetryAsyncService {

    private final BinTimePredictionService binTimePredictionService;
    private final BinPredictionService binPredictionService;
    private final PythonPredictionService pythonPredictionService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final AlertRuleService alertRuleService;
    private final MunicipalExceptionAlertService municipalExceptionAlertService;
    private final BinTelemetryRepository telemetryRepository;
    private final UrgentBinService urgentBinService;

    public TelemetryAsyncService(
            BinTimePredictionService binTimePredictionService,
            BinPredictionService binPredictionService,
            PythonPredictionService pythonPredictionService,
            AnomalyDetectionService anomalyDetectionService,
            AlertRuleService alertRuleService,
            MunicipalExceptionAlertService municipalExceptionAlertService,
            BinTelemetryRepository telemetryRepository,
            UrgentBinService urgentBinService
    ) {
        this.binTimePredictionService = binTimePredictionService;
        this.binPredictionService = binPredictionService;
        this.pythonPredictionService = pythonPredictionService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.alertRuleService = alertRuleService;
        this.municipalExceptionAlertService = municipalExceptionAlertService;
        this.telemetryRepository = telemetryRepository;
        this.urgentBinService = urgentBinService;
    }

    @Async
    public void processAfterTelemetry(Bin bin, BinTelemetry saved, double fillRate) {
        double hour = saved.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .getHour();

        try {
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
        } catch (Exception e) {
            System.err.println("Model2 prediction failed telemetryId=" + saved.getId() + ": " + e.getMessage());
        }

        try {
            List<BinTelemetry> history = telemetryRepository
                    .findByBinIdOrderByTimestampDesc(bin.getId(), PageRequest.of(0, 3));

            double fillLevelLag1 = history.size() > 1 ? history.get(1).getFillLevel() : saved.getFillLevel();
            double fillLevelLag2 = history.size() > 2 ? history.get(2).getFillLevel() : saved.getFillLevel();

            PredictionResult result = pythonPredictionService.runPrediction(
                    hour,
                    saved.getFillLevel(),
                    fillRate,
                    saved.getBatteryLevel() != null ? saved.getBatteryLevel() : 0,
                    saved.getWeightKg() != null ? saved.getWeightKg().doubleValue() : 0.0,
                    saved.getRssi() != null ? saved.getRssi() : 0,
                    Boolean.TRUE.equals(saved.getCollected()),
                    fillLevelLag1,
                    fillLevelLag2,
                    fillRate,
                    saved.getWeightKg() != null ? saved.getWeightKg().doubleValue() : 0.0,
                    saved.getRssi() != null ? saved.getRssi() : 0
            );

            binPredictionService.save(bin.getId(), saved, result);
        } catch (Exception e) {
            System.err.println("Model1 prediction failed telemetryId=" + saved.getId() + ": " + e.getMessage());
        }

        try {
            anomalyDetectionService.evaluateAndPersist(bin, saved);
        } catch (Exception e) {
            System.err.println("Anomaly detection failed telemetryId=" + saved.getId() + ": " + e.getMessage());
        }

        try {
            alertRuleService.evaluateAndCreateAlerts(bin, saved);
        } catch (Exception e) {
            System.err.println("Alert rules failed telemetryId=" + saved.getId() + ": " + e.getMessage());
        }
        try {
            if  (saved.getFillLevel() >= 95 && !Boolean.TRUE.equals(saved.getCollected())) {
                urgentBinService.handleUrgentBin(bin.getId(), saved.getId());
            }
        } catch (Exception e) {
            System.err.println("Urgent bin replanning failed telemetryId=" + saved.getId() + ": " + e.getMessage());
        }

        try {
            municipalExceptionAlertService.evaluateAfterTelemetry(bin);
        } catch (Exception e) {
            System.err.println("Municipal exception failed telemetryId=" + saved.getId() + ": " + e.getMessage());
        }
    }
}