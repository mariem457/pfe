package com.example.demo.controller;

import com.example.demo.dto.CreateMaintenanceUserRequest;
import com.example.demo.dto.CreateMunicipalityUserRequest;
import com.example.demo.dto.CreateUserResponse;
import com.example.demo.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    // ADMIN creates municipality agents
    @PostMapping("/municipality")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse createMunicipality(@Valid @RequestBody CreateMunicipalityUserRequest req) {
        return userAdminService.createMunicipalityUser(req);
    }

    // ADMIN creates maintenance agents
    @PostMapping("/maintenance")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse createMaintenance(@Valid @RequestBody CreateMaintenanceUserRequest req) {
        return userAdminService.createMaintenanceUser(req);
    }
}