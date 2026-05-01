package com.example.demo.controller;

import com.example.demo.dto.TruckRequestDto;
import com.example.demo.dto.TruckResponseDto;
import com.example.demo.dto.TruckStatusUpdateDto;
import com.example.demo.service.TruckService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/trucks")
@CrossOrigin(origins = "*")
public class TruckController {

    private final TruckService truckService;

    public TruckController(TruckService truckService) {
        this.truckService = truckService;
    }

    @PostMapping
    public ResponseEntity<TruckResponseDto> createTruck( @Valid @RequestBody TruckRequestDto request) {
        TruckResponseDto response = truckService.createTruck(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{truckId}")
    public ResponseEntity<TruckResponseDto> updateTruck(@PathVariable Long truckId,
                                                        @RequestBody TruckRequestDto request) {
        TruckResponseDto response = truckService.updateTruck(truckId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{truckId}/status")
    public ResponseEntity<TruckResponseDto> updateTruckStatus(@PathVariable Long truckId,
                                                              @RequestBody TruckStatusUpdateDto request) {
        TruckResponseDto response = truckService.updateTruckStatus(truckId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{truckId}")
    public ResponseEntity<TruckResponseDto> getTruckById(@PathVariable Long truckId) {
        TruckResponseDto response = truckService.getTruckById(truckId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TruckResponseDto>> getAllTrucks() {
        return ResponseEntity.ok(truckService.getAllTrucks());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TruckResponseDto>> getActiveTrucks() {
        return ResponseEntity.ok(truckService.getActiveTrucks());
    }

    @DeleteMapping("/{truckId}")
    public ResponseEntity<Void> deactivateTruck(@PathVariable Long truckId) {
        truckService.deactivateTruck(truckId);
        return ResponseEntity.noContent().build();
    }
}