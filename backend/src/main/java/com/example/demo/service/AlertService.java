package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alertRepo;
    private final BinRepository binRepo;
    private final BinTelemetryRepository telemetryRepo;
    private final UserRepository userRepo;

    public AlertService(AlertRepository alertRepo,
                        BinRepository binRepo,
                        BinTelemetryRepository telemetryRepo,
                        UserRepository userRepo) {
        this.alertRepo = alertRepo;
        this.binRepo = binRepo;
        this.telemetryRepo = telemetryRepo;
        this.userRepo = userRepo;
    }

    // ================= CREATE =================
    public AlertResponse create(AlertCreateRequest req) {

        Bin bin = binRepo.findById(req.getBinId())
                .orElseThrow(() -> new RuntimeException("Bin not found: " + req.getBinId()));

        BinTelemetry telemetry = null;
        if (req.getTelemetryId() != null) {
            telemetry = telemetryRepo.findById(req.getTelemetryId())
                    .orElseThrow(() -> new RuntimeException("Telemetry not found: " + req.getTelemetryId()));
        }

        Alert alert = new Alert();
        alert.setBin(bin);
        alert.setTelemetry(telemetry);
        alert.setAlertType(req.getAlertType());
        alert.setSeverity(req.getSeverity());
        alert.setTitle(req.getTitle());
        alert.setMessage(req.getMessage());
        alert.setResolved(false);

        return toResponse(alertRepo.save(alert));
    }

    // ================= OPEN ALERTS =================
    public List<AlertResponse> getOpenAlerts() {
        return alertRepo.findOpenAlertsWithRelations()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ================= ALERTS BY BIN =================
    public List<AlertResponse> getAlertsByBin(Long binId, boolean onlyOpen) {

        List<Alert> list = onlyOpen
                ? alertRepo.findByBinIdAndResolvedFalseWithRelations(binId)
                : alertRepo.findByBinIdWithRelations(binId);

        return list.stream().map(this::toResponse).toList();
    }

    // ================= RESOLVE =================
    

    @Transactional
    public AlertResponse resolve(Long alertId, String username) {

        // ✅ جيب alert مع العلاقات باش نتفادو LazyInitialization
        Alert alert = alertRepo.findByIdWithRelations(alertId);
        if (alert == null) throw new RuntimeException("Alert not found: " + alertId);

        // ✅ prevent double resolve
        if (alert.isResolved()) return toResponse(alert);

        // ✅ جيب user حسب username (مش id)
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());
        alert.setResolvedBy(user);

        // save
        alertRepo.save(alert);

        // ✅ رجّع response من نفس entity اللي فيه relations جاهزين
        return toResponse(alert);
    }

    // ================= DETAILS =================
    public AlertDetailsResponse getAlertDetails(Long alertId) {

        Alert a = alertRepo.findByIdWithRelations(alertId);
        if (a == null) throw new RuntimeException("Alert not found: " + alertId);

        AlertDetailsResponse res = new AlertDetailsResponse();

        // base mapping
        res.setId(a.getId());
        res.setBinId(a.getBin().getId());
        res.setBinCode(a.getBin().getBinCode());
        res.setTelemetryId(a.getTelemetry() != null ? a.getTelemetry().getId() : null);
        res.setAlertType(a.getAlertType());
        res.setSeverity(a.getSeverity());
        res.setTitle(a.getTitle());
        res.setMessage(a.getMessage());
        res.setCreatedAt(a.getCreatedAt());
        res.setResolved(a.isResolved());
        res.setResolvedAt(a.getResolvedAt());
        res.setResolvedByUserId(a.getResolvedBy() != null ? a.getResolvedBy().getId() : null);

        // extra bin info
        res.setBinLat(a.getBin().getLat());
        res.setBinLng(a.getBin().getLng());

        // zoneName (optional)
        try {
            if (a.getBin().getZone() != null) {
                res.setZoneName(a.getBin().getZone().getName());
            }
        } catch (Exception ignored) {}

        // telemetry extra
        if (a.getTelemetry() != null) {
            res.setTelemetryTimestamp(a.getTelemetry().getTimestamp());
            res.setFillLevel((int) a.getTelemetry().getFillLevel());
            res.setBatteryLevel((int) a.getTelemetry().getBatteryLevel());
        }

        return res;
    }

    // ================= SEARCH / FILTERS =================
    public List<AlertResponse> search(Boolean resolved, String severity, String alertType, String q) {

        String qq  = (q == null || q.isBlank()) ? null : q.trim();
        String sev = (severity == null || severity.isBlank()) ? null : severity.trim();
        String typ = (alertType == null || alertType.isBlank()) ? null : alertType.trim();

        // 1) get ids (native ILIKE)
        List<Long> ids = alertRepo.searchIdsNative(resolved, sev, typ, qq);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        // 2) fetch relations by ids (no LazyInit)
        return alertRepo.findByIdInWithRelations(ids)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ================= MAPPING =================
    private AlertResponse toResponse(Alert a) {
        AlertResponse res = new AlertResponse();
        res.setId(a.getId());
        res.setBinId(a.getBin().getId());
        res.setBinCode(a.getBin().getBinCode());
        res.setTelemetryId(a.getTelemetry() != null ? a.getTelemetry().getId() : null);
        res.setAlertType(a.getAlertType());
        res.setSeverity(a.getSeverity());
        res.setTitle(a.getTitle());
        res.setMessage(a.getMessage());
        res.setCreatedAt(a.getCreatedAt());
        res.setResolved(a.isResolved());
        res.setResolvedAt(a.getResolvedAt());
        res.setResolvedByUserId(a.getResolvedBy() != null ? a.getResolvedBy().getId() : null);
        return res;
    }
}