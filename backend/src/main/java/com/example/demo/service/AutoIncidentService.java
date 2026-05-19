package com.example.demo.service;

import com.example.demo.dto.AutoIncidentRunResponse;
import com.example.demo.entity.Mission;
import com.example.demo.dto.TruckIncidentRequestDto;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckIncident;
import com.example.demo.entity.TruckLocation;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.TruckIncidentRepository;
import com.example.demo.repository.TruckLocationRepository;
import com.example.demo.repository.TruckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AutoIncidentService {

    private static final int FUEL_LOW_PERCENT = 20;
    private static final int GPS_TIMEOUT_MINUTES = 2;
    private static final int FUEL_RESOLVED_PERCENT = 25;
    private static final int GPS_RECOVERED_MINUTES = 2;

    private final TruckRepository truckRepository;
    private final TruckLocationRepository truckLocationRepository;
    private final TruckIncidentRepository truckIncidentRepository;
    private final TruckIncidentService truckIncidentService;
    private final SmartAlertService smartAlertService;
    private final MissionRepository missionRepository;
    private final EmailService emailService;

    public AutoIncidentService(
            TruckRepository truckRepository,
            TruckLocationRepository truckLocationRepository,
            TruckIncidentRepository truckIncidentRepository,
            TruckIncidentService truckIncidentService,
            SmartAlertService smartAlertService,
            MissionRepository missionRepository,
            EmailService emailService
    ) {
        this.truckRepository = truckRepository;
        this.truckLocationRepository = truckLocationRepository;
        this.truckIncidentRepository = truckIncidentRepository;
        this.truckIncidentService = truckIncidentService;
        this.smartAlertService = smartAlertService;
        this.missionRepository = missionRepository;
        this.emailService = emailService;
    }

    @Transactional
    public AutoIncidentRunResponse runAutoDetection() {
        List<Truck> trucks = truckRepository.findByIsActiveTrue();

        int created = 0;

        for (Truck truck : trucks) {
           

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
    public int autoResolveIncidents() {
        List<TruckIncident> openIncidents = truckIncidentRepository.findByStatusIn(
                List.of(
                        TruckIncident.IncidentStatus.OPEN,
                        TruckIncident.IncidentStatus.IN_PROGRESS
                )
        );

        int resolved = 0;

        for (TruckIncident incident : openIncidents) {
            if (incident == null || incident.getTruck() == null || incident.getIncidentType() == null) {
                continue;
            }

            if (!shouldAutoResolve(incident)) {
                continue;
            }

            incident.setStatus(TruckIncident.IncidentStatus.RESOLVED);
            incident.setResolvedAt(OffsetDateTime.now());

            TruckIncident saved = truckIncidentRepository.save(incident);

            smartAlertService.resolveAlertsByIncident(saved.getId());

            restoreTruckStatusIfPossible(saved.getTruck());

            resolved++;

            System.out.println(
                    "AUTO INCIDENT RESOLVED => truck="
                            + saved.getTruck().getTruckCode()
                            + ", type="
                            + saved.getIncidentType()
                            + ", incidentId="
                            + saved.getId()
            );
        }

        return resolved;
    }

    private boolean shouldAutoResolve(TruckIncident incident) {
        return switch (incident.getIncidentType()) {
            case FUEL_LOW -> isFuelRecovered(incident.getTruck());
            case GPS_LOST -> isGpsRecovered(incident.getTruck());
            case OVERLOAD -> isOverloadRecovered(incident.getTruck());
            default -> false;
        };
    }

    private boolean isFuelRecovered(Truck truck) {
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

        return fuelPercent >= FUEL_RESOLVED_PERCENT;
    }

    private boolean isGpsRecovered(Truck truck) {
        if (truck.getAssignedDriver() == null) {
            return false;
        }

        Optional<TruckLocation> latestLocation =
                truckLocationRepository.findLatestByDriverId(truck.getAssignedDriver().getId());

        if (latestLocation.isEmpty()) {
            return false;
        }

        Instant lastTime = latestLocation.get().getTimestamp();

        return lastTime != null
                && Duration.between(lastTime, Instant.now()).toMinutes() < GPS_RECOVERED_MINUTES;
    }

    private boolean isOverloadRecovered(Truck truck) {
        if (truck.getCurrentLoadKg() == null || truck.getMaxLoadKg() == null) {
            return false;
        }

        return truck.getCurrentLoadKg().compareTo(truck.getMaxLoadKg()) <= 0;
    }

    private void restoreTruckStatusIfPossible(Truck truck) {
        if (truck == null || truck.getId() == null) {
            return;
        }

        boolean hasOtherOpenIncidents = truckIncidentRepository
                .findByTruckAndStatus(truck, TruckIncident.IncidentStatus.OPEN)
                .stream()
                .anyMatch(i -> i.getStatus() == TruckIncident.IncidentStatus.OPEN);

        if (hasOtherOpenIncidents) {
            return;
        }

        Mission activeMission = missionRepository
                .findTopByTruckAndStatusInOrderByCreatedAtDesc(
                        truck,
                        List.of("IN_PROGRESS", "CREATED", "PLANNED")
                )
                .orElse(null);

        if (activeMission != null) {
            truck.setStatus(Truck.TruckStatus.ON_MISSION);
        } else {
            truck.setStatus(Truck.TruckStatus.AVAILABLE);
        }

        truckRepository.save(truck);
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

        TruckIncident incident = new TruckIncident();
        incident.setTruck(truck);
        incident.setIncidentType(type);
        incident.setSeverity(severity);
        incident.setDescription(description);
        incident.setStatus(TruckIncident.IncidentStatus.OPEN);
        incident.setAutoDetected(true);
        incident.setLat(truck.getLastKnownLat());
        incident.setLng(truck.getLastKnownLng());

        Mission activeMission = missionRepository
                .findTopByTruckAndStatusInOrderByCreatedAtDesc(
                        truck,
                        List.of("IN_PROGRESS", "CREATED", "PLANNED")
                )
                .orElse(null);

        if (activeMission != null) {
            incident.setMission(activeMission);
        }

        TruckIncident saved = truckIncidentRepository.save(incident);
        truckIncidentRepository.flush();

        // Pour les incidents automatiques, on garde le camion en mission.
        // L'incident est affiché comme alerte, mais le camion reste visible dans "Missions en cours".
        if (type == TruckIncident.IncidentType.BREAKDOWN) {
            truck.setStatus(Truck.TruckStatus.BREAKDOWN);
        } else if (type == TruckIncident.IncidentType.OVERLOAD) {
            truck.setStatus(Truck.TruckStatus.UNAVAILABLE);
        } else if (truck.getStatus() != Truck.TruckStatus.ON_MISSION) {
            // FUEL_LOW / GPS_LOST ne doivent pas sortir le camion de la mission dans le suivi flotte
            truck.setStatus(Truck.TruckStatus.ON_MISSION);
        }

        truckRepository.save(truck);

        smartAlertService.createTruckIncidentAlert(saved);
        sendGpsLostEmailIfNeeded(saved);

        System.out.println(
                "AUTO INCIDENT CREATED => truck="
                        + truck.getTruckCode()
                        + ", type="
                        + type
                        + ", incidentId="
                        + saved.getId()
        );

        return true;
    }

    private void sendGpsLostEmailIfNeeded(TruckIncident incident) {
        if (incident == null || incident.getIncidentType() != TruckIncident.IncidentType.GPS_LOST) {
            return;
        }

        Truck truck = incident.getTruck();
        if (truck == null || truck.getAssignedDriver() == null || truck.getAssignedDriver().getUser() == null) {
            return;
        }

        String email = truck.getAssignedDriver().getUser().getEmail();
        if (email == null || email.isBlank()) {
            return;
        }

        try {
            emailService.sendGpsLostEmail(
                    email,
                    truck.getAssignedDriver().getFullName(),
                    truck.getTruckCode(),
                    GPS_TIMEOUT_MINUTES
            );
        } catch (Exception e) {
            System.err.println("GPS_LOST EMAIL ERROR => truckId="
                    + truck.getId()
                    + ", driverId="
                    + truck.getAssignedDriver().getId()
                    + ", error="
                    + e.getMessage());
        }
    }
}
