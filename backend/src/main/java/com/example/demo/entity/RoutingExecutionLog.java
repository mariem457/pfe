package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "routing_execution_logs")
public class RoutingExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy", length = 30)
    private String strategy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "should_optimize", nullable = false)
    private boolean shouldOptimize;

    @Column(name = "refuel_only", nullable = false)
    private boolean refuelOnly;

    @Column(name = "trucks_count")
    private Integer trucksCount;

    @Column(name = "bins_sent_count")
    private Integer binsSentCount;

    @Column(name = "mandatory_bins_count")
    private Integer mandatoryBinsCount;

    @Column(name = "missions_created_count")
    private Integer missionsCreatedCount;

    @Column(name = "dropped_bins_count")
    private Integer droppedBinsCount;

    @Column(name = "matrix_source", length = 30)
    private String matrixSource;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isShouldOptimize() {
        return shouldOptimize;
    }

    public void setShouldOptimize(boolean shouldOptimize) {
        this.shouldOptimize = shouldOptimize;
    }

    public boolean isRefuelOnly() {
        return refuelOnly;
    }

    public void setRefuelOnly(boolean refuelOnly) {
        this.refuelOnly = refuelOnly;
    }

    public Integer getTrucksCount() {
        return trucksCount;
    }

    public void setTrucksCount(Integer trucksCount) {
        this.trucksCount = trucksCount;
    }

    public Integer getBinsSentCount() {
        return binsSentCount;
    }

    public void setBinsSentCount(Integer binsSentCount) {
        this.binsSentCount = binsSentCount;
    }

    public Integer getMandatoryBinsCount() {
        return mandatoryBinsCount;
    }

    public void setMandatoryBinsCount(Integer mandatoryBinsCount) {
        this.mandatoryBinsCount = mandatoryBinsCount;
    }

    public Integer getMissionsCreatedCount() {
        return missionsCreatedCount;
    }

    public void setMissionsCreatedCount(Integer missionsCreatedCount) {
        this.missionsCreatedCount = missionsCreatedCount;
    }

    public Integer getDroppedBinsCount() {
        return droppedBinsCount;
    }

    public void setDroppedBinsCount(Integer droppedBinsCount) {
        this.droppedBinsCount = droppedBinsCount;
    }

    public String getMatrixSource() {
        return matrixSource;
    }

    public void setMatrixSource(String matrixSource) {
        this.matrixSource = matrixSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}