package com.example.demo.service;

import com.example.demo.entity.Truck;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FuelManagementServiceImpl implements FuelManagementService {

    private static final double CRITICAL_FUEL_THRESHOLD_LITERS = 10.0;

    // نفس الفكرة اللي في Python
    private static final BigDecimal MIN_FUEL_RESERVE_LITERS = BigDecimal.valueOf(5.0);
    private static final BigDecimal FUEL_SAFETY_FACTOR = BigDecimal.valueOf(0.8);

    @Override
    public double calculateEstimatedAutonomyKm(Truck truck) {
        if (truck == null || truck.getFuelLevelLiters() == null || truck.getFuelConsumptionPerKm() == null) {
            return 0.0;
        }

        if (truck.getFuelConsumptionPerKm().compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        BigDecimal usableFuel = truck.getFuelLevelLiters().subtract(MIN_FUEL_RESERVE_LITERS);
        if (usableFuel.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        BigDecimal rawAutonomyKm = usableFuel.divide(
                truck.getFuelConsumptionPerKm(),
                4,
                RoundingMode.HALF_UP
        );

        BigDecimal safeAutonomyKm = rawAutonomyKm.multiply(FUEL_SAFETY_FACTOR);

        return safeAutonomyKm.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public boolean isFuelCritical(Truck truck) {
        if (truck == null || truck.getFuelLevelLiters() == null) {
            return true;
        }

        return truck.getFuelLevelLiters()
                .compareTo(BigDecimal.valueOf(CRITICAL_FUEL_THRESHOLD_LITERS)) < 0;
    }

    @Override
    public boolean canCompleteDistance(Truck truck, double distanceKm) {
        return calculateEstimatedAutonomyKm(truck) >= distanceKm;
    }
}