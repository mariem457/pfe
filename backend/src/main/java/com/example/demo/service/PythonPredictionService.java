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

    public void runPredictionForTelemetry(Long telemetryId) {
        if (telemetryId == null) {
            throw new IllegalArgumentException("telemetryId must not be null");
        }

        try {
            String scriptPath = resolveScriptPath("predict_one_from_db.py");
            String pythonExe = resolvePythonPath();

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExe,
                    scriptPath,
                    String.valueOf(telemetryId)
            );

            File workingDir = new File(System.getProperty("user.dir"));
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(true);

            System.out.println("========== XGBOOST ONE TELEMETRY PREDICTION START ==========");
            System.out.println("Telemetry ID = " + telemetryId);
            System.out.println("Working directory = " + workingDir.getAbsolutePath());
            System.out.println("Python executable = " + pythonExe);
            System.out.println("Script path = " + scriptPath);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder fullOutput = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println("[XGBOOST-ONE] " + line);
                fullOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String output = fullOutput.toString().trim();

            System.out.println("EXIT CODE = " + exitCode);
            System.out.println("RAW OUTPUT = " + output);
            System.out.println("========== XGBOOST ONE TELEMETRY PREDICTION END ==========");

            if (exitCode != 0) {
                throw new RuntimeException(
                        "Python XGBoost one-telemetry prediction failed. ExitCode="
                                + exitCode + ", Output=" + output
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Error running XGBoost prediction for telemetryId=" + telemetryId, e);
        }
    }

    public void runPredictionFromDatabase() {
        try {
            String scriptPath = resolveScriptPath("predict_from_db.py");
            String pythonExe = resolvePythonPath();

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExe,
                    scriptPath
            );

            File workingDir = new File(System.getProperty("user.dir"));
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(true);

            System.out.println("========== XGBOOST BATCH PREDICTION START ==========");
            System.out.println("Working directory = " + workingDir.getAbsolutePath());
            System.out.println("Python executable = " + pythonExe);
            System.out.println("Script path = " + scriptPath);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder fullOutput = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println("[XGBOOST-BATCH] " + line);
                fullOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String output = fullOutput.toString().trim();

            System.out.println("EXIT CODE = " + exitCode);
            System.out.println("RAW OUTPUT = " + output);
            System.out.println("========== XGBOOST BATCH PREDICTION END ==========");

            if (exitCode != 0) {
                throw new RuntimeException(
                        "Python XGBoost batch prediction failed. ExitCode=" + exitCode + ", Output=" + output
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Error running XGBoost batch prediction", e);
        }
    }

    /*
     * Kept only so old code does not break compilation.
     * New automatic workflow should use runPredictionForTelemetry(saved.getId()).
     */
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
        return new PredictionResult(
                0.0,
                0.0,
                "DEPRECATED",
                0.0,
                false
        );
    }

    private String resolvePythonPath() {
        Path root = Paths.get(System.getProperty("user.dir"));

        List<Path> candidates = List.of(
                root.resolve("src/ml/ml-venv/Scripts/python.exe"),
                root.resolve("ml/ml-venv/Scripts/python.exe"),
                root.resolve(".venv/Scripts/python.exe"),
                root.resolve("venv/Scripts/python.exe")
        );

        return candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .orElse("python");
    }

    private String resolveScriptPath(String fileName) {
        Path root = Paths.get(System.getProperty("user.dir"));

        List<Path> candidates = List.of(
                root.resolve("src/ml").resolve(fileName),
                root.resolve("ml").resolve(fileName),
                root.resolve("src/main/resources/ml").resolve(fileName)
        );

        return candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Python script not found: " + fileName))
                .toAbsolutePath()
                .toString();
    }
}