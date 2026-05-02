package com.example.demo.service;

import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertBackfillService {

    private final BinTelemetryRepository telemetryRepository;
    private final AlertRuleService alertRuleService;

    public AlertBackfillService(BinTelemetryRepository telemetryRepository,
                                AlertRuleService alertRuleService) {
        this.telemetryRepository = telemetryRepository;
        this.alertRuleService = alertRuleService;
    }

    @Transactional
    public int backfillLatestBinAlerts() {
        List<BinTelemetry> latest = telemetryRepository.findLatestForAllBins();

        int processed = 0;

        for (BinTelemetry telemetry : latest) {
            if (telemetry == null || telemetry.getBin() == null) {
                continue;
            }

            alertRuleService.evaluateAndCreateAlerts(telemetry.getBin(), telemetry);
            processed++;
        }

        return processed;
    }
}