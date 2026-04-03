package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.ResetPasswordByCodeRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.AuthBusinessService;
import com.example.demo.service.SecurityDashboardService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.VerifyResetCodeRequest;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final SecurityDashboardService securityDashboardService;
    private final AuthBusinessService authBusinessService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public AuthController(
            AuthenticationManager authManager,
            JwtService jwtService,
            UserRepository userRepo,
            SecurityDashboardService securityDashboardService,
            AuthBusinessService authBusinessService
    ) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.securityDashboardService = securityDashboardService;
        this.authBusinessService = authBusinessService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
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

            String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getTokenVersion());

            RefreshToken refreshToken = authBusinessService.createRefreshToken(user, req.isRememberMe());

            Cookie cookie = new Cookie("refresh_token", refreshToken.getToken());
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // true en production avec HTTPS
            cookie.setPath("/api/auth");
            cookie.setMaxAge(req.isRememberMe() ? 30 * 24 * 60 * 60 : 24 * 60 * 60);
            response.addCookie(cookie);

            return new AuthResponse(
                    token,
                    user.getRole(),
                    user.getId(),
                    user.getUsername(),
                    user.getMustChangePassword()
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

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue("refresh_token") String refreshToken) {
        return authBusinessService.refresh(refreshToken);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authBusinessService.forgotPassword(request, frontendUrl);

        return ResponseEntity.ok(Map.of(
                "message", "Si un compte existe, les instructions de réinitialisation ont été envoyées."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authBusinessService.resetPassword(request);

        return ResponseEntity.ok(Map.of(
                "message", "Mot de passe réinitialisé avec succès."
        ));
    }

    @PostMapping("/reset-password-by-code")
    public ResponseEntity<?> resetPasswordByCode(@Valid @RequestBody ResetPasswordByCodeRequest request) {
        authBusinessService.resetPasswordByCode(request);

        return ResponseEntity.ok(Map.of(
                "message", "Mot de passe réinitialisé avec succès."
        ));
    }
    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        authBusinessService.verifyResetCode(request);

        return ResponseEntity.ok(Map.of(
                "message", "Code vérifié avec succès."
        ));
    }
}