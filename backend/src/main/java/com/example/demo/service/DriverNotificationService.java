package com.example.demo.service;

import com.example.demo.dto.DriverNotificationResponse;
import com.example.demo.entity.Driver;
import com.example.demo.entity.DriverNotification;
import com.example.demo.entity.Mission;
import com.example.demo.entity.TruckIncident;
import com.example.demo.repository.DriverNotificationRepository;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.TruckIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DriverNotificationService {

    private final DriverNotificationRepository notificationRepository;
    private final DriverRepository driverRepository;
    private final TruckIncidentRepository truckIncidentRepository;
    private final DriverPushNotificationService pushNotificationService;

    public DriverNotificationService(
            DriverNotificationRepository notificationRepository,
            DriverRepository driverRepository,
            TruckIncidentRepository truckIncidentRepository,
            DriverPushNotificationService pushNotificationService
    ) {
        this.notificationRepository = notificationRepository;
        this.driverRepository = driverRepository;
        this.truckIncidentRepository = truckIncidentRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @Transactional(readOnly = true)
    public List<DriverNotificationResponse> getNotificationsByUserId(Long userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        return notificationRepository
                .findTop25ByDriverIdOrderByCreatedAtDesc(driver.getId())
                .stream()
                .map(DriverNotificationResponse::new)
                .toList();
    }

    @Transactional
    public DriverNotification createNotification(
            Long driverId,
            String type,
            String title,
            String message
    ) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        DriverNotification notification = new DriverNotification();
        notification.setDriver(driver);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setStatus("SENT");

        DriverNotification saved = notificationRepository.save(notification);
        pruneOldNotifications(driver.getId());
        pushNotificationService.sendIfImportant(saved);
        return saved;
    }

    @Transactional
    public DriverNotification createMissionNotification(
            Mission mission,
            String type,
            String title,
            String message
    ) {
        if (mission == null || mission.getDriver() == null || mission.getDriver().getId() == null) {
            throw new RuntimeException("Mission driver not found");
        }

        Driver driver = driverRepository.findById(mission.getDriver().getId())
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        DriverNotification notification = new DriverNotification();
        notification.setDriver(driver);
        notification.setMission(mission);
        notification.setTruck(mission.getTruck());
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setStatus("SENT");

        DriverNotification saved = notificationRepository.save(notification);
        pruneOldNotifications(driver.getId());
        pushNotificationService.sendIfImportant(saved);
        return saved;
    }

    @Transactional
    public DriverNotificationResponse contactDriverForIncident(Long incidentId, String customMessage) {
        TruckIncident incident = truckIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        if (incident.getTruck() == null || incident.getTruck().getAssignedDriver() == null) {
            throw new RuntimeException("Aucun chauffeur assigné à ce camion");
        }

        Driver driver = incident.getTruck().getAssignedDriver();

        String title = buildTitle(incident);
        String message = customMessage != null && !customMessage.isBlank()
                ? customMessage
                : buildDefaultMessage(incident);

        DriverNotification notification = new DriverNotification();
        notification.setDriver(driver);
        notification.setTruck(incident.getTruck());
        notification.setMission(incident.getMission());
        notification.setIncident(incident);
        notification.setType("INCIDENT_CONTACT");
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setStatus("SENT");

        DriverNotification saved = notificationRepository.save(notification);
        pruneOldNotifications(driver.getId());
        pushNotificationService.sendIfImportant(saved);

        return new DriverNotificationResponse(saved);
    }

    @Transactional
    public DriverNotificationResponse markAsRead(Long notificationId) {
        DriverNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        notification.setReadAt(OffsetDateTime.now());

        if (!"RESPONDED".equals(notification.getStatus())) {
            notification.setStatus("READ");
        }

        return new DriverNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional
    public DriverNotificationResponse respond(Long notificationId, String response) {
        if (response == null || response.isBlank()) {
            throw new RuntimeException("Response is required");
        }

        if (!List.of("POSITION_CONFIRMED", "PROBLEM_RESOLVED", "NEED_ASSISTANCE").contains(response)) {
            throw new RuntimeException("Invalid response: " + response);
        }

        DriverNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        notification.setReadAt(notification.getReadAt() == null ? OffsetDateTime.now() : notification.getReadAt());
        notification.setStatus("RESPONDED");
        notification.setResponse(response);
        notification.setRespondedAt(OffsetDateTime.now());

        return new DriverNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public DriverNotificationResponse getLatestByIncident(Long incidentId) {
        return notificationRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId)
                .map(DriverNotificationResponse::new)
                .orElse(null);
    }

    private String buildTitle(TruckIncident incident) {
        String truckCode = incident.getTruck() != null ? incident.getTruck().getTruckCode() : "Camion";
        String type = incident.getIncidentType() != null ? incident.getIncidentType().name() : "INCIDENT";
        return truckCode + " - " + type;
    }

    private String buildDefaultMessage(TruckIncident incident) {
        if (incident.getIncidentType() == null) {
            return "Incident détecté. Merci de vérifier la situation.";
        }

        return switch (incident.getIncidentType()) {
            case GPS_LOST ->
                    "Perte GPS détectée. Merci de vérifier votre connexion et confirmer votre position.";
            case FUEL_LOW ->
                    "Carburant faible détecté. Merci de vérifier le niveau et confirmer la situation.";
            case BREAKDOWN ->
                    "Panne camion signalée. Merci de confirmer si vous avez besoin d’assistance.";
            case TRAFFIC_BLOCK, DELAY ->
                    "Retard ou blocage détecté. Merci de confirmer votre situation.";
            default ->
                    "Incident détecté. Merci de confirmer votre situation.";
        };
    }

    private void pruneOldNotifications(Long driverId) {
        if (driverId == null) {
            return;
        }

        List<DriverNotification> notifications =
                notificationRepository.findByDriverIdOrderByCreatedAtDesc(driverId);

        if (notifications.size() <= 25) {
            return;
        }

        notificationRepository.deleteAll(notifications.subList(25, notifications.size()));
    }
}