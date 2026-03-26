package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
            String projectDir = System.getProperty("user.dir");
            String scriptPath = projectDir + File.separator + "src" + File.separator + "ml" + File.separator + "predict_from_db.py";

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python",
                    scriptPath,
                    String.valueOf(hour),
                    String.valueOf(day),
                    String.valueOf(fillLevel),
                    String.valueOf(fillRate),
                    String.valueOf(batteryLevel),
                    String.valueOf(weightKg),
                    String.valueOf(rssi)
            );

            processBuilder.directory(new File(projectDir));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String output = reader.readLine();
            int exitCode = process.waitFor();

            System.out.println("Python script path: " + scriptPath);
            System.out.println("Python output: " + output);
            System.out.println("Python exit code: " + exitCode);

            if (exitCode != 0 || output == null || output.isBlank()) {
                throw new RuntimeException("Python prediction failed. Output = " + output);
            }

            String[] parts = output.split(",");

            if (parts.length < 4) {
                throw new RuntimeException("Unexpected Python output format: " + output);
            }

            double predictedFillNext = Double.parseDouble(parts[0].trim());
            String alertStatus = parts[1].trim();
            double priorityScore = Double.parseDouble(parts[2].trim());
            boolean shouldCollect = Boolean.parseBoolean(parts[3].trim());

            return new PredictionResult(
                    predictedFillNext,
                    alertStatus,
                    priorityScore,
                    shouldCollect
            );

        } catch (Exception e) {
            throw new RuntimeException("Error running Python prediction", e);
        }
    }
}