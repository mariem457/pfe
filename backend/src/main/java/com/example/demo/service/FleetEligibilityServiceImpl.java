package com.example.demo.service;

import com.example.demo.entity.Truck;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FleetEligibilityServiceImpl implements FleetEligibilityService {

    @Override
    public boolean isTruckEligible(Truck truck, double requiredLoadKg, double estimatedDistanceKm) {
        if (truck == null) {
            return false;
        }

        if (truck.getIsActive() == null || !truck.getIsActive()) {
            return false;
        }

        if (truck.getStatus() == null || truck.getStatus() != Truck.TruckStatus.AVAILABLE) {
            return false;
        }

        return hasEnoughCapacity(truck, requiredLoadKg);
    }

    @Override
    public boolean hasEnoughCapacity(Truck truck, double requiredLoadKg) {
        if (truck == null) {
            return false;
        }

        BigDecimal maxLoad = truck.getMaxLoadKg();
        BigDecimal currentLoad = truck.getCurrentLoadKg() != null
                ? truck.getCurrentLoadKg()
                : BigDecimal.ZERO;

        if (maxLoad == null) {
            return false;
        }

        BigDecimal remainingCapacity = maxLoad.subtract(currentLoad);
        return remainingCapacity.compareTo(BigDecimal.valueOf(requiredLoadKg)) >= 0;
    }

    @Override
    public boolean hasEnoughFuel(Truck truck, double estimatedDistanceKm) {
        return true;
    }
}