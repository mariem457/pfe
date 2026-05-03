package com.example.demo.dto;

public class AlertCreateRequest {

    private Long binId;
    private Long telemetryId; // optional

    private Long truckId;
    private Long missionId;
    private Long incidentId;

    private String entityType;
    private Long entityId;

    private String alertType;
    private String severity;
    private String title;
    private String message;

    private String recommendation;
    private String actionType;

    // getters / setters

    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }

    public Long getTelemetryId() { return telemetryId; }
    public void setTelemetryId(Long telemetryId) { this.telemetryId = telemetryId; }

    public Long getTruckId() { return truckId; }
    public void setTruckId(Long truckId) { this.truckId = truckId; }

    public Long getMissionId() { return missionId; }
    public void setMissionId(Long missionId) { this.missionId = missionId; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
}