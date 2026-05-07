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
        this.telemetryAsyncService=telemetryAsyncService;
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
        telemetry.setSource(source);

        BinTelemetry saved = telemetryRepository.save(telemetry);

        double fillRate = 0.0;

        if (previousOpt.isPresent()) {
            BinTelemetry previous = previousOpt.get();

            long seconds = saved.getTimestamp().getEpochSecond()
                    - previous.getTimestamp().getEpochSecond();

            double hoursDiff = seconds / 3600.0;

            if (seconds >= 120 && hoursDiff > 0) {
                fillRate = (saved.getFillLevel() - previous.getFillLevel()) / hoursDiff;
            }
        }

        telemetryAsyncService.processAfterTelemetry(bin, saved, fillRate);
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