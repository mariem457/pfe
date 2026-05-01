package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bin_telemetry")
public class BinTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bin_id")
    private Bin bin;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private short fillLevel;

    private BigDecimal weightKg;

    private Short batteryLevel;

    private String status;

    private Short rssi;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private Boolean collected = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bin getBin() {
        return bin;
    }

    public void setBin(Bin bin) {
        this.bin = bin;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public short getFillLevel() {
        return fillLevel;
    }

    public void setFillLevel(short fillLevel) {
        this.fillLevel = fillLevel;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Short getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Short batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Short getRssi() {
        return rssi;
    }

    public void setRssi(Short rssi) {
        this.rssi = rssi;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Boolean getCollected() {
        return collected;
    }

    public void setCollected(Boolean collected) {
        this.collected = collected;
    }
}