package com.example.demo.service;

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.SettingsProfileResponse;
import com.example.demo.dto.UpdateSettingsProfileRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UpdateUserStatusRequest;
import com.example.demo.dto.UserAdminListResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UserStatsResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public List<UserAdminListResponse> findAllForAdmin() {
        return repo.findAll().stream().map(this::toAdminListResponse).toList();
    }

    public UserStatsResponse getStats() {
        List<User> users = repo.findAll();

        UserStatsResponse stats = new UserStatsResponse();
        stats.totalUsers = users.size();
        stats.activeUsers = users.stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsEnabled()))
                .count();
        stats.inactiveUsers = users.stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsEnabled()))
                .count();
        stats.drivers = users.stream()
                .filter(u -> "DRIVER".equalsIgnoreCase(u.getRole()))
                .count();

        return stats;
    }

    public UserResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User u = getOrThrow(id);

        if (req.getUsername() != null && !req.getUsername().equals(u.getUsername())) {
            if (repo.existsByUsername(req.getUsername())) {
                throw new IllegalArgumentException("username already exists");
            }
            u.setUsername(req.getUsername());
        }

        if (req.getEmail() != null && !req.getEmail().equals(u.getEmail())) {
            if (repo.existsByEmail(req.getEmail())) {
                throw new IllegalArgumentException("email already exists");
            }
            u.setEmail(req.getEmail());
        }

        if (req.getPasswordHash() != null && !req.getPasswordHash().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(req.getPasswordHash()));
        }

        if (req.getRole() != null) {
            u.setRole(req.getRole());
        }

        if (req.getIsEnabled() != null) {
            u.setIsEnabled(req.getIsEnabled());
        }

        return toResponse(repo.save(u));
    }

    @Transactional
    public UserAdminListResponse updateStatus(Long id, Boolean isEnabled) {
        User u = getOrThrow(id);

        if (isEnabled == null) {
            throw new IllegalArgumentException("isEnabled is required");
        }

        u.setIsEnabled(isEnabled);

        return toAdminListResponse(repo.save(u));
    }

    public SettingsProfileResponse getSettingsProfileByUsername(String username) {
        User u = repo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        return new SettingsProfileResponse(
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getJobTitle(),
                u.getOrganization()
        );
    }

    @Transactional
    public SettingsProfileResponse updateSettingsProfileByUsername(String username, UpdateSettingsProfileRequest req) {
        User u = repo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        if (req.getEmail() != null && !req.getEmail().equals(u.getEmail())) {
            if (repo.existsByEmail(req.getEmail())) {
                throw new IllegalArgumentException("email already exists");
            }
            u.setEmail(req.getEmail());
        }

        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setJobTitle(req.getFunction());
        u.setOrganization(req.getOrganization());

        User saved = repo.save(u);

        return new SettingsProfileResponse(
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getJobTitle(),
                saved.getOrganization()
        );
    }

    @Transactional
    public void changePasswordByUsername(String username, ChangePasswordRequest req) {
        User u = repo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("current password is required");
        }

        if (req.getNewPassword() == null || req.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("new password is required");
        }

        if (req.getConfirmPassword() == null || req.getConfirmPassword().isBlank()) {
            throw new IllegalArgumentException("confirm password is required");
        }

        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("current password is invalid");
        }

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("password confirmation does not match");
        }

        if (req.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("new password must contain at least 6 characters");
        }

        u.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        repo.save(u);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("user not found");
        }
        repo.deleteById(id);
    }

    private User getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.email = u.getEmail();
        r.role = u.getRole();
        r.isEnabled = u.getIsEnabled();
        r.lastLoginAt = u.getLastLoginAt();
        r.createdAt = u.getCreatedAt();
        r.updatedAt = u.getUpdatedAt();
        return r;
    }

    private UserAdminListResponse toAdminListResponse(User u) {
        UserAdminListResponse r = new UserAdminListResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.fullName = buildFullName(u);
        r.email = u.getEmail();
        r.phone = "--";
        r.role = u.getRole();
        r.isEnabled = u.getIsEnabled();
        r.lastLoginAt = u.getLastLoginAt();
        return r;
    }

    private String buildFullName(User u) {
        String firstName = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String lastName = u.getLastName() != null ? u.getLastName().trim() : "";

        String fullName = (firstName + " " + lastName).trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        return u.getUsername();
    }
}