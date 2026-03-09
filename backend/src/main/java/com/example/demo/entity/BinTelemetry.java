package com.example.demo.entity;
import java.math.BigDecimal;
import jakarta.persistence.*;
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
    private short batteryLevel;

    private String status;

    private short rssi;

    @Column(nullable = false)
    private String source;

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

    public short getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(short batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public short getRssi() {
        return rssi;
    }
    public void setRssi(short rssi) {
        this.rssi = rssi;
    }
}