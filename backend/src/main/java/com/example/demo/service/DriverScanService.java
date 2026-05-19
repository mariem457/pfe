package com.example.demo.service;
import com.example.demo.entity.Mission;
import com.example.demo.entity.Truck;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.TruckRepository;

import java.util.List;
import com.example.demo.dto.BinScanRequest;
import com.example.demo.entity.Bin;
import com.example.demo.entity.Driver;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.User;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinTelemetryRepository;
import java.math.BigDecimal;
@Service
public class DriverScanService {

    private final BinRepository binRepository;
    private final DriverRepository driverRepository;
    private final MissionBinRepository missionBinRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final TruckRepository truckRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    private final MissionRealtimeService missionRealtimeService;
    private final SmartAlertService smartAlertService;

    public DriverScanService(
            BinRepository binRepository,
            DriverRepository driverRepository,
            MissionBinRepository missionBinRepository,
            UserRepository userRepository,
            MissionRepository missionRepository,
            TruckRepository truckRepository,
            BinTelemetryRepository binTelemetryRepository,
            MissionRealtimeService missionRealtimeService,
            SmartAlertService smartAlertService
    ) {
        this.binRepository = binRepository;
        this.driverRepository = driverRepository;
        this.missionBinRepository = missionBinRepository;
        this.userRepository = userRepository;
        this.missionRepository = missionRepository;
        this.truckRepository = truckRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.missionRealtimeService = missionRealtimeService;
        this.smartAlertService = smartAlertService;
    }

