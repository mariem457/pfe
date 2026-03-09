package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepo;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UserRepository userRepo) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsernameOrEmail(), req.getPassword())
        );

        String principal = auth.getName();

        User user = userRepo.findByUsername(principal)
                .or(() -> userRepo.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // update last login (OffsetDateTime)
        user.setLastLoginAt(OffsetDateTime.now());
        userRepo.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole());

        return new AuthResponse(
                token,
                user.getRole(),
                user.getId(),
                user.getUsername()
        );
    }
}