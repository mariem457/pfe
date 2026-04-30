package com.example.demo.service;

import com.example.demo.dto.TruckDashboardItemResponse;
import com.example.demo.dto.TruckDashboardResponse;
import com.example.demo.dto.TruckLocationRequest;
import com.example.demo.dto.TruckLocationResponse;
import com.example.demo.entity.Driver;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckLocation;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.TruckLocationRepository;
import com.example.demo.repository.TruckRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class TruckLocationService {

    private final TruckLocationRepository truckLocationRepository;
    private final DriverRepository driverRepository;
    private final TruckRepository truckRepository;
    private final MissionRepository missionRepository;
    private final MissionBinRepository missionBinRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TruckLocationService(
            TruckLocationRepository truckLocationRepository,
            DriverRepository driverRepository,
            TruckRepository truckRepository,
            MissionRepository missionRepository,
            MissionBinRepository missionBinRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.truckLocationRepository = truckLocationRepository;
        this.driverRepository = driverRepository;
        this.truckRepository = truckRepository;
        this.missionRepository = missionRepository;
        this.missionBinRepository = missionBinRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public TruckLocationResponse save(TruckLocationRequest in) {
        validateInput(in);

        Driver driver = driverRepository.findById(in.driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + in.driverId));

        Truck truck = resolveActiveTruckByDriver(driver);

        TruckLocation location = new TruckLocation();
        location.setDriver(driver);
        location.setLat(in.lat);
        location.setLng(in.lng);
        location.setSpeedKmh(in.speedKmh == null ? null : BigDecimal.valueOf(in.speedKmh));
        location.setHeadingDeg(in.headingDeg == null ? null : BigDecimal.valueOf(in.headingDeg));
        location.setTimestamp(Instant.now());

        TruckLocation saved = truckLocationRepository.save(location);

        truck.setLastKnownLat(in.lat);
        truck.setLastKnownLng(in.lng);
        truck.setLastStatusUpdate(OffsetDateTime.now(ZoneOffset.UTC));
        truckRepository.save(truck);

        TruckLocationResponse resp = TruckLocationResponse.of(
                driver.getId(),
                saved.getLat(),
                saved.getLng(),
                saved.getSpeedKmh() == null ? null : saved.getSpeedKmh().doubleValue(),
                saved.getHeadingDeg() == null ? null : saved.getHeadingDeg().doubleValue(),
                saved.getTimestamp()
        );

        System.out.println(
                "WS BROADCAST => /topic/truck-locations driverId="
                        + resp.driverId
                        + ", lat=" + resp.lat
                        + ", lng=" + resp.lng
        );

        messagingTemplate.convertAndSend("/topic/truck-locations", resp);

        return resp;
    }

    @Transactional(readOnly = true)
    public TruckDashboardResponse getDashboard() {
        List<Truck> allTrucks = truckRepository.findByIsActiveTrue();
        List<TruckDashboardItemResponse> trucks = new ArrayList<>();

        int totalProgress = 0;
        int totalFuel = 0;

        for (Truck truck : allTrucks) {
            Double lat = truck.getLastKnownLat();
            Double lng = truck.getLastKnownLng();

            if (lat == null || lng == null) {
                continue;
            }

            Driver driver = truck.getAssignedDriver();

            String truckStatus = truck.getStatus() != null
                    ? truck.getStatus().name()
                    : "UNKNOWN";

            List<Mission> activeMissions = findActiveMissionsForTruck(truck);
            Long currentMissionId = activeMissions.isEmpty() ? null : activeMissions.get(0).getId();

            int collectedBins = calculateCollectedBinsFromMissions(activeMissions);
            int remainingBins = calculateRemainingBinsFromMissions(activeMissions);
            int totalBins = collectedBins + remainingBins;

            int progress = totalBins == 0
                    ? 0
                    : (int) Math.round((collectedBins * 100.0) / totalBins);

            int fuelLevel = calculateFuelPercent(truck);
            int etaMinutes = calculateEtaMinutes(truck);
            boolean active = Boolean.TRUE.equals(truck.getIsActive());

            totalProgress += progress;
            totalFuel += fuelLevel;

            trucks.add(new TruckDashboardItemResponse(
                    driver != null ? driver.getId() : null,
                    truck.getTruckCode() != null ? truck.getTruckCode() : "TRUCK-" + truck.getId(),
                    driver != null ? driver.getFullName() : "Non assigné",
                    lat,
                    lng,
                    buildLocationLabel(lat, lng),
                    progress,
                    collectedBins,
                    remainingBins,
                    fuelLevel,
                    etaMinutes,
                    active,
                    truckStatus,
                    currentMissionId
            ));
        }

        long activeTrucks = trucks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .count();

        long totalRoutes = trucks.size();

        int averageProgress = trucks.isEmpty() ? 0 : totalProgress / trucks.size();
        int averageFuel = trucks.isEmpty() ? 0 : totalFuel / trucks.size();

        String fuelStatus;
        if (averageFuel >= 70) {
            fuelStatus = "Bon";
        } else if (averageFuel >= 40) {
            fuelStatus = "Moyen";
        } else {
            fuelStatus = "Faible";
        }

        return new TruckDashboardResponse(
                activeTrucks,
                totalRoutes,
                averageProgress,
                fuelStatus,
                trucks
        );
    }

    private Truck resolveActiveTruckByDriver(Driver driver) {
        return truckRepository.findByIsActiveTrue()
                .stream()
                .filter(truck -> truck.getAssignedDriver() != null)
                .filter(truck -> truck.getAssignedDriver().getId().equals(driver.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No active truck assigned to driver: " + driver.getId()
                ));
    }

    private List<Mission> findActiveMissionsForTruck(Truck truck) {
        return missionRepository
                .findTopByTruckAndStatusInOrderByCreatedAtDesc(
                        truck,
                        List.of("IN_PROGRESS", "STARTED", "CREATED")
                )
                .map(List::of)
                .orElse(List.of());
    }

    private int calculateCollectedBinsFromMissions(List<Mission> missions) {
        int count = 0;

        for (Mission mission : missions) {
            count += missionBinRepository.findByMissionOrderByVisitOrderAsc(mission)
                    .stream()
                    .filter(MissionBin::isCollected)
                    .count();
        }

        return count;
    }

    private int calculateRemainingBinsFromMissions(List<Mission> missions) {
        int count = 0;

        for (Mission mission : missions) {
            count += missionBinRepository.findByMissionOrderByVisitOrderAsc(mission)
                    .stream()
                    .filter(mb -> !mb.isCollected())
                    .count();
        }

        return count;
    }

    private int calculateEtaMinutes(Truck truck) {
        if (truck.getRoutePlans() == null || truck.getRoutePlans().isEmpty()) {
            return 0;
        }

        return truck.getRoutePlans()
                .stream()
                .filter(rp -> rp.getPlanStatus() != null)
                .filter(rp -> "PLANNED".equals(rp.getPlanStatus().name())
                        || "ACTIVE".equals(rp.getPlanStatus().name()))
                .map(rp -> rp.getEstimatedDurationMin() != null ? rp.getEstimatedDurationMin() : 0)
                .findFirst()
                .orElse(0);
    }

    private int calculateFuelPercent(Truck truck) {
        if (truck.getFuelLevelLiters() == null || truck.getTankCapacityLiters() == null) {
            return 0;
        }

        if (truck.getTankCapacityLiters().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return truck.getFuelLevelLiters()
                .multiply(BigDecimal.valueOf(100))
                .divide(truck.getTankCapacityLiters(), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String buildLocationLabel(Double lat, Double lng) {
        return "Lat " + String.format("%.4f", lat) + ", Lng " + String.format("%.4f", lng);
    }

    private void validateInput(TruckLocationRequest in) {
        if (in == null) {
            throw new RuntimeException("TruckLocationRequest is required");
        }

        if (in.driverId == null) {
            throw new RuntimeException("driverId is required");
        }

        if (in.lat == null || in.lng == null) {
            throw new RuntimeException("lat/lng are required");
        }

        if (in.lat < -90 || in.lat > 90) {
            throw new RuntimeException("lat must be between -90 and 90");
        }

        if (in.lng < -180 || in.lng > 180) {
            throw new RuntimeException("lng must be between -180 and 180");
        }

        if (in.speedKmh != null && in.speedKmh < 0) {
            throw new RuntimeException("speedKmh must be >= 0");
        }
    }
}