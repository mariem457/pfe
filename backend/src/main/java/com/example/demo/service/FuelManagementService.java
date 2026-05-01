package com.example.demo.service;

import com.example.demo.entity.Truck;

public interface FuelManagementService {

    double calculateEstimatedAutonomyKm(Truck truck);

    boolean isFuelCritical(Truck truck);

    boolean canCompleteDistance(Truck truck, double distanceKm);

    boolean isRefuelRecommended(Truck truck);

    double getRefuelAlertAutonomyThresholdKm();

    double getCriticalFuelThresholdLiters();
}