package com.example.demo.dto;

public class AutoIncidentRunResponse {
    private int scannedTrucks;
    private int createdIncidents;

    public AutoIncidentRunResponse() {}

    public AutoIncidentRunResponse(int scannedTrucks, int createdIncidents) {
        this.scannedTrucks = scannedTrucks;
        this.createdIncidents = createdIncidents;
    }

    public int getScannedTrucks() {
        return scannedTrucks;
    }

    public void setScannedTrucks(int scannedTrucks) {
        this.scannedTrucks = scannedTrucks;
    }

    public int getCreatedIncidents() {
        return createdIncidents;
    }

    public void setCreatedIncidents(int createdIncidents) {
        this.createdIncidents = createdIncidents;
    }
}