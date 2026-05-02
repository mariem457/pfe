package com.example.demo.controller;

import com.example.demo.dto.AlertDetailsResponse;
import com.example.demo.dto.AlertResponse;
import com.example.demo.service.AlertService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // ✅ GET open alerts
    @GetMapping("/open")
    public List<AlertResponse> getOpenAlerts() {
        return alertService.getOpenAlerts();
    }

    // ✅ GET alerts by bin
    @GetMapping("/bins/{binId}")
    public List<AlertResponse> getAlertsByBin(
            @PathVariable Long binId,
            @RequestParam(defaultValue = "false") boolean onlyOpen
    ) {
        return alertService.getAlertsByBin(binId, onlyOpen);
    }

    // ✅ PATCH resolve alert (FIXED)
    @PatchMapping("/{id}/resolve")
    public AlertResponse resolve(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        return alertService.resolve(id, username);
    }
    @GetMapping("/missions/{missionId}")
    public List<AlertResponse> getAlertsByMission(@PathVariable Long missionId) {
        return alertService.getAlertsByMission(missionId);
    }

    // ✅ GET alert details
    @GetMapping("/{id}")
    public AlertDetailsResponse getDetails(@PathVariable Long id) {
        return alertService.getAlertDetails(id);
    }

    // ✅ SEARCH alerts
    @GetMapping
    public List<AlertResponse> search(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String q
    ) {
        return alertService.search(resolved, severity, alertType, entityType, q);
    }
}