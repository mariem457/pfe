package com.example.demo.controller;

import com.example.demo.entity.BinSensorData;
import com.example.demo.repository.BinSensorDataRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensor-data")
@CrossOrigin("*")
public class BinSensorDataController {

    private final BinSensorDataRepository repository;

    public BinSensorDataController(BinSensorDataRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/latest/{binId}")
    public ResponseEntity<?> getLatestByBinId(@PathVariable String binId) {
        return repository.findTopByBinIdOrderByCreatedAtDesc(binId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}