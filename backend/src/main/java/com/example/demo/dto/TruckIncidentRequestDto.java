package com.example.demo.dto;

import com.example.demo.entity.TruckIncident;

public class TruckIncidentRequestDto {

    private Long truckId;
    private Long missionId;
    private TruckIncident.IncidentType incidentType;
    private TruckIncident.Severity severity;
    private String description;
    private TruckIncident.IncidentStatus status;
    private Long reportedByUserId;
    private Boolean autoDetected;
    private Double lat;
    private Double lng;

    public TruckIncidentRequestDto() {
    }

    public Long getTruckId() {
        return truckId;
    }

    public void setTruckId(Long truckId) {
        this.truckId = truckId;
    }

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public TruckIncident.IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(TruckIncident.IncidentType incidentType) {
        this.incidentType = incidentType;
    }

    public TruckIncident.Severity getSeverity() {
        return severity;
    }

    public void setSeverity(TruckIncident.Severity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public TruckIncident.IncidentStatus getStatus() {
        return status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TruckIncident.IncidentStatus status) {
        this.status = status;
    }

    public Long getReportedByUserId() {
        return reportedByUserId;
    }

    public void setReportedByUserId(Long reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public Boolean getAutoDetected() {
        return autoDetected;
    }

    public void setAutoDetected(Boolean autoDetected) {
        this.autoDetected = autoDetected;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }
}