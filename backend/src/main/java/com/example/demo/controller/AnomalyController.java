package com.example.demo.controller;

import com.example.demo.dto.AnomalyDto;
import com.example.demo.dto.CloseAnomalyRequest;
import com.example.demo.service.AnomalyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    private final AnomalyService anomalyService;

    public AnomalyController(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    // GET /api/anomalies/bin/1
    @GetMapping("/bin/{binId}")
    public ResponseEntity<List<AnomalyDto>> byBin(@PathVariable Long binId) {
        return ResponseEntity.ok(anomalyService.getByBin(binId));
    }

    // GET /api/anomalies/bin/1/active
    @GetMapping("/bin/{binId}/active")
    public ResponseEntity<List<AnomalyDto>> activeByBin(@PathVariable Long binId) {
        return ResponseEntity.ok(anomalyService.getActiveByBin(binId));
    }

    // PATCH /api/anomalies/5/close
    @PatchMapping("/{id}/close")
    public ResponseEntity<AnomalyDto> close(@PathVariable Long id, @RequestBody(required = false) CloseAnomalyRequest req) {
        return ResponseEntity.ok(anomalyService.close(id, req));
    }
}