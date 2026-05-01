package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.AlertService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    public AlertResponse create(@RequestBody AlertCreateRequest req) {
        return alertService.create(req);
    }

    // ✅ all open alerts
    @GetMapping("/open")
    public List<AlertResponse> openAlerts() {
        return alertService.getOpenAlerts();
    }

    // ✅ alerts by bin
    @GetMapping("/bins/{binId}")
    public List<AlertResponse> byBin(
            @PathVariable Long binId,
            @RequestParam(defaultValue = "false") boolean onlyOpen
    ) {
        return alertService.getAlertsByBin(binId, onlyOpen);
    }

    // ✅ details
    @GetMapping("/{alertId}")
    public AlertDetailsResponse details(@PathVariable Long alertId) {
        return alertService.getAlertDetails(alertId);
    }

    // ✅ resolve
    @PatchMapping("/{id}/resolve")
    public AlertResponse resolve(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return alertService.resolve(id, authentication.getName());
    }

    // ✅ SEARCH / FILTERS  (هذا اللي يناديه الـ front)
    @GetMapping
    public List<AlertResponse> search(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String q
    ) {
        return alertService.search(resolved, severity, alertType, q);
    }
}