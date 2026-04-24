package com.example.demo.controller;

import com.example.demo.dto.DriverProfileResponse;
import com.example.demo.service.DriverService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final DriverService driverService;

    public SettingsController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping("/profile")
    public DriverProfileResponse getProfile(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication required");
        }

        return driverService.getMyProfileByUsernameOrEmail(authentication.getName());
    }
}