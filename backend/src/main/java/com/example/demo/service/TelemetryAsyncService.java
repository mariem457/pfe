package com.example.demo.service;

import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TelemetryAsyncService {

    private final AnomalyDetectionService anomalyDetectionService;
    private final AlertRuleService alertRuleService;
    private final MunicipalExceptionAlertService municipalExceptionAlertService;
    private final UrgentBinService urgentBinService;

    public TelemetryAsyncService(
            AnomalyDetectionService anomalyDetectionService,
            AlertRuleService alertRuleService,
            MunicipalExceptionAlertService municipalExceptionAlertService,
            UrgentBinService urgentBinService
    ) {
        this.anomalyDetectionService = anomalyDetectionService;
        this.alertRuleService = alertRuleService;
        this.municipalExceptionAlertService = municipalExceptionAlertService;
        this.urgentBinService = urgentBinService;
    }

    @Async
    public void processAfterTelemetry(Bin bin, BinTelemetry saved, double fillRate) {
        /*
         * AI predictions are saved by predict_one_from_db.py after the telemetry
         * transaction commits. This keeps bin_predictions and bin_time_predictions
         * clean and avoids duplicate rows from deprecated model calls.
         */

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
            if (saved.getFillLevel() >= 95 && !Boolean.TRUE.equals(saved.getCollected())) {
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
