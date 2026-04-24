package com.example.demo.controller;

import com.example.demo.dto.BinScanRequest;
import com.example.demo.dto.CreateDriverRequest;
import com.example.demo.dto.CreateDriverResponse;
import com.example.demo.dto.DriverBinDto;
import com.example.demo.dto.DriverProfileResponse;
import com.example.demo.entity.MissionBin;
import com.example.demo.service.DriverScanService;
import com.example.demo.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final DriverScanService driverScanService;

    public DriverController(DriverService driverService, DriverScanService driverScanService) {
        this.driverService = driverService;
        this.driverScanService = driverScanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateDriverResponse create(@Valid @RequestBody CreateDriverRequest req) {
        return driverService.createDriver(req);
    }

    @GetMapping("/{userId}/my-bins")
    public List<DriverBinDto> getMyBins(@PathVariable Long userId) {
        return driverService.getMyBins(userId);
    }

    @GetMapping("/{userId}/profile")
    public DriverProfileResponse getMyProfile(@PathVariable Long userId) {
        return driverService.getMyProfile(userId);
    }

    @PostMapping("/bin-scan")
    public ResponseEntity<?> scanBin(
            @RequestBody BinScanRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        String usernameOrEmail = authentication.getName();

        MissionBin updated = driverScanService.scanAndCollect(
                request.getBinCode(),
                usernameOrEmail,
                request
        );

        return ResponseEntity.ok(updated);
    }
}