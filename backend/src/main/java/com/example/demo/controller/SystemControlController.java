package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.SystemControlService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemControlController {

    private final SystemControlService systemControlService;

    public SystemControlController(SystemControlService systemControlService) {
        this.systemControlService = systemControlService;
    }

    @GetMapping("/overview")
    public SystemOverviewResponse getOverview() {
        return systemControlService.getOverview();
    }

    @GetMapping("/services")
    public List<SystemComponentResponse> getServices() {
        return systemControlService.getComponents();
    }

    @GetMapping("/notifications")
    public List<SystemNotificationResponse> getNotifications() {
        return systemControlService.getNotifications();
    }

    @GetMapping("/database")
    public SystemDatabaseStatusResponse getDatabaseStatus() {
        return systemControlService.getDatabaseStatus();
    }

    @GetMapping("/settings")
    public SystemSettingsResponse getSettings() {
        return systemControlService.getSettings();
    }

    @PutMapping("/settings")
    public SystemSettingsResponse updateSettings(@RequestBody UpdateSystemSettingsRequest req) {
        return systemControlService.updateSettings(req);
    }
}