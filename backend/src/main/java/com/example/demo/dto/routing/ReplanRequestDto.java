package com.example.demo.dto.routing;

public class ReplanRequestDto {

    private Long affectedTruckId;
    private String incidentType;
    private String reason;

    public ReplanRequestDto() {
    }

    public Long getAffectedTruckId() {
        return affectedTruckId;
    }

    public void setAffectedTruckId(Long affectedTruckId) {
        this.affectedTruckId = affectedTruckId;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}