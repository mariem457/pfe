package com.example.demo.service;

import com.example.demo.entity.FuelStation;
import com.example.demo.entity.Truck;

public interface FuelStationService {
    FuelStation findNearestCompatibleStation(Truck truck);

    double distanceToNearestCompatibleStationKm(Truck truck);
}