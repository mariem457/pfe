package com.example.demo.service;

import com.example.demo.entity.Mission;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.MissionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class MissionAlertRuleService {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Tunis");

    private final MissionRepository missionRepository;
    private final MissionBinRepository missionBinRepository;
    private final SmartAlertService smartAlertService;

    public MissionAlertRuleService(
            MissionRepository missionRepository,
            MissionBinRepository missionBinRepository,
            SmartAlertService smartAlertService
    ) {
        this.missionRepository = missionRepository;
        this.missionBinRepository = missionBinRepository;
        this.smartAlertService = smartAlertService;
    }

    @Scheduled(fixedRate = 60000)
    public void scanMissionAlerts() {
        checkNotStartedMissions();
        checkStuckMissions();
    }

    private void checkNotStartedMissions() {
        LocalDate today = LocalDate.now(APP_ZONE);

        List<Mission> createdMissions = missionRepository.findByStatusOrderByPlannedDateAsc("CREATED");

        for (Mission mission : createdMissions) {
            if (mission.getPlannedDate() == null) continue;

            if (mission.getPlannedDate().isBefore(today)) {
                smartAlertService.createMissionAlert(
                        mission,
                        "MISSION_NOT_STARTED",
                        "HIGH",
                        "Mission non démarrée",
                        "La mission " + mission.getMissionCode() + " était prévue le "
                                + mission.getPlannedDate() + " mais elle n'a pas encore démarré.",
                        "Démarrer la mission, vérifier le chauffeur ou replanifier la tournée.",
                        "REPLAN"
                );
            }
        }
    }

    private void checkStuckMissions() {
        List<Mission> inProgressMissions = missionRepository.findByStatusOrderByPlannedDateAsc("IN_PROGRESS");

        Instant now = Instant.now();

        for (Mission mission : inProgressMissions) {
            if (mission.getStartedAt() == null) continue;

            long minutesSinceStart = java.time.Duration.between(mission.getStartedAt(), now).toMinutes();

            long totalBins = missionBinRepository.countByMissionId(mission.getId());
            long collectedBins = missionBinRepository.countByMissionIdAndCollectedTrue(mission.getId());

            if (totalBins > 0 && collectedBins == 0 && minutesSinceStart >= 30) {
                smartAlertService.createMissionAlert(
                        mission,
                        "MISSION_STUCK",
                        "MEDIUM",
                        "Mission bloquée",
                        "La mission " + mission.getMissionCode()
                                + " est en cours depuis plus de 30 minutes sans aucun bac collecté.",
                        "Contacter le chauffeur et vérifier l'avancement réel de la mission.",
                        "CALL_DRIVER"
                );
            }
        }
    }
}