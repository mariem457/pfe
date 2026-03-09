package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class AnomalyDto {

    public Long id;
    public Long binId;
    public String anomalyType;
    public BigDecimal score;

    // ✅ Map مش String
    public Map<String, Object> details;

    public Instant startTime;
    public Instant endTime;
    public Instant detectedAt;
    public String modelName;
    public boolean active;
}