package com.example.demo.config;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;

@Component
public class MqttSimulatorRunner implements ApplicationRunner {

    private Process simulatorProcess;

    // Chemin vers mqtt_sim.py — relatif à la racine du projet
    private static final String SCRIPT_PATH = "src/ml/mqtt_sim.py";

    @Override
    public void run(ApplicationArguments args) {
        try {
            File script = Paths.get(SCRIPT_PATH).toFile();

            if (!script.exists()) {
                System.out.println("[SIM] mqtt_sim.py introuvable : "
                        + script.getAbsolutePath());
                return;
            }

            ProcessBuilder pb = new ProcessBuilder("python", script.getAbsolutePath());
            pb.redirectErrorStream(true);          // stderr → stdout
            pb.inheritIO();                         // affiche dans la console Spring Boot
            pb.directory(script.getParentFile());  // working dir = dossier ml/

            simulatorProcess = pb.start();
            System.out.println("[SIM] mqtt_sim.py démarré (PID "
                    + simulatorProcess.pid() + ")");

        } catch (Exception e) {
            System.err.println("[SIM] Erreur au démarrage : " + e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        if (simulatorProcess != null && simulatorProcess.isAlive()) {
            simulatorProcess.destroy();
            System.out.println("[SIM] mqtt_sim.py arrêté.");
        }
    }
}