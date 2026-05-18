package com.example.demo.service;

import com.example.demo.entity.Driver;
import com.example.demo.entity.DriverNotification;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class DriverPushNotificationService {

    private final WebClient webClient;

    public DriverPushNotificationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://exp.host")
                .build();
    }

    public void sendIfImportant(DriverNotification notification) {
        if (notification == null || !isImportant(notification)) {
            return;
        }

        Driver driver = notification.getDriver();
        if (driver == null || driver.getExpoPushToken() == null || driver.getExpoPushToken().isBlank()) {
            return;
        }

        Map<String, Object> body = Map.of(
                "to", driver.getExpoPushToken(),
                "title", buildTitle(notification),
                "body", buildMessage(notification),
                "sound", "default",
                "priority", "high",
                "data", Map.of("notificationId", notification.getId())
        );

        try {
            webClient.post()
                    .uri("/--/api/v2/push/send")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> System.out.println("EXPO PUSH SENT => notificationId=" + notification.getId()),
                            error -> System.out.println("EXPO PUSH FAILED => " + error.getMessage())
                    );
        } catch (Exception e) {
            System.out.println("EXPO PUSH FAILED => " + e.getMessage());
        }
    }

    private boolean isImportant(DriverNotification notification) {
        String type = notification.getType();
        String text = ((notification.getTitle() != null ? notification.getTitle() : "") + " "
                + (notification.getMessage() != null ? notification.getMessage() : "")).toUpperCase();

        return "INCIDENT_CONTACT".equals(type)
                || "TRUCK_BREAKDOWN_HANDLED".equals(type)
                || text.contains("GPS_LOST")
                || text.contains("GPS-LOST");
    }

    private String buildTitle(DriverNotification notification) {
        String truckCode = notification.getTruck() != null && notification.getTruck().getTruckCode() != null
                ? notification.getTruck().getTruckCode().replaceFirst("(?i)^TRUCK", "Camion")
                : "Alerte chauffeur";

        if ("INCIDENT_CONTACT".equals(notification.getType())) {
            return truckCode + " - Besoin de confirmation";
        }

        if ("TRUCK_BREAKDOWN_HANDLED".equals(notification.getType())) {
            return truckCode + " - Panne camion traitée";
        }

        String title = notification.getTitle() != null ? notification.getTitle() : "Alerte chauffeur";
        return normalizeText(title);
    }

    private String buildMessage(DriverNotification notification) {
        String message = notification.getMessage();
        if (message == null || message.isBlank()) {
            return "Une alerte importante concerne votre tournée.";
        }

        return normalizeText(message);
    }

    private String normalizeText(String value) {
        return value
                .replace("GPS_LOST", "perte GPS")
                .replace("GPS-LOST", "perte GPS")
                .replace("BREAKDOWN", "panne")
                .replace("FUEL_LOW", "carburant faible")
                .replace("FUEL-LOW", "carburant faible")
                .replace("_", " ")
                .replace("-", " ");
    }
}
