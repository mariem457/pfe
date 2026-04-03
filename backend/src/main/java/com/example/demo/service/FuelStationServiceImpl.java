package com.example.demo.service;

import com.example.demo.entity.FuelStation;
import com.example.demo.entity.Truck;
import com.example.demo.repository.FuelStationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuelStationServiceImpl implements FuelStationService {

    private final FuelStationRepository fuelStationRepository;

    public FuelStationServiceImpl(FuelStationRepository fuelStationRepository) {
        this.fuelStationRepository = fuelStationRepository;
    }

    @Override
    public FuelStation findNearestCompatibleStation(Truck truck) {
        if (truck == null || truck.getFuelType() == null) {
            return null;
        }

        List<FuelStation> stations = fuelStationRepository.findByFuelTypeAndIsActiveTrue(truck.getFuelType());

        if (stations == null || stations.isEmpty()) {
            return null;
        }

        if (truck.getLastKnownLat() == null || truck.getLastKnownLng() == null) {
            return stations.get(0);
        }

        FuelStation nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (FuelStation station : stations) {
            if (station.getLat() == null || station.getLng() == null) {
                continue;
            }

            double distance = haversineDistanceKm(
                truck.getLastKnownLat(),
                truck.getLastKnownLng(),
                station.getLat(),
                station.getLng()
            );

            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = station;
            }
        }

        return nearest != null ? nearest : stations.get(0);
    }

    private double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }
}