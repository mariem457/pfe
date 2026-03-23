package com.example.demo.service;

import com.example.demo.entity.Truck;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FuelManagementServiceImpl implements FuelManagementService {

    private static final double CRITICAL_FUEL_THRESHOLD_LITERS = 10.0;

    @Override
    public double calculateEstimatedAutonomyKm(Truck truck) {
        if (truck == null || truck.getFuelLevelLiters() == null || truck.getFuelConsumptionPerKm() == null) {
            return 0.0;
        }

        if (truck.getFuelConsumptionPerKm().compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        return truck.getFuelLevelLiters()
                .divide(truck.getFuelConsumptionPerKm(), 2, RoundingMode.HALF_UP)
                .doubleValue();
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