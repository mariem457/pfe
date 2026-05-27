package com.example.demo.controller;
import com.example.demo.dto.MissionResponse;
import com.example.demo.service.RoutingOptimizationService;
import com.example.demo.dto.AlertCreateRequest;
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
    private final RoutingOptimizationService routingOptimizationService;
   

    public AlertController(AlertService alertService , RoutingOptimizationService routingOptimizationService) {
        this.alertService = alertService;
        this.routingOptimizationService = routingOptimizationService;
    }

    @PostMapping
    public AlertResponse create(@RequestBody AlertCreateRequest req) {
        return alertService.create(req);
    }

    @GetMapping("/open")
    public List<AlertResponse> getOpenAlerts() {
        return alertService.getOpenAlerts();
    }

    @GetMapping("/bins/{binId}")
    public List<AlertResponse> getAlertsByBin(
            @PathVariable Long binId,
            @RequestParam(defaultValue = "false") boolean onlyOpen
    ) {
        return alertService.getAlertsByBin(binId, onlyOpen);
    }

    @PatchMapping("/{id}/resolve")
    public AlertResponse resolve(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        return alertService.resolve(id, username);
    }

    @PostMapping("/{id}/treat-qr-code")
    public AlertResponse treatQrCodeProblem(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        return alertService.treatQrCodeProblem(id, username);
    }
    @PostMapping("/{id}/create-exception-mission")
    public List<MissionResponse> createExceptionMission(@PathVariable Long id) {
        return routingOptimizationService.planExceptionMissionFromAlert(id);
    }

    @GetMapping("/missions/{missionId}")
    public List<AlertResponse> getAlertsByMission(@PathVariable Long missionId) {
        return alertService.getAlertsByMission(missionId);
    }

    @GetMapping("/{id}")
    public AlertDetailsResponse getDetails(@PathVariable Long id) {
        return alertService.getAlertDetails(id);
    }

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
