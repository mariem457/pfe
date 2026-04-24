package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.User;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository repo;
    private final DriverRepository driverRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository repo,
            DriverRepository driverRepo,
            PasswordEncoder passwordEncoder
    ) {
        this.repo = repo;
        this.driverRepo = driverRepo;
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

            int currentVersion = u.getTokenVersion() == null ? 0 : u.getTokenVersion();
            u.setTokenVersion(currentVersion + 1);

            u.setMustChangePassword(false);
        }

        if (req.getRole() != null) {
            u.setRole(req.getRole());
        }

        if (req.getIsEnabled() != null) {
            u.setIsEnabled(req.getIsEnabled());

            if (!Boolean.TRUE.equals(req.getIsEnabled())) {
                int currentVersion = u.getTokenVersion() == null ? 0 : u.getTokenVersion();
                u.setTokenVersion(currentVersion + 1);
            }
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

        if (!Boolean.TRUE.equals(isEnabled)) {
            int currentVersion = u.getTokenVersion() == null ? 0 : u.getTokenVersion();
            u.setTokenVersion(currentVersion + 1);
        }

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

        if (!isStrongPassword(req.getNewPassword())) {
            throw new IllegalArgumentException(
                    "new password must contain at least 8 characters, uppercase, lowercase, number and special character"
            );
        }

        if (passwordEncoder.matches(req.getNewPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("new password must be different from current password");
        }

        u.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        u.setMustChangePassword(false);

        int currentVersion = u.getTokenVersion() == null ? 0 : u.getTokenVersion();
        u.setTokenVersion(currentVersion + 1);

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

    private boolean isStrongPassword(String password) {
        if (password == null) return false;

        return password.length() >= 8
                && Pattern.compile("[A-Z]").matcher(password).find()
                && Pattern.compile("[a-z]").matcher(password).find()
                && Pattern.compile("[0-9]").matcher(password).find()
                && Pattern.compile("[^A-Za-z0-9]").matcher(password).find();
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
        r.accountStatus = u.getAccountStatus() != null ? u.getAccountStatus().name() : null;
        r.lastLoginAt = u.getLastLoginAt();
        r.createdAt = u.getCreatedAt();

        if ("DRIVER".equalsIgnoreCase(u.getRole())) {
            driverRepo.findByUser(u).ifPresent(driver -> {
                r.fullName = driver.getFullName() != null && !driver.getFullName().isBlank()
                        ? driver.getFullName()
                        : u.getUsername();

                r.phone = driver.getPhone() != null && !driver.getPhone().isBlank()
                        ? driver.getPhone()
                        : "--";
            });
        }

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