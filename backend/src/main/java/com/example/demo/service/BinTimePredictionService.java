// BinTimePredictionService.java
package com.example.demo.service;

import com.example.demo.entity.BinTimePrediction;
import com.example.demo.repository.BinTimePredictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class BinTimePredictionService {

    private final PythonTimePredictionService pythonTimePredictionService;
    private final BinTimePredictionRepository binTimePredictionRepository;

    public BinTimePredictionService(
            PythonTimePredictionService pythonTimePredictionService,
            BinTimePredictionRepository binTimePredictionRepository
    ) {
        this.pythonTimePredictionService = pythonTimePredictionService;
        this.binTimePredictionRepository = binTimePredictionRepository;
    }

    @Transactional
    public BinTimePrediction predictAndSave(
            Long binId,
            Long telemetryId,
            double hour,
            double fillLevel,
            double fillRate,
            double batteryLevel,
            double weightKg,
            double rssi,
            boolean collected
    ) {
        TimeToThresholdPredictionResult result = pythonTimePredictionService.runPrediction(
                hour,
                fillLevel,
                fillRate,
                batteryLevel,
                weightKg,
                rssi,
                collected
        );

        BinTimePrediction prediction = new BinTimePrediction();
        prediction.setBinId(binId);
        prediction.setTelemetryId(telemetryId);
        prediction.setPredictedHours(BigDecimal.valueOf(result.getPredictedHours()));
        prediction.setAlertStatus(result.getAlertStatus());
        prediction.setPriorityScore(BigDecimal.valueOf(result.getPriorityScore()));
        prediction.setShouldCollect(result.isShouldCollect());
        prediction.setCreatedAt(OffsetDateTime.now());

        return binTimePredictionRepository.save(prediction);
    }
}