package com.example.demo.dto;

public class BinSensorDataDto {

    private String binId;
    private double fillLevel;
    private int gasValue;
    private boolean fireDetected;
    private String status;
    private double voltage;

    public BinSensorDataDto() {
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

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }
}