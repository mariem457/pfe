package com.example.demo.controller;

import com.example.demo.dto.ContactDriverRequest;
import com.example.demo.dto.DriverNotificationRespondRequest;
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
@CrossOrigin(origins = "*")
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
        User user = extractUser(request);
        return notificationService.getNotificationsByUserId(user.getId());
    }

    @PostMapping("/contact-driver")
    public DriverNotificationResponse contactDriver(@RequestBody ContactDriverRequest request) {
        if (request.getIncidentId() == null) {
            throw new RuntimeException("incidentId is required");
        }

        return notificationService.contactDriverForIncident(
                request.getIncidentId(),
                request.getMessage()
        );
    }

    @PatchMapping("/{id}/read")
    public DriverNotificationResponse markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @PatchMapping("/{id}/respond")
    public DriverNotificationResponse respond(
            @PathVariable Long id,
            @RequestBody DriverNotificationRespondRequest request
    ) {
        return notificationService.respond(id, request.getResponse());
    }

    @GetMapping("/incident/{incidentId}/latest")
    public DriverNotificationResponse getLatestForIncident(@PathVariable Long incidentId) {
        return notificationService.getLatestByIncident(incidentId);
    }

    private User extractUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing token");
        }

        String token = authHeader.substring(7);
        Claims claims = jwtService.extractClaims(token);
        String email = claims.getSubject();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}