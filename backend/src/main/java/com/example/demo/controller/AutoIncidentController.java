package com.example.demo.controller;

import com.example.demo.dto.AutoIncidentRunResponse;
import com.example.demo.service.AutoIncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auto-incidents")
@CrossOrigin(origins = "*")
public class AutoIncidentController {

    private final AutoIncidentService autoIncidentService;

    public AutoIncidentController(AutoIncidentService autoIncidentService) {
        this.autoIncidentService = autoIncidentService;
    }

    @PostMapping("/run")
    public ResponseEntity<AutoIncidentRunResponse> runAutoDetection() {
        return ResponseEntity.ok(autoIncidentService.runAutoDetection());
    }

    @PostMapping("/backfill-alerts")
    public ResponseEntity<AutoIncidentRunResponse> backfillIncidentAlerts() {
        return ResponseEntity.ok(autoIncidentService.backfillIncidentAlerts());
    }
}