package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bin_sensor_data")
public class BinSensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String binId;

    private double fillLevel;

    private int gasValue;

    private boolean fireDetected;

    private String status;

    private LocalDateTime createdAt;

    public BinSensorData() {
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getBinId() {
        return binId;
    }

    public void setBinId(String binId) {
        this.binId = binId;
    }

    public double getFillLevel() {
        return fillLevel;
    }

    public void setFillLevel(double fillLevel) {
        this.fillLevel = fillLevel;
    }

    public int getGasValue() {
        return gasValue;
    }

    public void setGasValue(int gasValue) {
        this.gasValue = gasValue;
    }

    public boolean isFireDetected() {
        return fireDetected;
    }

    public void setFireDetected(boolean fireDetected) {
        this.fireDetected = fireDetected;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}