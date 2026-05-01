package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "system_settings")
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maintenance_mode", nullable = false)
    private Boolean maintenanceMode = false;

    @Column(name = "automatic_backup", nullable = false)
    private Boolean automaticBackup = true;

    @Column(name = "realtime_monitoring", nullable = false)
    private Boolean realtimeMonitoring = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (maintenanceMode == null) maintenanceMode = false;
        if (automaticBackup == null) automaticBackup = true;
        if (realtimeMonitoring == null) realtimeMonitoring = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }

    public Boolean getMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(Boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public Boolean getAutomaticBackup() { return automaticBackup; }
    public void setAutomaticBackup(Boolean automaticBackup) { this.automaticBackup = automaticBackup; }

    public Boolean getRealtimeMonitoring() { return realtimeMonitoring; }
    public void setRealtimeMonitoring(Boolean realtimeMonitoring) { this.realtimeMonitoring = realtimeMonitoring; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}