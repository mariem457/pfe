package com.example.demo.service;

import com.example.demo.dto.AlertResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public AlertRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishCreated(AlertResponse alert) {
        if (alert == null) return;
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }

    public void publishResolved(AlertResponse alert) {
        if (alert == null) return;
        messagingTemplate.convertAndSend("/topic/alerts/resolved", alert);
    }
}