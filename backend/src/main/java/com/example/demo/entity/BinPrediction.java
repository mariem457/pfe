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

    @Column(name = "bin_id")
    private Long binId;

    @Column(name = "telemetry_id")
    private Long telemetryId;

    @Column(name = "actual_fill_next", precision = 10, scale = 2)
    private BigDecimal actualFillNext;

    @Column(name = "error_value", precision = 10, scale = 2)
    private BigDecimal errorValue;

    @Column(name = "predicted_fill_next", precision = 10, scale = 2)
    private BigDecimal predictedFillNext;

    @Column(name = "alert_status")
    private String alertStatus;

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

    public BigDecimal getPredictedFillNext() {
        return predictedFillNext;
    }

    public void setPredictedFillNext(BigDecimal predictedFillNext) {
        this.predictedFillNext = predictedFillNext;
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

    public void setErrorValue(BigDecimal
         errorValue) {
        this.errorValue = errorValue;
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public void setAlertStatus(String alertStatus) {
        this.alertStatus = alertStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}