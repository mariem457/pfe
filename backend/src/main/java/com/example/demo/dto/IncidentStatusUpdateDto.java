package com.example.demo.dto;

import com.example.demo.entity.TruckIncident;

public class IncidentStatusUpdateDto {

    private TruckIncident.IncidentStatus status;
    private String description;

    public IncidentStatusUpdateDto() {
    }

    public TruckIncident.IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(TruckIncident.IncidentStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}