package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.SecurityDashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private final SecurityDashboardService securityDashboardService;

    public SecurityController(SecurityDashboardService securityDashboardService) {
        this.securityDashboardService = securityDashboardService;
    }

    @GetMapping("/dashboard")
    public SecurityDashboardResponse getDashboard() {
        return securityDashboardService.getDashboard();
    }

    @GetMapping("/events")
    public List<SecurityEventResponse> getEvents() {
        return securityDashboardService.getEvents();
    }

    @GetMapping("/settings")
    public SecuritySettingsResponse getSettings() {
        return securityDashboardService.getSettings();
    }

    @PutMapping("/settings")
    public SecuritySettingsResponse updateSettings(@RequestBody UpdateSecuritySettingsRequest req) {
        return securityDashboardService.updateSettings(req);
    }

    @GetMapping("/api-keys")
    public List<ApiKeyResponse> getApiKeys() {
        return securityDashboardService.getApiKeys();
    }

    @PostMapping("/api-keys")
    public ApiKeyResponse generateApiKey(@RequestParam(defaultValue = "false") boolean testKey) {
        return securityDashboardService.generateNewKey(testKey);
    }
}