    @Transactional
    public MissionBin scanAndCollect(String scannedCode, String usernameOrEmail, BinScanRequest request) {
        String cleanCode = scannedCode == null ? null : scannedCode.trim();
        System.out.println("BIN CODE RECU = [" + cleanCode + "]");

        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver introuvable pour cet utilisateur"));

        OffsetDateTime nowOffset = OffsetDateTime.now();
        Instant nowInstant = Instant.now();

        String issueType = normalizeBinIssueType(request.getIssueType());
        boolean collectedAfterIssue = Boolean.TRUE.equals(request.getCollectedAfterIssue());
        MissionBin missionBin;

        if (issueType != null && !issueType.isBlank() && request.getMissionBinId() != null) {
            missionBin = findDriverMissionBinById(request.getMissionBinId(), driver.getId());
        } else if (collectedAfterIssue && request.getMissionBinId() != null) {
            missionBin = findDriverMissionBinById(request.getMissionBinId(), driver.getId(), true);
        } else {
            Bin bin = binRepository.findByBinCode(cleanCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Poubelle introuvable pour ce code : " + cleanCode));

            missionBin = findPlannedMissionBin(bin.getBinCode(), driver.getId());
        }


        // cas problème
        if (issueType != null && !issueType.isBlank()) {
            missionBin.setCollected(false);
            missionBin.setCollectedAt(null);
            missionBin.setCollectedBy(null);
            missionBin.setActualArrival(nowOffset);
            missionBin.setIssueType(issueType);
            missionBin.setDriverNote(request.getDriverNote());
            missionBin.setAssignmentStatus(MissionBin.AssignmentStatus.SKIPPED);
            missionBin.setSkippedReason(issueType);

            MissionBin saved = missionBinRepository.save(missionBin);

            syncMissionStatusAfterDriverAction(saved.getMission());
            smartAlertService.createDriverBinIssueAlert(saved);
            missionRealtimeService.publishMissionBinUpdated(saved, "MISSION_BIN_SKIPPED");
            missionRealtimeService.publishMissionStatusChanged(saved.getMission());

            return saved;
        }

        // cas normal
        missionBin.setCollected(true);
        missionBin.setCollectedAt(nowInstant);
        missionBin.setCollectedBy(driver);
        missionBin.setActualArrival(nowOffset);
        if (!collectedAfterIssue) {
            missionBin.setIssueType(null);
        }
        missionBin.setDriverNote(request.getDriverNote());
        missionBin.setAssignmentStatus(MissionBin.AssignmentStatus.COLLECTED);
        if (!collectedAfterIssue) {
            missionBin.setSkippedReason(null);
        }

        MissionBin saved = missionBinRepository.save(missionBin);

        updateTruckLoadAfterBinCollection(saved);

        syncMissionStatusAfterDriverAction(saved.getMission());
        missionRealtimeService.publishMissionBinUpdated(saved, "MISSION_BIN_COLLECTED");
        missionRealtimeService.publishMissionStatusChanged(saved.getMission());

        return saved;
    }
    
    
    private void updateTruckLoadAfterBinCollection(MissionBin missionBin) {
        if (missionBin == null || missionBin.getMission() == null) {
            return;
        }

        Mission mission = missionBin.getMission();
        Truck truck = mission.getTruck();

        if (truck == null) {
            System.out.println("TRUCK LOAD SKIPPED => mission has no truck");
            return;
        }

        BigDecimal currentLoad = truck.getCurrentLoadKg() != null
                ? truck.getCurrentLoadKg()
                : BigDecimal.ZERO;

        BigDecimal binWeight = BigDecimal.ZERO;

        if (missionBin.getBin() != null && missionBin.getBin().getId() != null) {
            binWeight = binTelemetryRepository
                    .findTopByBinIdOrderByTimestampDesc(missionBin.getBin().getId())
                    .map(BinTelemetry::getWeightKg)
                    .filter(weight -> weight != null)
                    .orElse(BigDecimal.ZERO);
        }

        BigDecimal maxLoad = truck.getMaxLoadKg() != null
                ? truck.getMaxLoadKg()
                : BigDecimal.ZERO;

        BigDecimal newLoad = currentLoad.add(binWeight);

        if (maxLoad.compareTo(BigDecimal.ZERO) > 0 && newLoad.compareTo(maxLoad) > 0) {
            throw new ConflictException(
                    "Capacité camion dépassée: charge actuelle="
                            + currentLoad + "kg, bac="
                            + binWeight + "kg, capacité="
                            + maxLoad + "kg"
            );
        }

        truck.setCurrentLoadKg(newLoad);
        truck.setLastStatusUpdate(OffsetDateTime.now());
        truckRepository.save(truck);

        System.out.println(
                "TRUCK LOAD UPDATED FROM DRIVER SCAN => truckId=" + truck.getId()
                        + " | binId=" + (missionBin.getBin() != null ? missionBin.getBin().getId() : null)
                        + " | added=" + binWeight
                        + "kg | newLoad=" + newLoad
        );
    }
    
    private void syncMissionStatusAfterDriverAction(Mission mission) {
        if (mission == null || mission.getId() == null) {
            return;
        }

        List<MissionBin> allBins = missionBinRepository.findByMissionOrderByVisitOrderAsc(mission);

        boolean hasCollected = allBins.stream().anyMatch(MissionBin::isCollected);

        boolean hasRemaining = allBins.stream()
                .anyMatch(mb -> !mb.isCollected()
                        && mb.getAssignmentStatus() != MissionBin.AssignmentStatus.REASSIGNED
                        && mb.getAssignmentStatus() != MissionBin.AssignmentStatus.SKIPPED);

        if (hasCollected && hasRemaining) {
            mission.setStatus("IN_PROGRESS");
            mission.setMissionStatusDetail(Mission.MissionStatusDetail.IN_PROGRESS);

            if (mission.getStartedAt() == null) {
                mission.setStartedAt(Instant.now());
            }

            Truck truck = mission.getTruck();

            if (truck != null) {
                truck.setStatus(Truck.TruckStatus.ON_MISSION);
                truck.setLastStatusUpdate(OffsetDateTime.now());
                truckRepository.save(truck);
            }

            missionRepository.save(mission);
            return;
        }

        if (hasCollected && !hasRemaining) {
            mission.setStatus("COMPLETED");
            mission.setMissionStatusDetail(Mission.MissionStatusDetail.COMPLETED);

            if (mission.getStartedAt() == null) {
                mission.setStartedAt(Instant.now());
            }

            if (mission.getCompletedAt() == null) {
                mission.setCompletedAt(Instant.now());
            }

            Truck truck = mission.getTruck();

            if (truck != null) {
                truck.setStatus(Truck.TruckStatus.AVAILABLE);
                truck.setCurrentLoadKg(java.math.BigDecimal.ZERO);
                truck.setLastStatusUpdate(OffsetDateTime.now());
                truckRepository.save(truck);
            }

            missionRepository.save(mission);
        }
    }

    private MissionBin findPlannedMissionBin(String binCode, Long driverId) {
        Optional<MissionBin> planned = missionBinRepository
                .findFirstByBinBinCodeAndMissionDriverIdAndMissionStatusAndAssignmentStatusOrderByVisitOrderAsc(
                        binCode,
                        driverId,
                        "IN_PROGRESS",
                        MissionBin.AssignmentStatus.PLANNED
                );

        if (planned.isPresent()) {
            return planned.get();
        }

        planned = missionBinRepository
                .findFirstByBinBinCodeAndMissionDriverIdAndMissionStatusAndAssignmentStatusOrderByVisitOrderAsc(
                        binCode,
                        driverId,
                        "CREATED",
                        MissionBin.AssignmentStatus.PLANNED
                );

        if (planned.isPresent()) {
            return planned.get();
        }

        Optional<MissionBin> existing = missionBinRepository
                .findFirstByBinBinCodeAndMissionDriverIdAndMissionStatusOrderByVisitOrderAsc(
                        binCode,
                        driverId,
                        "IN_PROGRESS"
                );

        if (existing.isEmpty()) {
            existing = missionBinRepository
                    .findFirstByBinBinCodeAndMissionDriverIdAndMissionStatusOrderByVisitOrderAsc(
                            binCode,
                            driverId,
                            "CREATED"
                    );
        }

        if (existing.isPresent()) {
            MissionBin.AssignmentStatus status = existing.get().getAssignmentStatus();

            if (status == MissionBin.AssignmentStatus.COLLECTED) {
                throw new ConflictException("Cette poubelle a deja ete collectee");
            }

            if (status == MissionBin.AssignmentStatus.SKIPPED) {
                throw new ConflictException("Cette poubelle a deja ete signalee comme probleme");
            }

            throw new ConflictException("Cette poubelle a deja ete traitee");
        }

        throw new ResourceNotFoundException("Aucune mission active pour cette poubelle et ce chauffeur");
    }

    private MissionBin findDriverMissionBinById(Long missionBinId, Long driverId) {
        return findDriverMissionBinById(missionBinId, driverId, false);
    }

    private MissionBin findDriverMissionBinById(Long missionBinId, Long driverId, boolean allowSkipped) {
        MissionBin missionBin = missionBinRepository.findById(missionBinId)
                .orElseThrow(() -> new ResourceNotFoundException("Poubelle de mission introuvable : " + missionBinId));

        Mission mission = missionBin.getMission();
        if (mission == null || mission.getDriver() == null || !driverId.equals(mission.getDriver().getId())) {
            throw new ResourceNotFoundException("Cette poubelle n'appartient pas a ce chauffeur");
        }

        String status = mission.getStatus();
        if (!"CREATED".equals(status) && !"IN_PROGRESS".equals(status)) {
            throw new ConflictException("Aucune mission active pour cette poubelle");
        }

        MissionBin.AssignmentStatus assignmentStatus = missionBin.getAssignmentStatus();
        if (assignmentStatus == MissionBin.AssignmentStatus.COLLECTED) {
            throw new ConflictException("Cette poubelle a deja ete collectee");
        }

        if (!allowSkipped && assignmentStatus == MissionBin.AssignmentStatus.SKIPPED) {
            throw new ConflictException("Cette poubelle a deja ete signalee comme probleme");
        }

        return missionBin;
    }

    private String normalizeBinIssueType(String issueType) {
        if (issueType == null || issueType.trim().isBlank()) {
            return null;
        }

        String normalized = issueType.trim().toUpperCase();

        if (
                "BLOCKED".equals(normalized)
                        || "DAMAGED".equals(normalized)
                        || "SENSOR_ERROR".equals(normalized)
                        || "OTHER".equals(normalized)
        ) {
            return normalized;
        }

        String lower = issueType.trim().toLowerCase();
        if (lower.contains("bloqu") || lower.contains("acces") || lower.contains("accès")) {
            return "BLOCKED";
        }

        if (lower.contains("endommag") || lower.contains("panne")) {
            return "DAMAGED";
        }

        if (lower.contains("capteur")) {
            return "SENSOR_ERROR";
        }

        return "OTHER";
    }
}
