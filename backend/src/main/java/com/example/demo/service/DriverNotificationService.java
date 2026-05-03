package com.example.demo.service;

import com.example.demo.dto.DriverNotificationResponse;
import com.example.demo.entity.Driver;
import com.example.demo.entity.DriverNotification;
import com.example.demo.repository.DriverNotificationRepository;
import com.example.demo.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverNotificationService {

    private final DriverNotificationRepository notificationRepository;
    private final DriverRepository driverRepository;

    public DriverNotificationService(
            DriverNotificationRepository notificationRepository,
            DriverRepository driverRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.driverRepository = driverRepository;
    }

    public List<DriverNotificationResponse> getNotificationsByUserId(Long userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        return notificationRepository
                .findByDriverIdOrderByCreatedAtDesc(driver.getId())
                .stream()
                .map(DriverNotificationResponse::new)
                .toList();
    }

    public DriverNotification createNotification(
            Long driverId,
            String type,
            String title,
            String message
    ) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        DriverNotification notification = new DriverNotification();
        notification.setDriver(driver);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }
}