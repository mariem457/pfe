package com.example.demo.service;

import com.example.demo.dto.AutoIncidentRunResponse;
import com.example.demo.dto.TruckIncidentRequestDto;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckIncident;
import com.example.demo.entity.TruckLocation;
import com.example.demo.repository.TruckIncidentRepository;
import com.example.demo.repository.TruckLocationRepository;
import com.example.demo.repository.TruckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AutoIncidentService {

    private static final int FUEL_LOW_PERCENT = 20;
    private static final int GPS_TIMEOUT_MINUTES = 10;

    private final TruckRepository truckRepository;
    private final TruckLocationRepository truckLocationRepository;
    private final TruckIncidentRepository truckIncidentRepository;
    private final TruckIncidentService truckIncidentService;
    private final SmartAlertService smartAlertService;

    public AutoIncidentService(
            TruckRepository truckRepository,
            TruckLocationRepository truckLocationRepository,
            TruckIncidentRepository truckIncidentRepository,
            TruckIncidentService truckIncidentService,
            SmartAlertService smartAlertService
    ) {
        this.truckRepository = truckRepository;
        this.truckLocationRepository = truckLocationRepository;
        this.truckIncidentRepository = truckIncidentRepository;
        this.truckIncidentService = truckIncidentService;
        this.smartAlertService = smartAlertService;
    }

    @Transactional
    public AutoIncidentRunResponse runAutoDetection() {
        List<Truck> trucks = truckRepository.findByIsActiveTrue();

        int created = 0;

        for (Truck truck : trucks) {
            if (detectFuelLow(truck)) {
                created++;
            }

            if (detectOverload(truck)) {
                created++;
            }

            if (detectGpsLost(truck)) {
                created++;
            }
        }

        return new AutoIncidentRunResponse(trucks.size(), created);
    }

    @Transactional
    public AutoIncidentRunResponse backfillIncidentAlerts() {
        List<TruckIncident> openIncidents = truckIncidentRepository.findByStatusIn(
                List.of(
                        TruckIncident.IncidentStatus.OPEN,
                        TruckIncident.IncidentStatus.IN_PROGRESS
                )
        );

        int created = smartAlertService.backfillOpenTruckIncidentAlerts(openIncidents);

        return new AutoIncidentRunResponse(openIncidents.size(), created);
    }

    private boolean detectFuelLow(Truck truck) {
        if (truck.getFuelLevelLiters() == null || truck.getTankCapacityLiters() == null) {
            return false;
        }

        if (truck.getTankCapacityLiters().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        int fuelPercent = truck.getFuelLevelLiters()
                .multiply(BigDecimal.valueOf(100))
                .divide(truck.getTankCapacityLiters(), 0, java.math.RoundingMode.HALF_UP)
                .intValue();

        if (fuelPercent >= FUEL_LOW_PERCENT) {
            return false;
        }

        return createIfNotExists(
                truck,
                TruckIncident.IncidentType.FUEL_LOW,
                TruckIncident.Severity.MEDIUM,
                "Incident automatique: niveau carburant faible (" + fuelPercent + "%)."
        );
    }

    private boolean detectOverload(Truck truck) {
        if (truck.getCurrentLoadKg() == null || truck.getMaxLoadKg() == null) {
            return false;
        }

        if (truck.getMaxLoadKg().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if (truck.getCurrentLoadKg().compareTo(truck.getMaxLoadKg()) <= 0) {
            return false;
        }

        return createIfNotExists(
                truck,
                TruckIncident.IncidentType.OVERLOAD,
                TruckIncident.Severity.HIGH,
                "Incident automatique: surcharge détectée. Charge actuelle: "
                        + truck.getCurrentLoadKg()
                        + " Kg / Charge max: "
                        + truck.getMaxLoadKg()
                        + " Kg."
        );
    }

    private boolean detectGpsLost(Truck truck) {
        if (truck.getAssignedDriver() == null) {
            return false;
        }

        if (truck.getStatus() != Truck.TruckStatus.ON_MISSION) {
            return false;
        }

        Optional<TruckLocation> latestLocation =
                truckLocationRepository.findLatestByDriverId(truck.getAssignedDriver().getId());

        boolean gpsLost;

        if (latestLocation.isEmpty()) {
            gpsLost = true;
        } else {
            Instant lastTime = latestLocation.get().getTimestamp();
            gpsLost = lastTime == null ||
                    Duration.between(lastTime, Instant.now()).toMinutes() >= GPS_TIMEOUT_MINUTES;
        }

        if (!gpsLost) {
            return false;
        }

        return createIfNotExists(
                truck,
                TruckIncident.IncidentType.GPS_LOST,
                TruckIncident.Severity.MEDIUM,
                "Incident automatique: aucune localisation reçue depuis plus de "
                        + GPS_TIMEOUT_MINUTES
                        + " minutes."
        );
    }

    private boolean createIfNotExists(
            Truck truck,
            TruckIncident.IncidentType type,
            TruckIncident.Severity severity,
            String description
    ) {
        boolean exists = truckIncidentRepository.existsByTruckAndIncidentTypeAndStatusIn(
                truck,
                type,
                List.of(
                        TruckIncident.IncidentStatus.OPEN,
                        TruckIncident.IncidentStatus.IN_PROGRESS
                )
        );

        if (exists) {
            return false;
        }

        TruckIncidentRequestDto request = new TruckIncidentRequestDto();
        request.setTruckId(truck.getId());
        request.setIncidentType(type);
        request.setSeverity(severity);
        request.setDescription(description);
        request.setStatus(TruckIncident.IncidentStatus.OPEN);
        request.setAutoDetected(true);
        request.setLat(truck.getLastKnownLat());
        request.setLng(truck.getLastKnownLng());

        truckIncidentService.createIncident(request);

        return true;
    }
}