package com.example.demo.service;

import com.example.demo.dto.CreateMaintenanceUserRequest;
import com.example.demo.dto.CreateMunicipalityUserRequest;
import com.example.demo.dto.CreateUserResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class UserAdminService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateUserResponse createMunicipalityUser(CreateMunicipalityUserRequest req) {

        if (userRepo.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        String tempPassword = generateTempPassword(10);

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setRole("MUNICIPALITY");
        user.setIsEnabled(true);
        user.setMustChangePassword(true);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));

        user = userRepo.save(user);

        return new CreateUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                tempPassword
        );
    }

    @Transactional
    public CreateUserResponse createMaintenanceUser(CreateMaintenanceUserRequest req) {

        if (userRepo.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        String tempPassword = generateTempPassword(10);

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setRole("MAINTENANCE");
        user.setIsEnabled(true);
        user.setMustChangePassword(true);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));

        user = userRepo.save(user);

        return new CreateUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                tempPassword
        );
    }

    private String generateTempPassword(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}