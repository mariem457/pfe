package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "kpi_daily")
public class KpiDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "total_bins", nullable = false)
    private Integer totalBins = 0;

    @Column(name = "full_bins_count", nullable = false)
    private Integer fullBinsCount = 0;

    @Column(name = "bins_overflow_count", nullable = false)
    private Integer binsOverflowCount = 0;

    @Column(name = "alerts_count", nullable = false)
    private Integer alertsCount = 0;

    @Column(name = "open_alerts_count", nullable = false)
    private Integer openAlertsCount = 0;

    @Column(name = "missions_count", nullable = false)
    private Integer missionsCount = 0;

    @Column(name = "collected_bins_count", nullable = false)
    private Integer collectedBinsCount = 0;

    @Column(name = "active_trucks_count", nullable = false)
    private Integer activeTrucksCount = 0;

    @Column(name = "avg_fill_level", precision = 5, scale = 2)
    private BigDecimal avgFillLevel;

    @Column(name = "estimated_distance_km", precision = 10, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "estimated_co2_kg", precision = 10, scale = 2)
    private BigDecimal estimatedCo2Kg;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public KpiDaily() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getTotalBins() {
        return totalBins;
    }

    public void setTotalBins(Integer totalBins) {
        this.totalBins = totalBins;
    }

    public Integer getFullBinsCount() {
        return fullBinsCount;
    }

    public void setFullBinsCount(Integer fullBinsCount) {
        this.fullBinsCount = fullBinsCount;
    }

    public Integer getBinsOverflowCount() {
        return binsOverflowCount;
    }

    public void setBinsOverflowCount(Integer binsOverflowCount) {
        this.binsOverflowCount = binsOverflowCount;
    }

    public Integer getAlertsCount() {
        return alertsCount;
    }

    public void setAlertsCount(Integer alertsCount) {
        this.alertsCount = alertsCount;
    }

    public Integer getOpenAlertsCount() {
        return openAlertsCount;
    }

    public void setOpenAlertsCount(Integer openAlertsCount) {
        this.openAlertsCount = openAlertsCount;
    }

    public Integer getMissionsCount() {
        return missionsCount;
    }

    public void setMissionsCount(Integer missionsCount) {
        this.missionsCount = missionsCount;
    }

    public Integer getCollectedBinsCount() {
        return collectedBinsCount;
    }

    public void setCollectedBinsCount(Integer collectedBinsCount) {
        this.collectedBinsCount = collectedBinsCount;
    }

    public Integer getActiveTrucksCount() {
        return activeTrucksCount;
    }

    public void setActiveTrucksCount(Integer activeTrucksCount) {
        this.activeTrucksCount = activeTrucksCount;
    }

    public BigDecimal getAvgFillLevel() {
        return avgFillLevel;
    }

    public void setAvgFillLevel(BigDecimal avgFillLevel) {
        this.avgFillLevel = avgFillLevel;
    }

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public BigDecimal getEstimatedCo2Kg() {
        return estimatedCo2Kg;
    }

    public void setEstimatedCo2Kg(BigDecimal estimatedCo2Kg) {
        this.estimatedCo2Kg = estimatedCo2Kg;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}