package com.example.demo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bin_predictions")
public class BinPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bin_id", nullable = false)
    private Long binId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "telemetry_id", nullable = false)
    private BinTelemetry telemetry;

    @Column(name = "actual_fill_next", precision = 10, scale = 2)
    private BigDecimal actualFillNext;

    @Column(name = "error_value", precision = 10, scale = 2)
    private BigDecimal errorValue;

    @Column(name = "predicted_fill_next", precision = 10, scale = 2)
    private BigDecimal predictedFillNext;

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

    public BinTelemetry getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(BinTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    public BigDecimal getActualFillNext() {
        return actualFillNext;
    }

    public void setActualFillNext(BigDecimal actualFillNext) {
        this.actualFillNext = actualFillNext;
    }

    public BigDecimal getErrorValue() {
        return errorValue;
    }

    public void setErrorValue(BigDecimal errorValue) {
        this.errorValue = errorValue;
    }

    public BigDecimal getPredictedFillNext() {
        return predictedFillNext;
    }

    public void setPredictedFillNext(BigDecimal predictedFillNext) {
        this.predictedFillNext = predictedFillNext;
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