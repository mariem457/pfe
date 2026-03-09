package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.SettingsProfileResponse;
import com.example.demo.dto.UpdateSettingsProfileRequest;
import com.example.demo.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserService userService;

    public SettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public SettingsProfileResponse getProfile(Authentication authentication) {
        String username = authentication.getName();
        return userService.getSettingsProfileByUsername(username);
    }

    @PutMapping("/profile")
    public SettingsProfileResponse updateProfile(
            Authentication authentication,
            @RequestBody UpdateSettingsProfileRequest request
    ) {
        String username = authentication.getName();
        return userService.updateSettingsProfileByUsername(username, request);
    }

    @PutMapping("/password")
    public void changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request
    ) {
        String username = authentication.getName();
        userService.changePasswordByUsername(username, request);
    }
}