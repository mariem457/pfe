package com.example.demo.dto;

import com.example.demo.entity.TruckIncident;
import java.time.OffsetDateTime;

public class TruckIncidentResponseDto {

    private Long id;
    private Long truckId;
    private String truckCode;
    private Long missionId;
    private TruckIncident.IncidentType incidentType;
    private TruckIncident.Severity severity;
    private String description;
    private TruckIncident.IncidentStatus status;
    private Long reportedByUserId;
    private String reportedByUsername;
    private Boolean autoDetected;
    private Double lat;
    private Double lng;
    private OffsetDateTime reportedAt;
    private OffsetDateTime resolvedAt;

    public TruckIncidentResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public Long getTruckId() {
        return truckId;
    }

    public String getTruckCode() {
        return truckCode;
    }

    public Long getMissionId() {
        return missionId;
    }

    public TruckIncident.IncidentType getIncidentType() {
        return incidentType;
    }

    public TruckIncident.Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public TruckIncident.IncidentStatus getStatus() {
        return status;
    }

    public Long getReportedByUserId() {
        return reportedByUserId;
    }

    public String getReportedByUsername() {
        return reportedByUsername;
    }

    public Boolean getAutoDetected() {
        return autoDetected;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public OffsetDateTime getReportedAt() {
        return reportedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTruckId(Long truckId) {
        this.truckId = truckId;
    }

    public void setTruckCode(String truckCode) {
        this.truckCode = truckCode;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public void setIncidentType(TruckIncident.IncidentType incidentType) {
        this.incidentType = incidentType;
    }

    public void setSeverity(TruckIncident.Severity severity) {
        this.severity = severity;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TruckIncident.IncidentStatus status) {
        this.status = status;
    }

    public void setReportedByUserId(Long reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public void setReportedByUsername(String reportedByUsername) {
        this.reportedByUsername = reportedByUsername;
    }

    public void setAutoDetected(Boolean autoDetected) {
        this.autoDetected = autoDetected;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public void setReportedAt(OffsetDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}