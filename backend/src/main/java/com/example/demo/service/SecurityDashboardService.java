package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.ApiKeyEntity;
import com.example.demo.entity.SecurityEvent;
import com.example.demo.entity.SecuritySettings;
import com.example.demo.entity.User;
import com.example.demo.repository.ApiKeyRepository;
import com.example.demo.repository.SecurityEventRepository;
import com.example.demo.repository.SecuritySettingsRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SecurityDashboardService {

    private final SecurityEventRepository securityEventRepository;
    private final SecuritySettingsRepository securitySettingsRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public SecurityDashboardService(
            SecurityEventRepository securityEventRepository,
            SecuritySettingsRepository securitySettingsRepository,
            ApiKeyRepository apiKeyRepository,
            UserRepository userRepository
    ) {
        this.securityEventRepository = securityEventRepository;
        this.securitySettingsRepository = securitySettingsRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    public SecurityDashboardResponse getDashboard() {
        OffsetDateTime since24h = OffsetDateTime.now().minusHours(24);

        long sessionsActives = userRepository.findAll().stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(since24h))
                .count();

        long failedLogins = securityEventRepository.countByEventTypeAndEventTimeAfter("LOGIN_FAILED", since24h);
        long apiAccess = securityEventRepository.countByEventTypeAndEventTimeAfter("API_ACCESS", since24h);

        long totalUsers = userRepository.count();
        long twoFactorUsers = getSettingsEntity().getTwoFactorEnabled() ? totalUsers : 0;

        SecurityDashboardResponse response = new SecurityDashboardResponse();
        response.sessionsActives = sessionsActives;
        response.tentativesEchouees24h = failedLogins;
        response.accesApi24h = apiAccess;
        response.authentificationDeuxFacteurs = totalUsers == 0 ? 0 : Math.round((twoFactorUsers * 100.0f) / totalUsers);

        SecurityEvent latestFailed = securityEventRepository
                .findTopByEventTypeAndEventTimeAfterOrderByEventTimeDesc("LOGIN_FAILED", since24h);

        if (latestFailed != null) {
            response.alerteMessage =
                    failedLogins + " tentatives de connexion échouées détectées depuis l'adresse IP "
                            + safe(latestFailed.getIpAddress())
                            + " au cours des dernières 24 heures.";
        } else {
            response.alerteMessage = "Aucune alerte de sécurité critique détectée au cours des dernières 24 heures.";
        }

        return response;
    }

    public List<SecurityEventResponse> getEvents() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return securityEventRepository.findTop20ByOrderByEventTimeDesc().stream().map(event -> {
            SecurityEventResponse dto = new SecurityEventResponse();
            dto.type = mapEventType(event.getEventType());
            dto.titre = event.getTitle();
            dto.statut = "SUCCESS".equalsIgnoreCase(event.getStatus()) ? "Succès" : "Échec";
            dto.utilisateur = safe(event.getUsername());
            dto.appareil = safe(event.getDevice());
            dto.adresseIp = safe(event.getIpAddress());
            dto.localisation = safe(event.getLocation());
            dto.dateHeure = event.getEventTime() != null ? event.getEventTime().format(formatter) : "--";
            return dto;
        }).toList();
    }

    public SecuritySettingsResponse getSettings() {
        SecuritySettings settings = getSettingsEntity();

        SecuritySettingsResponse dto = new SecuritySettingsResponse();
        dto.doubleAuthentification = settings.getTwoFactorEnabled();
        dto.notificationsConnexion = settings.getLoginNotifications();
        dto.limitationApi = settings.getApiRateLimiting();
        dto.detectionActiviteSuspecte = settings.getSuspiciousActivityDetection();
        return dto;
    }

    @Transactional
    public SecuritySettingsResponse updateSettings(UpdateSecuritySettingsRequest req) {
        SecuritySettings settings = getSettingsEntity();

        if (req.doubleAuthentification != null) settings.setTwoFactorEnabled(req.doubleAuthentification);
        if (req.notificationsConnexion != null) settings.setLoginNotifications(req.notificationsConnexion);
        if (req.limitationApi != null) settings.setApiRateLimiting(req.limitationApi);
        if (req.detectionActiviteSuspecte != null) settings.setSuspiciousActivityDetection(req.detectionActiviteSuspecte);

        securitySettingsRepository.save(settings);
        return getSettings();
    }

    public List<ApiKeyResponse> getApiKeys() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toApiKeyResponse).toList();
    }

    @Transactional
    public ApiKeyResponse generateNewKey(boolean testKey) {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setName(testKey ? "Clé API de test générée" : "Clé API générée");
        entity.setKeyValue(generateApiKey(testKey));
        entity.setIsTest(testKey);
        entity.setIsActive(true);

        entity = apiKeyRepository.save(entity);
        return toApiKeyResponse(entity);
    }

    @Transactional
    public void logEvent(
            String eventType,
            String title,
            String status,
            String username,
            String device,
            String ipAddress,
            String location
    ) {
        SecurityEvent event = new SecurityEvent();
        event.setEventType(eventType);
        event.setTitle(title);
        event.setStatus(status);
        event.setUsername(username);
        event.setDevice(device);
        event.setIpAddress(ipAddress);
        event.setLocation(location);
        event.setEventTime(OffsetDateTime.now());
        securityEventRepository.save(event);
    }

    private SecuritySettings getSettingsEntity() {
        return securitySettingsRepository.findAll().stream().findFirst().orElseGet(() -> {
            SecuritySettings settings = new SecuritySettings();
            settings.setTwoFactorEnabled(true);
            settings.setLoginNotifications(true);
            settings.setApiRateLimiting(true);
            settings.setSuspiciousActivityDetection(true);
            return securitySettingsRepository.save(settings);
        });
    }

    private ApiKeyResponse toApiKeyResponse(ApiKeyEntity entity) {
        ApiKeyResponse dto = new ApiKeyResponse();
        dto.id = entity.getId();
        dto.name = entity.getName();
        dto.keyValue = entity.getKeyValue();
        dto.isTest = entity.getIsTest();
        dto.isActive = entity.getIsActive();
        return dto;
    }

    private String mapEventType(String eventType) {
        return switch (eventType) {
            case "LOGIN_SUCCESS" -> "connexion";
            case "LOGIN_FAILED" -> "echec";
            case "API_ACCESS" -> "acces-api";
            case "PERMISSION_CHANGE" -> "modification";
            case "LOGOUT" -> "deconnexion";
            default -> "connexion";
        };
    }

    private String generateApiKey(boolean testKey) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(testKey ? "sk-test-" : "sk-live-");
        for (int i = 0; i < 20; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }
}