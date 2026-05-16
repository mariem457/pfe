package com.example.demo.service;

import com.example.demo.dto.AlertResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MissionRealtimeService missionRealtimeService;

    public AlertRealtimeService(
            SimpMessagingTemplate messagingTemplate,
            MissionRealtimeService missionRealtimeService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.missionRealtimeService = missionRealtimeService;
    }

    public void publishCreated(AlertResponse alert) {
        if (alert == null) return;

        messagingTemplate.convertAndSend("/topic/alerts", alert);

        if (alert.getMissionId() != null) {
            missionRealtimeService.publishMissionAlertCreated(alert.getMissionId(), alert.getId());
        }
    }

    public void publishResolved(AlertResponse alert) {
        if (alert == null) return;

        messagingTemplate.convertAndSend("/topic/alerts/resolved", alert);

        if (alert.getMissionId() != null) {
            missionRealtimeService.publishMissionAlertResolved(alert.getMissionId(), alert.getId());
        }
    }
}