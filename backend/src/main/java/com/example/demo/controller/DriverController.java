package com.example.demo.controller;

import com.example.demo.dto.CreateDriverRequest;
import com.example.demo.dto.CreateDriverResponse;
import com.example.demo.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // ADMIN creates driver accounts
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateDriverResponse create(@Valid @RequestBody CreateDriverRequest req) {
        return driverService.createDriver(req);
    }
}