package com.example.demo.controller;

import com.example.demo.dto.BinResponse;
import com.example.demo.service.BinService;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/maintenance/dashboard")
@CrossOrigin("*")
public class MaintenanceDashboardController {

    private final BinService binService;

    public MaintenanceDashboardController(BinService binService) {
        this.binService = binService;
    }

    @GetMapping
    public Map<String, Object> dashboard() {
        List<BinResponse> bins = binService.findAll();

        Map<String, Object> weather = Map.of(
                "city", "Paris 15",
                "icon", "⛅",
                "temperature", 22,
                "wind", 12,
                "humidity", 68,
                "precipitation", 10
        );

        boolean meteoSansSoleil = isMeteoSansSoleil(weather);

        long active = bins.stream()
                .filter(b -> Boolean.TRUE.equals(b.isActive))
                .count();

        long inactive = bins.stream()
                .filter(b -> !Boolean.TRUE.equals(b.isActive))
                .count();

        long noData = bins.stream()
                .filter(this::isNoData)
                .count();

        long batteryNormal = bins.stream()
                .filter(b -> !isNoData(b))
                .filter(b -> b.batteryLevel != null && b.batteryLevel > 60)
                .count();

        long batteryLow = bins.stream()
                .filter(b -> !isNoData(b))
                .filter(b -> b.batteryLevel != null && b.batteryLevel >= 20 && b.batteryLevel <= 60)
                .count();

        long batteryCritical = bins.stream()
                .filter(b -> !isNoData(b))
                .filter(b -> b.batteryLevel != null && b.batteryLevel < 20)
                .count();

        long weatherAlerts = bins.stream()
                .filter(b -> isBatterySolarRisk(b, meteoSansSoleil))
                .count();

        List<Map<String, Object>> alerts = bins.stream()
                .filter(b -> isMaintenanceAlert(b, meteoSansSoleil))
                .sorted(this::comparePriority)
                .limit(5)
                .map(b -> toAlert(b, meteoSansSoleil))
                .toList();

        List<Map<String, Object>> priority = bins.stream()
                .filter(b -> isMaintenanceAlert(b, meteoSansSoleil))
                .sorted(this::comparePriority)
          
                .map(b -> toPriority(b, meteoSansSoleil))
                .toList();

        return Map.of(
                "stats", List.of(
                        Map.of(
                                "icon", "📡",
                                "value", active,
                                "label", "Capteurs actifs",
                                "badge", "+" + active,
                                "badgeClass", "green",
                                "iconClass", "success"
                        ),
                        Map.of(
                                "icon", "🔋",
                                "value", batteryLow,
                                "label", "Batteries faibles",
                                "badge", "+" + batteryLow,
                                "badgeClass", "red",
                                "iconClass", "warning"
                        ),
                        Map.of(
                                "icon", "🪫",
                                "value", batteryCritical,
                                "label", "Batteries critiques",
                                "badge", "+" + batteryCritical,
                                "badgeClass", "red",
                                "iconClass", "danger"
                        ),
                        Map.of(
                                "icon", "☁️",
                                "value", weatherAlerts,
                                "label", "Alertes météo",
                                "badge", "+" + weatherAlerts,
                                "badgeClass", "red",
                                "iconClass", "info"
                        )
                ),

                "todayWeather", weather,

                "batteryStatus", Map.of(
                        "normal", batteryNormal,
                        "low", batteryLow,
                        "critical", batteryCritical,
                        "offline", noData
                ),

                "sensorStatus", Map.of(
                        "active", active,
                        "inactive", inactive,
                        "critical", batteryCritical,
                        "noData", noData
                ),

                "recentAlerts", alerts,
                "priorityItems", priority
        );
    }

    private boolean isMaintenanceAlert(BinResponse b, boolean meteoSansSoleil) {
        return isNoData(b)
                || isBatteryCritical(b)
                || isBatterySolarRisk(b, meteoSansSoleil);
    }

    private boolean isBatteryCritical(BinResponse b) {
        return b.batteryLevel != null && b.batteryLevel < 20;
    }

    private boolean isBatterySolarRisk(BinResponse b, boolean meteoSansSoleil) {
        return !isNoData(b)
                && b.batteryLevel != null
                && b.batteryLevel >= 20
                && b.batteryLevel < 30
                && meteoSansSoleil;
    }

    private boolean isNoData(BinResponse b) {
        if (b.lastTelemetryAt == null) return true;

        return Duration
                .between(b.lastTelemetryAt, OffsetDateTime.now())
                .toHours() > 24;
    }

    private boolean isMeteoSansSoleil(Map<String, Object> weather) {
        int precipitation = (int) weather.get("precipitation");
        String icon = String.valueOf(weather.get("icon"));

        return precipitation >= 40
                || icon.contains("🌧")
                || icon.contains("☁")
                || icon.contains("⛅");
    }

    private int comparePriority(BinResponse a, BinResponse b) {
        return Integer.compare(priorityScore(a), priorityScore(b));
    }

    private int priorityScore(BinResponse b) {
        if (isNoData(b)) return 1;
        if (isBatteryCritical(b)) return 2;
        return 3;
    }

    private Map<String, Object> toAlert(BinResponse b, boolean meteoSansSoleil) {
        int battery = b.batteryLevel == null ? 0 : b.batteryLevel;

        if (isNoData(b)) {
            return Map.of(
                    "title", b.binCode,
                    "type", "Capteur sans données",
                    "location", location(b),
                    "time", "Aucune donnée récente",
                    "rightLabel", "Sans données",
                    "chipClass", "chip-orange",
                    "rightClass", "pill-red"
            );
        }

        if (battery < 20) {
            return Map.of(
                    "title", b.binCode,
                    "type", "Batterie Critique",
                    "location", location(b),
                    "time", timeLabel(b),
                    "rightLabel", battery + "%",
                    "chipClass", "chip-red",
                    "rightClass", "text-red"
            );
        }

        return Map.of(
                "title", b.binCode,
                "type", "Batterie faible + manque de soleil",
                "location", location(b),
                "time", timeLabel(b),
                "rightLabel", battery + "%",
                "chipClass", "chip-yellow",
                "rightClass", "text-red"
        );
    }

    private Map<String, Object> toPriority(BinResponse b, boolean meteoSansSoleil) {
        int battery = b.batteryLevel == null ? 0 : b.batteryLevel;

        String badge;
        String badgeClass;
        String problem;

        if (isNoData(b)) {
            badge = "Critique";
            badgeClass = "status-red";
            problem = "Capteur sans données";
        } else if (battery < 20) {
            badge = "Critique";
            badgeClass = "status-red";
            problem = "Batterie " + battery + "%";
        } else {
            badge = "Météo";
            badgeClass = "status-orange";
            problem = "Batterie faible + manque de soleil";
        }

        return Map.of(
                "title", b.binCode,
                "badge", badge,
                "badgeClass", badgeClass,
                "subtitle", "Capteur batterie",
                "location", location(b),
                "problem", problem
        );
    }

    private String location(BinResponse b) {
        if (b.zoneName != null && !b.zoneName.isBlank()) {
            return b.zoneName;
        }

        return "Paris 15";
    }

    private String timeLabel(BinResponse b) {
        if (b.lastTelemetryAt == null) {
            return "Aucune donnée";
        }

        long minutes = Duration
                .between(b.lastTelemetryAt, OffsetDateTime.now())
                .toMinutes();

        if (minutes < 1) {
            return "À l’instant";
        }

        if (minutes < 60) {
            return "Il y a " + minutes + " min";
        }

        return "Il y a " + (minutes / 60) + " h";
    }
}