package com.example.demo.service;

import com.example.demo.dto.CreateDriverRequest;
import com.example.demo.dto.CreateDriverResponse;
import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.Driver;
import com.example.demo.entity.User;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DriverService {

    private final UserRepository userRepo;
    private final DriverRepository driverRepo;
    private final PasswordEncoder passwordEncoder;

    public DriverService(UserRepository userRepo,
                         DriverRepository driverRepo,
                         PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.driverRepo = driverRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateDriverResponse createDriver(CreateDriverRequest req) {

        if (userRepo.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setEmail(req.getEmail().trim());
        user.setRole("DRIVER");
        user.setIsEnabled(true);
        user.setMustChangePassword(false);
        user.setAccountStatus(AccountStatus.PENDING);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user = userRepo.save(user);

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setFullName(req.getFullName().trim());
        driver.setPhone(req.getPhone().trim());
        driver.setVehicleCode(req.getVehicleCode());
        driver.setIsActive(true);
        driver = driverRepo.save(driver);

        return new CreateDriverResponse(
                user.getId(),
                driver.getId(),
                user.getUsername(),
                user.getRole(),
                user.getAccountStatus().name()
        );
    }
}