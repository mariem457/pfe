package com.example.demo.repository;

import com.example.demo.entity.Truck;
import com.example.demo.entity.Truck.TruckStatus;
import com.example.demo.entity.Truck.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TruckRepository extends JpaRepository<Truck, Long> {

    Optional<Truck> findByTruckCode(String truckCode);

    Optional<Truck> findByPlateNumber(String plateNumber);

    List<Truck> findByStatus(TruckStatus status);

    List<Truck> findByStatusAndIsActiveTrue(TruckStatus status);

    List<Truck> findByIsActiveTrue();

    List<Truck> findByFuelType(FuelType fuelType);
    boolean existsByPlateNumber(String plateNumber);
}