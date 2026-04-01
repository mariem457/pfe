package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "truck_incidents")
public class TruckIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 30)
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id")
    private User reportedByUser;

    @Column(name = "auto_detected", nullable = false)
    private Boolean autoDetected = false;

    @Column
    private Double lat;

    @Column
    private Double lng;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private OffsetDateTime reportedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        if (this.reportedAt == null) {
            this.reportedAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = IncidentStatus.OPEN;
        }
        if (this.autoDetected == null) {
            this.autoDetected = false;
        }
    }

    public enum IncidentType {
        BREAKDOWN,
        FUEL_LOW,
        GPS_LOST,
        TRAFFIC_BLOCK,
        DELAY,
        OVERLOAD,
        DRIVER_UNAVAILABLE,
        OTHER
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum IncidentStatus {
        OPEN, IN_PROGRESS, RESOLVED, CANCELLED
    }

    public TruckIncident() {
    }

    public Long getId() {
        return id;
    }

    public Truck getTruck() {
        return truck;
    }

    public void setTruck(Truck truck) {
        this.truck = truck;
    }

    public Mission getMission() {
        return mission;
    }

    public void setMission(Mission mission) {
        this.mission = mission;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public User getReportedByUser() {
        return reportedByUser;
    }

    public void setReportedByUser(User reportedByUser) {
        this.reportedByUser = reportedByUser;
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

    public OffsetDateTime getReportedAt() {
        return reportedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}