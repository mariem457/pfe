package com.example.demo.service;

import com.example.demo.dto.DriverProfileResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public DriverProfileResponse getCurrentDriverProfile(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";

        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) {
            fullName = "Chauffeur #" + user.getId();
        }

        return new DriverProfileResponse(
                fullName,
                user.getEmail(),
                null,
                user.getUsername(),
                "DRV-" + user.getId(),
                "TR-" + user.getId(),
                "Monday - Friday, 8:00 AM - 4:00 PM",
                120,
                95,
                300,
                15
        );
    }
}