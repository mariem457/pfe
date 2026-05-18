package com.example.demo.repository;

import com.example.demo.entity.DriverNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverNotificationRepository extends JpaRepository<DriverNotification, Long> {

    List<DriverNotification> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    List<DriverNotification> findTop25ByDriverIdOrderByCreatedAtDesc(Long driverId);

    Optional<DriverNotification> findTopByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
