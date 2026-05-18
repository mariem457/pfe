package com.example.demo.service;

import com.example.demo.dto.MissionRealtimeEvent;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.repository.MissionBinRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MissionRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MissionBinRepository missionBinRepository;

    public MissionRealtimeService(
            SimpMessagingTemplate messagingTemplate,
            MissionBinRepository missionBinRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.missionBinRepository = missionBinRepository;
    }

    public void publishMissionStatusChanged(Mission mission) {
        if (mission == null || mission.getId() == null) return;

        MissionRealtimeEvent event = MissionRealtimeEvent.of("MISSION_STATUS_CHANGED", mission.getId());
        event.setStatus(mission.getStatus());
        event.setMissionStatusDetail(
                mission.getMissionStatusDetail() != null
                        ? mission.getMissionStatusDetail().name()
                        : null
        );
        event.setMessage("Mission status updated");

        publish(event);
    }

    public void publishMissionCompleted(Mission mission) {
        if (mission == null || mission.getId() == null) return;

        MissionRealtimeEvent event = MissionRealtimeEvent.of("MISSION_COMPLETED", mission.getId());
        event.setStatus(mission.getStatus());
        event.setMissionStatusDetail(
                mission.getMissionStatusDetail() != null
                        ? mission.getMissionStatusDetail().name()
                        : null
        );
        event.setMessage("Mission completed");

        publish(event);
    }

    public void publishMissionCancelled(Mission mission) {
        if (mission == null || mission.getId() == null) return;

        MissionRealtimeEvent event = MissionRealtimeEvent.of("MISSION_CANCELLED", mission.getId());
        event.setStatus(mission.getStatus());
        event.setMissionStatusDetail(
                mission.getMissionStatusDetail() != null
                        ? mission.getMissionStatusDetail().name()
                        : null
        );
        event.setMessage("Mission cancelled");

        publish(event);
    }

    public void publishMissionBinUpdated(MissionBin missionBin, String type) {
        if (missionBin == null || missionBin.getMission() == null) return;

        Mission mission = missionBin.getMission();

        MissionRealtimeEvent event = MissionRealtimeEvent.of(type, mission.getId());
        event.setMissionBinId(missionBin.getId());

        if (missionBin.getBin() != null) {
            event.setBinId(missionBin.getBin().getId());
            event.setBinCode(missionBin.getBin().getBinCode());
        }

        event.setStatus(mission.getStatus());
        event.setMissionStatusDetail(
                mission.getMissionStatusDetail() != null
                        ? mission.getMissionStatusDetail().name()
                        : null
        );

        attachProgress(event, mission);

        publish(event);
    }

    public void publishMissionReplanned(Mission mission, Long newMissionId) {
        if (mission == null || mission.getId() == null) return;

        MissionRealtimeEvent event = MissionRealtimeEvent.of("MISSION_REPLANNED", mission.getId());
        event.setOldMissionId(mission.getId());
        event.setNewMissionId(newMissionId);
        event.setStatus(mission.getStatus());
        event.setMissionStatusDetail(
                mission.getMissionStatusDetail() != null
                        ? mission.getMissionStatusDetail().name()
                        : null
        );
        event.setMessage("Mission replanned");

        publish(event);
    }

    public void publishMissionPartiallyReassigned(Mission mission, Long newMissionId) {
        if (mission == null || mission.getId() == null) return;

        MissionRealtimeEvent event = MissionRealtimeEvent.of("MISSION_PARTIALLY_REASSIGNED", mission.getId());
        event.setOldMissionId(mission.getId());
        event.setNewMissionId(newMissionId);
        event.setStatus(mission.getStatus());
        event.setMissionStatusDetail(
                mission.getMissionStatusDetail() != null
                        ? mission.getMissionStatusDetail().name()
                        : null
        );
        event.setMessage("Mission partially reassigned");

        publish(event);
    }

    public void publishMissionAlertCreated(Long missionId, Long alertId) {
        if (missionId == null) return;

        MissionRealtimeEvent event = MissionRealtimeEvent.of("MISSION_ALERT_CREATED", missionId);
        event.setAlertId(alertId);
        event.setMessage("New alert linked to mission");

        publish(event);
    }

    public void publishMissionAlertResolved(Long missionId, Long alertId) {
        if (missionId == null) return;

        MissionRealtimeEvent event = MissionRealtimeEvent.of("MISSION_ALERT_RESOLVED", missionId);
        event.setAlertId(alertId);
        event.setMessage("Mission alert resolved");

        publish(event);
    }

    private void attachProgress(MissionRealtimeEvent event, Mission mission) {
        if (event == null || mission == null || mission.getId() == null) return;

        List<MissionBin> bins = missionBinRepository.findByMissionOrderByVisitOrderAsc(mission);

        int total = bins.size();
        int collected = (int) bins.stream().filter(MissionBin::isCollected).count();

        event.setTotalBins(total);
        event.setCollectedCount(collected);
    }

    private void publish(MissionRealtimeEvent event) {
        messagingTemplate.convertAndSend("/topic/missions", event);
    }
}
