package com.example.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UpdateUserStatusRequest;
import com.example.demo.dto.UserAdminListResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UserStatsResponse;
import com.example.demo.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserAdminListResponse> list() {
        return service.findAllForAdmin();
    }

    @GetMapping("/stats")
    public UserStatsResponse stats() {
        return service.getStats();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/status")
    public UserAdminListResponse updateStatus(@PathVariable Long id,
                                              @RequestBody UpdateUserStatusRequest req) {
        return service.updateStatus(id, req.isEnabled);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}