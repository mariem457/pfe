package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bin_time_predictions")
public class BinTimePrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bin_id")
    private Long binId;

    @Column(name = "telemetry_id")
    private Long telemetryId;

    @Column(name = "predicted_hours", precision = 10, scale = 2)
    private BigDecimal predictedHours;

    @Column(name = "alert_status")
    private String alertStatus;

    @Column(name = "priority_score", precision = 10, scale = 2)
    private BigDecimal priorityScore;

    @Column(name = "should_collect")
    private Boolean shouldCollect;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Long getBinId() {
        return binId;
    }

    public void setBinId(Long binId) {
        this.binId = binId;
    }

    public Long getTelemetryId() {
        return telemetryId;
    }

    public void setTelemetryId(Long telemetryId) {
        this.telemetryId = telemetryId;
    }

    public BigDecimal getPredictedHours() {
        return predictedHours;
    }

    public void setPredictedHours(BigDecimal predictedHours) {
        this.predictedHours = predictedHours;
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public void setAlertStatus(String alertStatus) {
        this.alertStatus = alertStatus;
    }

    public BigDecimal getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(BigDecimal priorityScore) {
        this.priorityScore = priorityScore;
    }

    public Boolean getShouldCollect() {
        return shouldCollect;
    }

    public void setShouldCollect(Boolean shouldCollect) {
        this.shouldCollect = shouldCollect;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}