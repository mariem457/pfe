package com.example.demo.service;

import com.example.demo.dto.DriverProfileResponse;
import com.example.demo.entity.Driver;
import com.example.demo.entity.Truck;
import com.example.demo.entity.User;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.TruckRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final TruckRepository truckRepository;

    public ProfileService(
            UserRepository userRepository,
            DriverRepository driverRepository,
            TruckRepository truckRepository
    ) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.truckRepository = truckRepository;
    }

    public DriverProfileResponse getCurrentDriverProfile(Authentication authentication) {
        String usernameOrEmail = authentication.getName();

        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Driver driver = driverRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        String vehicleCode = driver.getVehicleCode();
        Long truckId = findTruckIdFromVehicleCode(vehicleCode);

        return new DriverProfileResponse(
                driver.getFullName(),
                user.getEmail(),
                driver.getPhone(),
                user.getUsername(),
                "DRV-" + driver.getId(),
                vehicleCode,
                truckId,
                "Monday - Friday, 8:00 AM - 4:00 PM",
                0,
                0,
                0,
                0
        );
    }

    private Long findTruckIdFromVehicleCode(String vehicleCode) {
        if (vehicleCode == null || vehicleCode.isBlank()) {
            return null;
        }

        return truckRepository.findAll()
                .stream()
                .filter(t -> vehicleCode.equals(t.getTruckCode()))
                .map(Truck::getId)
                .findFirst()
                .orElse(null);
    }
}