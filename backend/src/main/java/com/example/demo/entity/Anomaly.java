package com.example.demo.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "anomalies",
        indexes = {
                @Index(name = "idx_anomalies_bin_detected", columnList = "bin_id,detected_at")
        })
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id", nullable = false)
    private Bin bin;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "anomaly_type", nullable = false, length = 20)
    private String anomalyType; // DRIFT, STUCK, OUTLIER, PACKET_LOSS

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @PrePersist
    void onCreate() {
        if (detectedAt == null) detectedAt = Instant.now();
    }

    // ===== GETTERS & SETTERS =====

    public Long getId() { return id; }

    public Bin getBin() { return bin; }
    public void setBin(Bin bin) { this.bin = bin; }

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