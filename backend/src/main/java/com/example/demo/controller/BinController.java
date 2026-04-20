package com.example.demo.controller;

import com.example.demo.dto.BinRequest;
import com.example.demo.dto.BinResponse;
import com.example.demo.service.BinService;
import com.example.demo.service.BinStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bins")
@CrossOrigin("*")
public class BinController {

    private final BinService service;
    private final BinStatusService binStatusService;

    public BinController(BinService service, BinStatusService binStatusService) {
        this.service = service;
        this.binStatusService = binStatusService;
    }

    @GetMapping
    public ResponseEntity<List<BinResponse>> list() {
        List<BinResponse> bins = service.findAll();
        return ResponseEntity.ok(bins);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BinResponse> get(@PathVariable Long id) {
        BinResponse bin = service.findById(id);
        return ResponseEntity.ok(bin);
    }

    @PostMapping
    public ResponseEntity<BinResponse> create(@RequestBody BinRequest request) {
        BinResponse created = service.create(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BinResponse> update(
            @PathVariable Long id,
            @RequestBody BinRequest request
    ) {
        BinResponse updated = service.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/backfill-zones")
    public ResponseEntity<Map<String, Object>> backfillZones() {
        int updated = service.backfillZonesForBinsWithoutZone();

        return ResponseEntity.ok(Map.of(
                "message", "Zones backfilled successfully",
                "updatedBins", updated
        ));
    }
}