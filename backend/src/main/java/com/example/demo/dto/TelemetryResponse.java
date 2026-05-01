package com.example.demo.dto;

import java.time.Instant;

public class TelemetryResponse {
    public Long id;
    public String binCode;
    public short fillLevel;
    public Short batteryLevel;
    public String status;
    public String source;
    public Instant timestamp;

    // optional (باش تتفادى Lazy)
    public Long zoneId;
    public String zoneName;
}