package com.example.demo.service;

import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.entity.Alert;
import com.example.demo.repository.AlertRepository;
import org.springframework.stereotype.Service;

@Service
public class AlertRuleService {

    private final AlertRepository alertRepo;

    public AlertRuleService(AlertRepository alertRepo) {
        this.alertRepo = alertRepo;
    }

    public void evaluateAndCreateAlerts(Bin bin, BinTelemetry t) {
        if (bin == null || t == null) return;

        int fill = (int) t.getFillLevel();
        int batt = (int) t.getBatteryLevel();
        String status = (t.getStatus() == null) ? "OK" : t.getStatus().toUpperCase();

        // 1) Bac plein critique
        if (fill >= 95) {
            createIfNotExists(bin, t,
                    "THRESHOLD", "HIGH",
                    "Bac plein critique",
                    "Le bac " + bin.getBinCode() + " est à " + fill + "%.");
        }

        // 2) Batterie faible
        if (batt > 0 && batt <= 15) {
            createIfNotExists(bin, t,
                    "MAINTENANCE", "MEDIUM",
                    "Batterie faible",
                    "Batterie du bac " + bin.getBinCode() + " = " + batt + "%.");
        }

        // 3) Erreur / Overflow
        if (status.equals("ERROR") || status.equals("OVERFLOW")) {
            createIfNotExists(bin, t,
                    "SYSTEM", "HIGH",
                    "Erreur capteur / Overflow",
                    "Statut = " + status + " pour le bac " + bin.getBinCode() + ".");
        }
    }

    private void createIfNotExists(Bin bin, BinTelemetry t,
                                   String alertType, String severity,
                                   String title, String message) {

        boolean exists = alertRepo.existsByBinIdAndResolvedFalseAndAlertTypeAndSeverity(
                bin.getId(), alertType, severity
        );

        if (exists) return;

        Alert a = new Alert();
        a.setBin(bin);
        a.setTelemetry(t);
        a.setAlertType(alertType);
        a.setSeverity(severity);
        a.setTitle(title);
        a.setMessage(message);
        a.setResolved(false);

        alertRepo.save(a);
    }
}