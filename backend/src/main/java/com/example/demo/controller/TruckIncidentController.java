package com.example.demo.controller;

import com.example.demo.dto.IncidentStatusUpdateDto;
import com.example.demo.dto.TruckIncidentRequestDto;
import com.example.demo.dto.TruckIncidentResponseDto;
import com.example.demo.service.TruckIncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/truck-incidents")
@CrossOrigin(origins = "*")
public class TruckIncidentController {

    private final TruckIncidentService truckIncidentService;

    public TruckIncidentController(TruckIncidentService truckIncidentService) {
        this.truckIncidentService = truckIncidentService;
    }

    @PostMapping
    public ResponseEntity<TruckIncidentResponseDto> createIncident(@RequestBody TruckIncidentRequestDto request) {
        TruckIncidentResponseDto response = truckIncidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{incidentId}/status")
    public ResponseEntity<TruckIncidentResponseDto> updateIncidentStatus(@PathVariable Long incidentId,
                                                                         @RequestBody IncidentStatusUpdateDto request) {
        TruckIncidentResponseDto response = truckIncidentService.updateIncidentStatus(incidentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<TruckIncidentResponseDto> getIncidentById(@PathVariable Long incidentId) {
        TruckIncidentResponseDto response = truckIncidentService.getIncidentById(incidentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TruckIncidentResponseDto>> getAllIncidents() {
        return ResponseEntity.ok(truckIncidentService.getAllIncidents());
    }

    @GetMapping("/open")
    public ResponseEntity<List<TruckIncidentResponseDto>> getOpenIncidents() {
        return ResponseEntity.ok(truckIncidentService.getOpenIncidents());
    }

    @GetMapping("/truck/{truckId}")
    public ResponseEntity<List<TruckIncidentResponseDto>> getIncidentsByTruck(@PathVariable Long truckId) {
        return ResponseEntity.ok(truckIncidentService.getIncidentsByTruck(truckId));
    }
}