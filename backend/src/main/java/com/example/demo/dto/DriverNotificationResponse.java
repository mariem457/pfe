package com.example.demo.dto;

import com.example.demo.entity.DriverNotification;
import java.time.OffsetDateTime;

public class DriverNotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String message;
    private OffsetDateTime createdAt;
    private Boolean read;

    private Long incidentId;
    private Long truckId;
    private String truckCode;
    private Long missionId;

    private String status;
    private String response;
    private OffsetDateTime readAt;
    private OffsetDateTime respondedAt;

    public DriverNotificationResponse(DriverNotification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt();
        this.read = notification.getRead();

        this.status = notification.getStatus();
        this.response = notification.getResponse();
        this.readAt = notification.getReadAt();
        this.respondedAt = notification.getRespondedAt();

        if (notification.getIncident() != null) {
            this.incidentId = notification.getIncident().getId();
        }

        if (notification.getTruck() != null) {
            this.truckId = notification.getTruck().getId();
            this.truckCode = notification.getTruck().getTruckCode();
        }

        if (notification.getMission() != null) {
            this.missionId = notification.getMission().getId();
        }
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Boolean getRead() { return read; }

    public Long getIncidentId() { return incidentId; }
    public Long getTruckId() { return truckId; }
    public String getTruckCode() { return truckCode; }
    public Long getMissionId() { return missionId; }

    public String getStatus() { return status; }
    public String getResponse() { return response; }
    public OffsetDateTime getReadAt() { return readAt; }
    public OffsetDateTime getRespondedAt() { return respondedAt; }
}