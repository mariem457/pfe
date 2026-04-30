package com.example.demo.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class BinResponse {
    public Long id;
    public String binCode;
    public String type;
    public String wasteType;
    public Long zoneId;
    public String zoneName;
    public Double lat;
    public Double lng;
    public Double accessLat;
    public Double accessLng;
    public LocalDate installationDate;
    public Boolean isActive;
    public String notes;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
    public Integer clusterId;

    // ===== Latest telemetry =====
    public Integer fillLevel;
    public Integer batteryLevel;
    public String status;
    public OffsetDateTime lastTelemetryAt;
}