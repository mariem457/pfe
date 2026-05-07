package com.example.demo.dto;

import java.time.Instant;

public class AlertResponse {
    private Long id;

    private Long binId;
    private String binCode;
    private Long telemetryId;

    private Long truckId;
    private String truckCode;

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

    private Instant createdAt;
    private boolean resolved;
    private Instant resolvedAt;
    private Long resolvedByUserId;
    private Long exceptionZoneId;
    private String exceptionWasteType;
    private String exceptionBinIds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }

    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }

    public Long getTelemetryId() { return telemetryId; }
    public void setTelemetryId(Long telemetryId) { this.telemetryId = telemetryId; }

    public Long getTruckId() { return truckId; }
    public void setTruckId(Long truckId) { this.truckId = truckId; }

    public String getTruckCode() { return truckCode; }
    public void setTruckCode(String truckCode) { this.truckCode = truckCode; }

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public Long getResolvedByUserId() { return resolvedByUserId; }
    public void setResolvedByUserId(Long resolvedByUserId) { this.resolvedByUserId = resolvedByUserId; }

public Long getExceptionZoneId() { return exceptionZoneId; }
public void setExceptionZoneId(Long exceptionZoneId) { this.exceptionZoneId = exceptionZoneId; }

public String getExceptionWasteType() { return exceptionWasteType; }
public void setExceptionWasteType(String exceptionWasteType) { this.exceptionWasteType = exceptionWasteType; }

public String getExceptionBinIds() { return exceptionBinIds; }
public void setExceptionBinIds(String exceptionBinIds) { this.exceptionBinIds = exceptionBinIds; }
}