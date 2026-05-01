package com.example.demo.dto;

import java.math.BigDecimal;

public class TelemetryRequest {

    private String binCode;
    private short fillLevel;
    private Short batteryLevel;
    private BigDecimal weightKg;
    private String status;
    private String source;
    private Short rssi;
    private Boolean collected;

    public String getBinCode() {
        return binCode;
    }

    public void setBinCode(String binCode) {
        this.binCode = binCode;
    }

    public short getFillLevel() {
        return fillLevel;
    }

    public void setFillLevel(short fillLevel) {
        this.fillLevel = fillLevel;
    }

    public Short getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Short batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
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

    public Short getRssi() {
        return rssi;
    }

    public void setRssi(Short rssi) {
        this.rssi = rssi;
    }

    public Boolean getCollected() {
        return collected;
    }

    public void setCollected(Boolean collected) {
        this.collected = collected;
    }
}