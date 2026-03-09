package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class AnomalyResponse {

    private Long id;
    private Long binId;
    private String binCode;
    private Instant startTime;
    private Instant endTime;
    private String anomalyType;
    private BigDecimal score;

    // ✅ Map بدل String
    private Map<String, Object> details;

    private Instant detectedAt;
    private String modelName;
    private boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }

    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}