package com.example.demo.service;

import com.example.demo.entity.Truck;

public interface FleetEligibilityService {

    boolean isTruckEligible(Truck truck, double requiredLoadKg, double estimatedDistanceKm);

    boolean hasEnoughCapacity(Truck truck, double requiredLoadKg);

    boolean hasEnoughFuel(Truck truck, double estimatedDistanceKm);
}