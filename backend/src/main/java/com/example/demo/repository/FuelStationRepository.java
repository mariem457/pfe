package com.example.demo.repository;

import com.example.demo.entity.FuelStation;
import com.example.demo.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuelStationRepository extends JpaRepository<FuelStation, Long> {

    List<FuelStation> findByIsActiveTrue();

    List<FuelStation> findByFuelType(Truck.FuelType fuelType);

    List<FuelStation> findByFuelTypeAndIsActiveTrue(Truck.FuelType fuelType);

    List<FuelStation> findByNameContainingIgnoreCase(String name);
}