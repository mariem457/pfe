package com.example.demo.controller;

import com.example.demo.dto.TruckLocationRequest;
import com.example.demo.dto.TruckLocationResponse;
import com.example.demo.service.TruckLocationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/truck-locations")
public class TruckLocationController {

    private final TruckLocationService truckLocationService;

    public TruckLocationController(TruckLocationService truckLocationService) {
        this.truckLocationService = truckLocationService;
    }

    @PostMapping
    public TruckLocationResponse save(@RequestBody TruckLocationRequest request) {
        return truckLocationService.save(request);
    }
}