package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class AnomalyCreateRequest {

    private Long binId;
    private Instant startTime;
    private Instant endTime;
    private String anomalyType;
    private BigDecimal score;

    // ✅ بدلناها من String إلى Map
    private Map<String, Object> details;

    private String modelName;
    private Boolean active;

    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }

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

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}