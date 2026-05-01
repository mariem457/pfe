package com.example.demo.dto;

public class UpdateSystemSettingsRequest {

    private Boolean maintenanceMode;
    private Boolean automaticBackup;
    private Boolean realtimeMonitoring;

    public UpdateSystemSettingsRequest() {
    }

    public Boolean getMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(Boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public Boolean getAutomaticBackup() { return automaticBackup; }
    public void setAutomaticBackup(Boolean automaticBackup) { this.automaticBackup = automaticBackup; }

    public Boolean getRealtimeMonitoring() { return realtimeMonitoring; }
    public void setRealtimeMonitoring(Boolean realtimeMonitoring) { this.realtimeMonitoring = realtimeMonitoring; }
}