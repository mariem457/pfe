package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "postponed_bins")
public class PostponedBin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id", nullable = false)
    private Bin bin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_mission_id")
    private Mission routingMission;

    @Column(name = "reason", length = 100, nullable = false)
    private String reason;

    @Column(name = "predicted_priority")
    private Double predictedPriority;

    @Column(name = "predicted_hours_to_full")
    private Double predictedHoursToFull;

    @Column(name = "fill_level")
    private Double fillLevel;

    @Column(name = "estimated_load_kg")
    private Double estimatedLoadKg;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Bin getBin() {
        return bin;
    }

    public void setBin(Bin bin) {
        this.bin = bin;
    }

    public Mission getRoutingMission() {
        return routingMission;
    }

    public void setRoutingMission(Mission routingMission) {
        this.routingMission = routingMission;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getPredictedPriority() {
        return predictedPriority;
    }

    public void setPredictedPriority(Double predictedPriority) {
        this.predictedPriority = predictedPriority;
    }

    public Double getPredictedHoursToFull() {
        return predictedHoursToFull;
    }

    public void setPredictedHoursToFull(Double predictedHoursToFull) {
        this.predictedHoursToFull = predictedHoursToFull;
    }

    public Double getFillLevel() {
        return fillLevel;
    }

    public void setFillLevel(Double fillLevel) {
        this.fillLevel = fillLevel;
    }

    public Double getEstimatedLoadKg() {
        return estimatedLoadKg;
    }

    public void setEstimatedLoadKg(Double estimatedLoadKg) {
        this.estimatedLoadKg = estimatedLoadKg;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}