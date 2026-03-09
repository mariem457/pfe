package com.example.demo.dto;

import java.time.Instant;

public class AlertDetailsResponse extends AlertResponse {

    private Double binLat;
    private Double binLng;
    private String zoneName;

    private Instant telemetryTimestamp;
    private Integer fillLevel;
    private Integer batteryLevel;

    public Double getBinLat() { return binLat; }
    public void setBinLat(Double binLat) { this.binLat = binLat; }

    public Double getBinLng() { return binLng; }
    public void setBinLng(Double binLng) { this.binLng = binLng; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

    public Instant getTelemetryTimestamp() { return telemetryTimestamp; }
    public void setTelemetryTimestamp(Instant telemetryTimestamp) { this.telemetryTimestamp = telemetryTimestamp; }

    public Integer getFillLevel() { return fillLevel; }
    public void setFillLevel(Integer fillLevel) { this.fillLevel = fillLevel; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
}