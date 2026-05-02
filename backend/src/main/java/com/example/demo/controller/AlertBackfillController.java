package com.example.demo.controller;

import com.example.demo.service.AlertBackfillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts/backfill")
public class AlertBackfillController {

    private final AlertBackfillService alertBackfillService;

    public AlertBackfillController(AlertBackfillService alertBackfillService) {
        this.alertBackfillService = alertBackfillService;
    }

    @PostMapping("/bins")
    public ResponseEntity<Map<String, Object>> backfillBins() {
        int processed = alertBackfillService.backfillLatestBinAlerts();

        return ResponseEntity.ok(Map.of(
                "message", "BIN ALERTS BACKFILLED",
                "processed", processed
        ));
    }
}