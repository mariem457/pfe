package com.example.demo.dto;

public class AlertCreateRequest {
    private Long binId;
    private Long telemetryId; // optional
    private String alertType;
    private String severity;
    private String title;
    private String message;

    // getters/setters
    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }
    public Long getTelemetryId() { return telemetryId; }
    public void setTelemetryId(Long telemetryId) { this.telemetryId = telemetryId; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}