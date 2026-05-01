package com.example.demo.controller;

import com.example.demo.dto.DriverNotificationResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.DriverNotificationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver/notifications")
public class DriverNotificationController {

    private final DriverNotificationService notificationService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public DriverNotificationController(
            DriverNotificationService notificationService,
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.notificationService = notificationService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<DriverNotificationResponse> getMyNotifications(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing token");
        }

        String token = authHeader.substring(7);

        Claims claims = jwtService.extractClaims(token);
        String email = claims.getSubject();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationService.getNotificationsByUserId(user.getId());
    }
}