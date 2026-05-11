package com.example.demo.service;

import com.example.demo.entity.Alert;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AlertRuleService {

    private static final int FULL_THRESHOLD = 95;
    private static final int ALMOST_FULL_THRESHOLD = 90;
    private static final int BATTERY_LOW_THRESHOLD = 15;

    private static final int SUDDEN_FILL_DELTA_PERCENT = 35;
    private static final int SUDDEN_FILL_MAX_MINUTES = 90;

    private static final int MIN_SECONDS_FOR_FAST_RATE = 120;
    private static final double FAST_FILL_RATE_PERCENT_PER_HOUR = 12.0;
    private static final int FAST_FILL_MIN_CURRENT_FILL = 55;

    private static final int SENSOR_STUCK_HOURS = 24;
    private static final int SENSOR_STUCK_MIN_POINTS = 4;
    private static final int SENSOR_STUCK_MAX_DELTA = 2;

    private final AlertRepository alertRepo;
    private final BinTelemetryRepository telemetryRepository;
    private final AlertRealtimeService alertRealtimeService;
    private final AlertService alertService;
    private final WeatherService weatherService;

    public AlertRuleService(AlertRepository alertRepo,
                            BinTelemetryRepository telemetryRepository,
                            AlertRealtimeService alertRealtimeService,
                            AlertService alertService,
                            WeatherService weatherService) {
        this.alertRepo = alertRepo;
        this.telemetryRepository = telemetryRepository;
        this.alertRealtimeService = alertRealtimeService;
        this.alertService = alertService;
        this.weatherService = weatherService;
    }

    public void evaluateAndCreateAlerts(Bin bin, BinTelemetry current) {
        if (bin == null || current == null) return;

        int fill = (int) current.getFillLevel();
        int batt = current.getBatteryLevel() != null ? (int) current.getBatteryLevel() : 0;
        String status = current.getStatus() == null ? "OK" : current.getStatus().trim().toUpperCase();

        BinTelemetry previous = telemetryRepository
                .findTopByBinIdAndIdNotOrderByTimestampDesc(bin.getId(), current.getId())
                .orElse(null);

        checkBasicBinAlerts(bin, current, fill, batt, status);

        boolean sudden = checkSuddenFill(bin, current, previous);

        if (!sudden) {
            double fillRate = calculateFillRatePercentPerHour(previous, current);
            checkFastFilling(bin, current, fillRate);
            checkNeedExtraBinNearby(bin, current, fillRate);
        }

        checkSensorStuck(bin, current);
    }

    private void checkBasicBinAlerts(Bin bin,
                                     BinTelemetry current,
                                     int fill,
                                     int batt,
                                     String status) {

        if (fill >= FULL_THRESHOLD) {
            createIfNotExists(bin, current,
                    "BIN_FULL", "HIGH",
                    "Bac plein critique",
                    "Le bac " + bin.getBinCode() + " est à " + fill + "%.",
                    "Injecter ce bac dans une mission active ou créer une mission urgente.",
                    "REPLAN");
        } else if (fill >= ALMOST_FULL_THRESHOLD) {
            createIfNotExists(bin, current,
                    "BIN_ALMOST_FULL", "MEDIUM",
                    "Bac presque plein",
                    "Le bac " + bin.getBinCode() + " est à " + fill + "%.",
                    "Planifier la collecte prochainement.",
                    "PLAN_COLLECTION");
        }

        if (batt > 0 && batt <= BATTERY_LOW_THRESHOLD) {
            createIfNotExists(bin, current,
                    "BIN_BATTERY_LOW", "MEDIUM",
                    "Batterie faible",
                    "Batterie du bac " + bin.getBinCode() + " = " + batt + "%.",
                    "Planifier une intervention maintenance capteur.",
                    "INSPECT");

            if (weatherService != null && weatherService.hasInsufficientSunlight(bin.getLat(), bin.getLng())) {
                createIfNotExists(bin, current,
                        "BATTERY_SOLAR_LOW", "HIGH",
                        "Batterie faible sans soleil",
                        "La batterie du bac " + bin.getBinCode() + " est faible (" + batt
                                + "%) et les conditions météo ne permettent pas une recharge solaire correcte.",
                        "Planifier une intervention maintenance et vérifier le panneau solaire ou la batterie.",
                        "INSPECT");
            }
        }

        if ("ERROR".equals(status) || "OVERFLOW".equals(status)) {
            createIfNotExists(bin, current,
                    "BIN_SENSOR_OR_OVERFLOW", "HIGH",
                    "Erreur capteur / Overflow",
                    "Statut = " + status + " pour le bac " + bin.getBinCode() + ".",
                    "Vérifier le capteur et la situation terrain.",
                    "INSPECT");
        }
    }

    private boolean checkSuddenFill(Bin bin, BinTelemetry current, BinTelemetry previous) {
        if (previous == null || previous.getTimestamp() == null || current.getTimestamp() == null) {
            return false;
        }

        long minutes = Duration.between(previous.getTimestamp(), current.getTimestamp()).toMinutes();
        int delta = current.getFillLevel() - previous.getFillLevel();

        if (minutes < 0 || minutes > SUDDEN_FILL_MAX_MINUTES) return false;

        if (delta >= SUDDEN_FILL_DELTA_PERCENT) {
            createIfNotExists(bin, current,
                    "BIN_SUDDEN_FILL", "HIGH",
                    "Remplissage soudain détecté",
                    "Le bac " + bin.getBinCode() + " est passé de "
                            + previous.getFillLevel() + "% à "
                            + current.getFillLevel() + "% en "
                            + Math.max(minutes, 1) + " minutes.",
                    "Vérifier un dépôt massif, un événement local ou une anomalie autour du bac.",
                    "INSPECT");

            return true;
        }

        return false;
    }

    private void checkFastFilling(Bin bin, BinTelemetry current, double fillRate) {
        if (fillRate < FAST_FILL_RATE_PERCENT_PER_HOUR) return;
        if (current.getFillLevel() < FAST_FILL_MIN_CURRENT_FILL) return;

        createIfNotExists(bin, current,
                "BIN_FAST_FILLING", "MEDIUM",
                "Bac qui se remplit rapidement",
                "Le bac " + bin.getBinCode() + " se remplit à environ "
                        + round(fillRate) + "% par heure.",
                "Surveiller ce bac et envisager une collecte plus fréquente.",
                "PLAN_COLLECTION");
    }

    private void checkSensorStuck(Bin bin, BinTelemetry current) {
        Instant since = Instant.now().minus(Duration.ofHours(SENSOR_STUCK_HOURS));
        List<BinTelemetry> recent = telemetryRepository.findRecentByBinId(bin.getId(), since);

        if (recent == null || recent.size() < SENSOR_STUCK_MIN_POINTS) return;

        int min = recent.stream().mapToInt(BinTelemetry::getFillLevel).min().orElse(current.getFillLevel());
        int max = recent.stream().mapToInt(BinTelemetry::getFillLevel).max().orElse(current.getFillLevel());

        boolean batteryOk = current.getBatteryLevel() == null || current.getBatteryLevel() > BATTERY_LOW_THRESHOLD;

        if ((max - min) <= SENSOR_STUCK_MAX_DELTA && batteryOk) {
            createIfNotExists(bin, current,
                    "BIN_SENSOR_STUCK", "MEDIUM",
                    "Capteur possiblement bloqué",
                    "Le bac " + bin.getBinCode() + " garde presque le même niveau depuis "
                            + SENSOR_STUCK_HOURS + " heures.",
                    "Planifier une inspection du capteur ou vérifier si le bac n'est pas utilisé.",
                    "INSPECT");
        }
    }

    private void checkNeedExtraBinNearby(Bin bin, BinTelemetry current, double fillRate) {
        if (current.getFillLevel() < 85) return;
        if (fillRate < FAST_FILL_RATE_PERCENT_PER_HOUR) return;

        List<BinTelemetry> last10 = telemetryRepository.findAllByBinIdNewestFirst(
                bin.getId(),
                PageRequest.of(0, 10)
        );

        long highCount = last10.stream()
                .filter(t -> t.getFillLevel() >= 85)
                .count();

        if (highCount >= 4) {
            createIfNotExists(bin, current,
                    "NEED_EXTRA_BIN_NEARBY", "HIGH",
                    "Besoin probable d'un bac supplémentaire",
                    "Le bac " + bin.getBinCode()
                            + " atteint souvent un niveau élevé et se remplit rapidement.",
                    "Ajouter un bac proche ou augmenter la fréquence de collecte dans cette zone.",
                    "ADD_BIN");
        }
    }

    private double calculateFillRatePercentPerHour(BinTelemetry previous, BinTelemetry current) {
        if (previous == null || previous.getTimestamp() == null || current.getTimestamp() == null) {
            return 0.0;
        }

        long seconds = current.getTimestamp().getEpochSecond() - previous.getTimestamp().getEpochSecond();

        if (seconds < MIN_SECONDS_FOR_FAST_RATE) return 0.0;

        double hours = seconds / 3600.0;
        return (current.getFillLevel() - previous.getFillLevel()) / hours;
    }

    private void createIfNotExists(Bin bin,
                                   BinTelemetry telemetry,
                                   String alertType,
                                   String severity,
                                   String title,
                                   String message,
                                   String recommendation,
                                   String actionType) {

        boolean exists = alertRepo.existsByBinIdAndResolvedFalseAndAlertTypeAndSeverity(
                bin.getId(),
                alertType,
                severity
        );

        if (exists) return;

        Alert alert = new Alert();
        alert.setBin(bin);
        alert.setTelemetry(telemetry);
        alert.setEntityType("BIN");
        alert.setEntityId(bin.getId());
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setRecommendation(recommendation);
        alert.setActionType(actionType);
        alert.setResolved(false);

        Alert saved = alertRepo.save(alert);
        alertRealtimeService.publishCreated(alertService.toResponse(saved));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}