package com.example.demo.service;

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

@Service
public class DriverScanService {

    private final BinRepository binRepository;
    private final DriverRepository driverRepository;
    private final MissionBinRepository missionBinRepository;
    private final UserRepository userRepository;

    public DriverScanService(
            BinRepository binRepository,
            DriverRepository driverRepository,
            MissionBinRepository missionBinRepository,
            UserRepository userRepository
    ) {
        this.binRepository = binRepository;
        this.driverRepository = driverRepository;
        this.missionBinRepository = missionBinRepository;
        this.userRepository = userRepository;
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

        Bin bin = binRepository.findByBinCode(cleanCode)
                .orElseThrow(() -> new ResourceNotFoundException("Poubelle introuvable pour ce code : " + cleanCode));

        MissionBin missionBin = findPlannedMissionBin(bin.getBinCode(), driver.getId());

        OffsetDateTime nowOffset = OffsetDateTime.now();
        Instant nowInstant = Instant.now();

        String issueType = request.getIssueType() == null ? null : request.getIssueType().trim();

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

            return missionBinRepository.save(missionBin);
        }

        // cas normal
        missionBin.setCollected(true);
        missionBin.setCollectedAt(nowInstant);
        missionBin.setCollectedBy(driver);
        missionBin.setActualArrival(nowOffset);
        missionBin.setIssueType(null);
        missionBin.setDriverNote(request.getDriverNote());
        missionBin.setAssignmentStatus(MissionBin.AssignmentStatus.COLLECTED);
        missionBin.setSkippedReason(null);

        return missionBinRepository.save(missionBin);
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
}