package com.example.demo.service;

import com.example.demo.dto.TruckDashboardItemResponse;
import com.example.demo.dto.TruckDashboardResponse;
import com.example.demo.dto.TruckLocationRequest;
import com.example.demo.dto.TruckLocationResponse;
import com.example.demo.entity.Driver;
import com.example.demo.entity.TruckLocation;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.TruckLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TruckLocationService {

    private static final Logger log = LoggerFactory.getLogger(TruckLocationService.class);

    private final TruckLocationRepository truckLocationRepository;
    private final DriverRepository driverRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TruckLocationService(
            TruckLocationRepository truckLocationRepository,
            DriverRepository driverRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.truckLocationRepository = truckLocationRepository;
        this.driverRepository = driverRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public TruckLocationResponse save(TruckLocationRequest in) {
        validateInput(in);

        log.info("Truck location received: driverId={}, lat={}, lng={}, speed={}, heading={}",
                in.driverId, in.lat, in.lng, in.speedKmh, in.headingDeg);

        Driver driver = driverRepository.findById(in.driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + in.driverId));

        TruckLocation location = new TruckLocation();
        location.setDriver(driver);
        location.setLat(in.lat);
        location.setLng(in.lng);
        location.setSpeedKmh(in.speedKmh == null ? null : BigDecimal.valueOf(in.speedKmh));
        location.setHeadingDeg(in.headingDeg == null ? null : BigDecimal.valueOf(in.headingDeg));
        location.setTimestamp(Instant.now());

        TruckLocation saved = truckLocationRepository.save(location);

        TruckLocationResponse resp = TruckLocationResponse.of(
                driver.getId(),
                saved.getLat(),
                saved.getLng(),
                saved.getSpeedKmh() == null ? null : saved.getSpeedKmh().doubleValue(),
                saved.getHeadingDeg() == null ? null : saved.getHeadingDeg().doubleValue(),
                saved.getTimestamp()
        );

        log.info("Broadcasting truck location to /topic/truck-locations: driverId={}, lat={}, lng={}",
                resp.driverId, resp.lat, resp.lng);

        messagingTemplate.convertAndSend("/topic/truck-locations", resp);

        return resp;
    }
    @Transactional(readOnly = true)
    public TruckDashboardResponse getDashboard() {
        List<TruckLocation> latestLocations = truckLocationRepository.findLatestLocationsForAllDrivers();
        List<TruckDashboardItemResponse> trucks = new ArrayList<>();

        int totalProgress = 0;
        int totalFuel = 0;

        for (int i = 0; i < latestLocations.size(); i++) {
            TruckLocation loc = latestLocations.get(i);
            Driver driver = loc.getDriver();

            int progress = buildProgress(driver.getId());
            int collectedBins = buildCollectedBins(driver.getId());
            int remainingBins = buildRemainingBins(driver.getId());
            int fuelLevel = buildFuelLevel(driver.getId());
            int etaMinutes = buildEta(driver.getId());
            boolean active = isActive(loc.getTimestamp());

            totalProgress += progress;
            totalFuel += fuelLevel;

            trucks.add(new TruckDashboardItemResponse(
                    driver.getId(),
                    driver.getVehicleCode() != null ? driver.getVehicleCode() : "TRK-" + driver.getId(),
                    driver.getFullName(),
                    loc.getLat(),
                    loc.getLng(),
                    buildLocationLabel(loc.getLat(), loc.getLng()),
                    progress,
                    collectedBins,
                    remainingBins,
                    fuelLevel,
                    etaMinutes,
                    active
            ));
        }

        long activeTrucks = trucks.stream().filter(t -> Boolean.TRUE.equals(t.getActive())).count();
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

    private boolean isActive(Instant timestamp) {
        return timestamp != null && timestamp.isAfter(Instant.now().minusSeconds(600));
    }

    private int buildProgress(Long driverId) {
        return 35 + (int) ((driverId * 13) % 55);
    }

    private int buildCollectedBins(Long driverId) {
        return 8 + (int) ((driverId * 5) % 15);
    }

    private int buildRemainingBins(Long driverId) {
        return 4 + (int) ((driverId * 3) % 12);
    }

    private int buildFuelLevel(Long driverId) {
        return 45 + (int) ((driverId * 7) % 45);
    }

    private int buildEta(Long driverId) {
        return 15 + (int) ((driverId * 11) % 40);
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