package com.example.demo.dto;

public class SystemSettingsResponse {

    private boolean maintenanceMode;
    private boolean automaticBackup;
    private boolean realtimeMonitoring;

    public SystemSettingsResponse() {
    }

    public SystemSettingsResponse(boolean maintenanceMode, boolean automaticBackup, boolean realtimeMonitoring) {
        this.maintenanceMode = maintenanceMode;
        this.automaticBackup = automaticBackup;
        this.realtimeMonitoring = realtimeMonitoring;
    }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public boolean isAutomaticBackup() { return automaticBackup; }
    public void setAutomaticBackup(boolean automaticBackup) { this.automaticBackup = automaticBackup; }

    public boolean isRealtimeMonitoring() { return realtimeMonitoring; }
    public void setRealtimeMonitoring(boolean realtimeMonitoring) { this.realtimeMonitoring = realtimeMonitoring; }
}