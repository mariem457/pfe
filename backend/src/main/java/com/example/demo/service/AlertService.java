package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepo;
    private final BinRepository binRepo;
    private final BinTelemetryRepository telemetryRepo;
    private final UserRepository userRepo;
    private final TruckRepository truckRepo;
    private final MissionRepository missionRepo;
    private final TruckIncidentRepository incidentRepo;
    private final AlertRealtimeService alertRealtimeService;

    public AlertService(AlertRepository alertRepo,
                        BinRepository binRepo,
                        BinTelemetryRepository telemetryRepo,
                        UserRepository userRepo,
                        TruckRepository truckRepo,
                        MissionRepository missionRepo,
                        TruckIncidentRepository incidentRepo,
                        AlertRealtimeService alertRealtimeService) {
        this.alertRepo = alertRepo;
        this.binRepo = binRepo;
        this.telemetryRepo = telemetryRepo;
        this.userRepo = userRepo;
        this.truckRepo = truckRepo;
        this.missionRepo = missionRepo;
        this.incidentRepo = incidentRepo;
        this.alertRealtimeService = alertRealtimeService;
    }

    public AlertResponse create(AlertCreateRequest req) {
        Alert alert = new Alert();

        if (req.getBinId() != null) {
            Bin bin = binRepo.findById(req.getBinId())
                    .orElseThrow(() -> new RuntimeException("Bin not found: " + req.getBinId()));
            alert.setBin(bin);
            alert.setEntityType("BIN");
            alert.setEntityId(bin.getId());
        }

        if (req.getTelemetryId() != null) {
            BinTelemetry telemetry = telemetryRepo.findById(req.getTelemetryId())
                    .orElseThrow(() -> new RuntimeException("Telemetry not found: " + req.getTelemetryId()));
            alert.setTelemetry(telemetry);
        }

        if (req.getTruckId() != null) {
            Truck truck = truckRepo.findById(req.getTruckId())
                    .orElseThrow(() -> new RuntimeException("Truck not found: " + req.getTruckId()));
            alert.setTruck(truck);
            alert.setEntityType("TRUCK");
            alert.setEntityId(truck.getId());
        }

        if (req.getMissionId() != null) {
            Mission mission = missionRepo.findById(req.getMissionId())
                    .orElseThrow(() -> new RuntimeException("Mission not found: " + req.getMissionId()));
            alert.setMission(mission);
            alert.setEntityType("MISSION");
            alert.setEntityId(mission.getId());
        }

        if (req.getIncidentId() != null) {
            TruckIncident incident = incidentRepo.findById(req.getIncidentId())
                    .orElseThrow(() -> new RuntimeException("Incident not found: " + req.getIncidentId()));
            alert.setIncident(incident);
            alert.setEntityType("INCIDENT");
            alert.setEntityId(incident.getId());

            if (incident.getTruck() != null) alert.setTruck(incident.getTruck());
            if (incident.getMission() != null) alert.setMission(incident.getMission());
        }

        if (req.getEntityType() != null && !req.getEntityType().isBlank()) {
            alert.setEntityType(req.getEntityType().trim().toUpperCase());
        }

        if (req.getEntityId() != null) alert.setEntityId(req.getEntityId());

        alert.setAlertType(req.getAlertType());
        alert.setSeverity(req.getSeverity());
        alert.setTitle(req.getTitle());
        alert.setMessage(req.getMessage());
        alert.setRecommendation(req.getRecommendation());
        alert.setActionType(req.getActionType());
        alert.setResolved(false);

        Alert saved = alertRepo.save(alert);
        AlertResponse response = toResponse(saved);
        alertRealtimeService.publishCreated(response);
        return response;
    }

    public List<AlertResponse> getOpenAlerts() {
        return alertRepo.findOpenAlertsWithRelations().stream().map(this::toResponse).toList();
    }

    public List<AlertResponse> getAlertsByBin(Long binId, boolean onlyOpen) {
        List<Alert> list = onlyOpen
                ? alertRepo.findByBinIdAndResolvedFalseWithRelations(binId)
                : alertRepo.findByBinIdWithRelations(binId);

        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AlertResponse resolve(Long alertId, String username) {
        Alert alert = alertRepo.findByIdWithRelations(alertId);

        if (alert == null) {
            throw new RuntimeException("Alert not found: " + alertId);
        }

        if (alert.isResolved()) {
            return toResponse(alert);
        }

        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());

        if (username != null && !username.isBlank()) {
            userRepo.findByUsername(username).ifPresent(alert::setResolvedBy);
        }

        alertRepo.save(alert);

        try {
            alertRealtimeService.publishResolved(toResponse(alert));
        } catch (Exception e) {
            System.out.println("Realtime failed (ignore): " + e.getMessage());
        }

        return toResponse(alert);
    }

    public AlertDetailsResponse getAlertDetails(Long alertId) {
        Alert a = alertRepo.findByIdWithRelations(alertId);
        if (a == null) throw new RuntimeException("Alert not found: " + alertId);

        AlertDetailsResponse res = new AlertDetailsResponse();
        fillCommonResponse(res, a);

        if (a.getBin() != null) {
            res.setBinLat(a.getBin().getLat());
            res.setBinLng(a.getBin().getLng());
            try {
                if (a.getBin().getZone() != null) {
                    res.setZoneName(a.getBin().getZone().getShapeName());
                }
            } catch (Exception ignored) {}
        }

        if (a.getTelemetry() != null) {
            res.setTelemetryTimestamp(a.getTelemetry().getTimestamp());
            res.setFillLevel((int) a.getTelemetry().getFillLevel());
            res.setBatteryLevel(a.getTelemetry().getBatteryLevel() != null ? (int) a.getTelemetry().getBatteryLevel() : null);
        }

        if (a.getTruck() != null) {
            res.setTruckLat(a.getTruck().getLastKnownLat());
            res.setTruckLng(a.getTruck().getLastKnownLng());
        }

        return res;
    }

    public List<AlertResponse> search(Boolean resolved, String severity, String alertType, String entityType, String q) {
        String qq = (q == null || q.isBlank()) ? null : q.trim();
        String sev = (severity == null || severity.isBlank()) ? null : severity.trim().toUpperCase();
        String typ = (alertType == null || alertType.isBlank()) ? null : alertType.trim().toUpperCase();
        String ent = (entityType == null || entityType.isBlank()) ? null : entityType.trim().toUpperCase();

        List<Long> ids = alertRepo.searchIdsNative(resolved, sev, typ, ent, qq);
        if (ids == null || ids.isEmpty()) return List.of();

        return alertRepo.findByIdInWithRelations(ids).stream().map(this::toResponse).toList();
    }

    public AlertResponse toResponse(Alert a) {
        AlertResponse res = new AlertResponse();
        fillCommonResponse(res, a);
        return res;
    }

    private void fillCommonResponse(AlertResponse res, Alert a) {
        res.setId(a.getId());

        if (a.getBin() != null) {
            res.setBinId(a.getBin().getId());
            res.setBinCode(a.getBin().getBinCode());
        }

        if (a.getTelemetry() != null) res.setTelemetryId(a.getTelemetry().getId());

        if (a.getTruck() != null) {
            res.setTruckId(a.getTruck().getId());
            res.setTruckCode(a.getTruck().getTruckCode());
        }

        if (a.getMission() != null) res.setMissionId(a.getMission().getId());
        if (a.getIncident() != null) res.setIncidentId(a.getIncident().getId());

        res.setEntityType(a.getEntityType());
        res.setEntityId(a.getEntityId());
        res.setAlertType(a.getAlertType());
        res.setSeverity(a.getSeverity());
        res.setTitle(a.getTitle());
        res.setMessage(a.getMessage());
        res.setRecommendation(a.getRecommendation());
        res.setActionType(a.getActionType());
        res.setCreatedAt(a.getCreatedAt());
        res.setResolved(a.isResolved());
        res.setResolvedAt(a.getResolvedAt());
        res.setResolvedByUserId(a.getResolvedBy() != null ? a.getResolvedBy().getId() : null);
    }

    public List<AlertResponse> getAlertsByMission(Long missionId) {
        return alertRepo.findByMissionIdAndResolvedFalseWithRelations(missionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}