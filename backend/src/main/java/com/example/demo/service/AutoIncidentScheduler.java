package com.example.demo.service;

import com.example.demo.dto.AutoIncidentRunResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoIncidentScheduler {

    private final AutoIncidentService autoIncidentService;

    public AutoIncidentScheduler(AutoIncidentService autoIncidentService) {
        this.autoIncidentService = autoIncidentService;
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 30000)
    public void runAutomaticIncidentDetection() {
        try {
            AutoIncidentRunResponse result = autoIncidentService.runAutoDetection();
            int resolved = autoIncidentService.autoResolveIncidents();

            if (result.getCreatedIncidents() > 0 || resolved > 0) {
                System.out.println(
                        "AUTO INCIDENT CYCLE => scanned="
                                + result.getScannedTrucks()
                                + ", created="
                                + result.getCreatedIncidents()
                                + ", resolved="
                                + resolved
                );
            }
        } catch (Exception e) {
            System.out.println("AUTO INCIDENT CYCLE FAILED => " + e.getMessage());
        }
    }
}