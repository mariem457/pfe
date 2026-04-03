package com.example.demo.controller;

import com.example.demo.dto.TelemetryRequest;
import com.example.demo.dto.TelemetryResponse;
import com.example.demo.service.TelemetryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    public TelemetryResponse save(@RequestBody TelemetryRequest req) {
        return telemetryService.saveTelemetry(
                req.getBinCode(),
                req.getFillLevel(),
                req.getBatteryLevel(),
                req.getWeightKg(),
                req.getStatus(),
                req.getSource(),
                req.getRssi(),
                req.getCollected()
        );
    }
}