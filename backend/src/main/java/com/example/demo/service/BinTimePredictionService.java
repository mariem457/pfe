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
    private final UrgentBinService urgentBinService;

    public BinTimePredictionService(
            PythonTimePredictionService pythonTimePredictionService,
            BinTimePredictionRepository binTimePredictionRepository,
            UrgentBinService urgentBinService
    ) {
        this.pythonTimePredictionService = pythonTimePredictionService;
        this.binTimePredictionRepository = binTimePredictionRepository;
        this.urgentBinService = urgentBinService;
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
        TimeToThresholdPredictionResult result =
                pythonTimePredictionService.runPrediction(
                        hour, fillLevel, fillRate,
                        batteryLevel, weightKg, rssi, collected
                );

        boolean isUrgent =
                result.getPredictedHours() <= 3 ||
                result.getPriorityScore() >= 0.9;

        BinTimePrediction prediction = new BinTimePrediction();
        prediction.setBinId(binId);
        prediction.setTelemetryId(telemetryId);
        prediction.setPredictedHours(BigDecimal.valueOf(result.getPredictedHours()));
        prediction.setAlertStatus(result.getAlertStatus());
        prediction.setPriorityScore(BigDecimal.valueOf(result.getPriorityScore()));
        prediction.setShouldCollect(result.isShouldCollect());
        prediction.setCreatedAt(OffsetDateTime.now());

        BinTimePrediction saved = binTimePredictionRepository.save(prediction);

        if (isUrgent) {
            try {
                urgentBinService.handleUrgentBin(binId, telemetryId);
            } catch (Exception e) {
                System.err.println(
                        "URGENT REPLAN FAILED but telemetry/prediction will continue => binId="
                                + binId
                                + ", telemetryId="
                                + telemetryId
                                + ", error="
                                + e.getMessage()
                );
            }
        }

        return saved;
    }
}