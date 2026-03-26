package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.SecurityDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final SecurityDashboardService securityDashboardService;

    public AuthController(
            AuthenticationManager authManager,
            JwtService jwtService,
            UserRepository userRepo,
            SecurityDashboardService securityDashboardService
    ) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.securityDashboardService = securityDashboardService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req, HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String device = userAgent != null && userAgent.toLowerCase().contains("mobile") ? "Mobile" : "Ordinateur";

        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsernameOrEmail(), req.getPassword())
            );

            String principal = auth.getName();

            User user = userRepo.findByUsername(principal)
                    .or(() -> userRepo.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setLastLoginAt(OffsetDateTime.now());
            userRepo.save(user);

            securityDashboardService.logEvent(
                    "LOGIN_SUCCESS",
                    "Connexion",
                    "SUCCESS",
                    user.getUsername(),
                    device,
                    ip,
                    "Localisation inconnue"
            );

            String token = jwtService.generateToken(user.getUsername(), user.getRole());

            return new AuthResponse(
                    token,
                    user.getRole(),
                    user.getId(),
                    user.getUsername()
            );

        } catch (BadCredentialsException ex) {
            securityDashboardService.logEvent(
                    "LOGIN_FAILED",
                    "Échec de connexion",
                    "FAILED",
                    req.getUsernameOrEmail(),
                    device,
                    ip,
                    "Localisation inconnue"
            );
            throw ex;
        }
    }
}