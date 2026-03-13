package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Service
public class PythonPredictionService {

    public PredictionResult runPrediction(
            double hour,
            double day,
            double fillLevel,
            double fillRate,
            double batteryLevel,
            double weightKg,
            double rssi
    ) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python",
                    "ml/predict_from_db.py",
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

            String output = reader.readLine();
            int exitCode = process.waitFor();

            if (exitCode != 0 || output == null || output.isBlank()) {
                throw new RuntimeException("Python prediction failed. Output = " + output);
            }

            String[] parts = output.split(",");
            double predictedFillNext = Double.parseDouble(parts[0].trim());
            String alertStatus = parts[1].trim();

            return new PredictionResult(predictedFillNext, alertStatus);

        } catch (Exception e) {
            throw new RuntimeException("Error running Python prediction", e);
        }
    }
}