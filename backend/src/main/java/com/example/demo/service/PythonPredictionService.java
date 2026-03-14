package com.example.demo.service;

import com.example.demo.entity.BinPrediction;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinPredictionRepository;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
public class PythonPredictionService {

    private final BinPredictionRepository binPredictionRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    
    public PythonPredictionService(
            BinPredictionRepository binPredictionRepository,
            BinTelemetryRepository binTelemetryRepository
    ) {
        this.binPredictionRepository = binPredictionRepository;
        this.binTelemetryRepository = binTelemetryRepository;
    }

    public PredictionResult runPredictionForBin(Long binId) {
        BinTelemetry latest = binTelemetryRepository.findTopByBin_IdOrderByTimestampDesc(binId)
                .orElseThrow(() -> new RuntimeException("No telemetry found for bin id = " + binId));

        BinTelemetry previous = binTelemetryRepository
                .findTopByBin_IdAndIdNotOrderByTimestampDesc(binId, latest.getId())
                .orElse(null);

        double hour = latest.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .getHour();

        double day = latest.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .getDayOfWeek()
                .getValue();

        double fillLevel = latest.getFillLevel();

        double fillRate = 0.0;
        if (previous != null) {
            double fillDiff = latest.getFillLevel() - previous.getFillLevel();
            long minutesDiff = Duration.between(previous.getTimestamp(), latest.getTimestamp()).toMinutes();

            if (minutesDiff > 0) {
                double hoursDiff = minutesDiff / 60.0;
                fillRate = fillDiff / hoursDiff;
            }
        }

        double batteryLevel = latest.getBatteryLevel();
        double weightKg = latest.getWeightKg() != null ? latest.getWeightKg().doubleValue() : 0.0;
        double rssi = latest.getRssi();

        return runAndSavePrediction(
                latest.getBin().getId(),
                latest.getId(),
                hour,
                day,
                fillLevel,
                fillRate,
                batteryLevel,
                weightKg,
                rssi
        );
    }

    private PredictionResult runAndSavePrediction(
            Long binId,
            Long telemetryId,
            double hour,
            double day,
            double fillLevel,
            double fillRate,
            double batteryLevel,
            double weightKg,
            double rssi
    ) {
        try {
            System.out.println("Starting Python prediction...");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python",
                    "src/ml/predict_from_db.py",
                    String.valueOf(hour),
                    String.valueOf(day),
                    String.valueOf(fillLevel),
                    String.valueOf(fillRate),
                    String.valueOf(batteryLevel),
                    String.valueOf(weightKg),
                    String.valueOf(rssi)
            );

            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            String output = null;

            while ((line = reader.readLine()) != null) {
                System.out.println("PYTHON OUTPUT: " + line);
                output = line;
            }

            int exitCode = process.waitFor();
            System.out.println("Python process finished with code: " + exitCode);

            if (exitCode != 0 || output == null || output.isBlank()) {
                throw new RuntimeException("Python prediction failed. Output = " + output);
            }

            String[] parts = output.split(",");
            double predictedFillNext = Double.parseDouble(parts[0].trim());
            String alertStatus = parts[1].trim();

            BinPrediction prediction = new BinPrediction();
            prediction.setBinId(binId);
            prediction.setTelemetryId(telemetryId);
            prediction.setPredictedFillNext(BigDecimal.valueOf(predictedFillNext));
            prediction.setAlertStatus(alertStatus);
            prediction.setCreatedAt(OffsetDateTime.now());

            binPredictionRepository.save(prediction);

            return new PredictionResult(predictedFillNext, alertStatus);

        } catch (Exception e) {
            throw new RuntimeException("Error running Python prediction", e);
        }
    }
}