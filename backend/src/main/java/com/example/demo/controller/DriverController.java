package com.example.demo.controller;

import com.example.demo.dto.BinScanRequest;
import com.example.demo.dto.CreateDriverRequest;
import com.example.demo.dto.CreateDriverResponse;
import com.example.demo.dto.DriverBinDto;
import com.example.demo.dto.DriverListResponse;
import com.example.demo.dto.DriverProfileResponse;
import com.example.demo.entity.MissionBin;
import com.example.demo.service.DriverScanService;
import com.example.demo.service.DriverService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final DriverScanService driverScanService;

    public DriverController(
            DriverService driverService,
            DriverScanService driverScanService
    ) {
        this.driverService = driverService;
        this.driverScanService = driverScanService;
    }

    @GetMapping
    public List<DriverListResponse> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/available")
    public List<DriverListResponse> getAvailableDrivers() {
        return driverService.getAvailableDrivers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateDriverResponse create(
            @Valid @RequestBody CreateDriverRequest req
    ) {
        return driverService.createDriver(req);
    }

    @GetMapping("/{userId}/my-bins")
    public List<DriverBinDto> getMyBins(
            @PathVariable Long userId
    ) {
        return driverService.getMyBins(userId);
    }

    @GetMapping("/{userId}/profile")
    public DriverProfileResponse getMyProfile(
            @PathVariable Long userId
    ) {
        return driverService.getMyProfile(userId);
    }

    @PostMapping("/bin-scan")
    @Transactional
    public ResponseEntity<?> scanBin(
            @RequestBody BinScanRequest request,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(
                            "success", false,
                            "message", "Authentication required"
                    )
            );
        }

        String usernameOrEmail = authentication.getName();

        MissionBin updated = driverScanService.scanAndCollect(
                request.getBinCode(),
                usernameOrEmail,
                request
        );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Poubelle collectée avec succès",
                        "missionBinId", updated.getId(),
                        "binCode", request.getBinCode()
                )
        );
    }
}