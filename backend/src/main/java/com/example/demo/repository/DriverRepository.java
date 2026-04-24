package com.example.demo.repository;

import com.example.demo.entity.Driver;
import com.example.demo.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUserId(Long userId);
    Optional<Driver> findByUser_Id(Long userId);

    Optional<Driver> findByVehicleCode(String vehicleCode);
    Optional<Driver> findByUser(User user);
}