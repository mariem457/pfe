package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PythonPredictionService {

    public PredictionResult runPrediction(
            double hour,
            double fillLevel,
            double fillRate,
            double batteryLevel,
            double weightKg,
            double rssi,
            boolean collected,
            double fillLevelLag1,
            double fillLevelLag2,
            double fillRateLag1,
            double weightKgLag1,
            double rssiLag1
    ) {
        try {
            String scriptPath = resolveScriptPath("predict_from_db.py");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python",
                    scriptPath,
                    String.valueOf(hour),
                    String.valueOf(fillLevel),
                    String.valueOf(fillRate),
                    String.valueOf(batteryLevel),
                    String.valueOf(weightKg),
                    String.valueOf(rssi),
                    String.valueOf(collected),
                    String.valueOf(fillLevelLag1),
                    String.valueOf(fillLevelLag2),
                    String.valueOf(fillRateLag1),
                    String.valueOf(weightKgLag1),
                    String.valueOf(rssiLag1)
            );

            File workingDir = new File(System.getProperty("user.dir"));
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(true);

            System.out.println("========== PYTHON MODEL 1 START ==========");
            System.out.println("Working directory = " + workingDir.getAbsolutePath());
            System.out.println("Script path = " + scriptPath);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder fullOutput = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON-M1] " + line);
                fullOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String output = fullOutput.toString().trim();

            System.out.println("MODEL 1 EXIT CODE = " + exitCode);
            System.out.println("MODEL 1 RAW OUTPUT = " + output);
            System.out.println("========== PYTHON MODEL 1 END ==========");

            if (exitCode != 0 || output.isBlank()) {
                throw new RuntimeException(
                        "Python model 1 failed. ExitCode=" + exitCode + ", Output=" + output
                );
            }

            String[] lines = output.split("\\R");
            String lastLine = lines[lines.length - 1].trim();
            String[] parts = lastLine.split(",");

            if (parts.length < 4) {
                throw new RuntimeException("Unexpected Python model 1 output: " + lastLine);
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
            throw new RuntimeException("Error running Python model 1", e);
        }
    }

    private String resolveScriptPath(String fileName) {
        Path root = Paths.get(System.getProperty("user.dir"));

        List<Path> candidates = List.of(
                root.resolve("src/ml").resolve(fileName),
                root.resolve("src/main/resources/ml").resolve(fileName),
                root.resolve("ml").resolve(fileName)
        );

        return candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Python script not found: " + fileName))
                .toAbsolutePath()
                .toString();
    }
}