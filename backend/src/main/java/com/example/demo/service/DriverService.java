package com.example.demo.service;

import com.example.demo.dto.CreateDriverRequest;
import com.example.demo.dto.CreateDriverResponse;
import com.example.demo.entity.Driver;
import com.example.demo.entity.User;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.sms.SmsService;   // ✅ add
import jakarta.transaction.Transactional;
import org.slf4j.Logger;                  // ✅ add
import org.slf4j.LoggerFactory;           // ✅ add
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);

    private final UserRepository userRepo;
    private final DriverRepository driverRepo;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService; // ✅ add

    public DriverService(UserRepository userRepo,
                         DriverRepository driverRepo,
                         PasswordEncoder passwordEncoder,
                         SmsService smsService) {        // ✅ add
        this.userRepo = userRepo;
        this.driverRepo = driverRepo;
        this.passwordEncoder = passwordEncoder;
        this.smsService = smsService;                   // ✅ add
    }

    @Transactional
    public CreateDriverResponse createDriver(CreateDriverRequest req) {

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
        user.setRole("DRIVER");
        user.setIsEnabled(true);
        user.setMustChangePassword(true);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user = userRepo.save(user);

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setFullName(req.getFullName());
        driver.setPhone(req.getPhone());
        driver.setVehicleCode(req.getVehicleCode());
        driver.setIsActive(true);
        driver = driverRepo.save(driver);

        // ✅ SMS send (non-bloquant)
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            try {
                smsService.sendDriverCredentials(req.getPhone(), user.getUsername(), tempPassword);
            } catch (Exception ex) {
                log.warn("Driver created but SMS failed for {}: {}", req.getPhone(), ex.getMessage());
            }
        }

        return new CreateDriverResponse(
                user.getId(),
                driver.getId(),
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