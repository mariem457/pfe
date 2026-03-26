package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "route_plans")
public class RoutePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private PlanType planType = PlanType.INITIAL;

    @Column(name = "optimization_algorithm", length = 100)
    private String optimizationAlgorithm;

    @Column(name = "optimization_version", length = 50)
    private String optimizationVersion;

    @Column(name = "total_distance_km", precision = 10, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Column(name = "estimated_fuel_liters", precision = 10, scale = 2)
    private BigDecimal estimatedFuelLiters;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_mode", length = 20)
    private TrafficMode trafficMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_status", nullable = false, length = 20)
    private PlanStatus planStatus = PlanStatus.PLANNED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "routePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<RouteStop> stops = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.planType == null) {
            this.planType = PlanType.INITIAL;
        }
        if (this.planStatus == null) {
            this.planStatus = PlanStatus.PLANNED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public enum PlanType {
        INITIAL, REPLANNED, EMERGENCY
    }

    public enum TrafficMode {
        SIMULATED, DYNAMIC, REAL
    }

    public enum PlanStatus {
        PLANNED, ACTIVE, REPLACED, CANCELLED, COMPLETED
    }

    public RoutePlan() {
    }

    public Long getId() {
        return id;
    }

    public Mission getMission() {
        return mission;
    }

    public void setMission(Mission mission) {
        this.mission = mission;
    }

    public Truck getTruck() {
        return truck;
    }

    public void setTruck(Truck truck) {
        this.truck = truck;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public String getOptimizationAlgorithm() {
        return optimizationAlgorithm;
    }

    public void setOptimizationAlgorithm(String optimizationAlgorithm) {
        this.optimizationAlgorithm = optimizationAlgorithm;
    }

    public String getOptimizationVersion() {
        return optimizationVersion;
    }

    public void setOptimizationVersion(String optimizationVersion) {
        this.optimizationVersion = optimizationVersion;
    }

    public BigDecimal getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(BigDecimal totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public Integer getEstimatedDurationMin() {
        return estimatedDurationMin;
    }

    public void setEstimatedDurationMin(Integer estimatedDurationMin) {
        this.estimatedDurationMin = estimatedDurationMin;
    }

    public BigDecimal getEstimatedFuelLiters() {
        return estimatedFuelLiters;
    }

    public void setEstimatedFuelLiters(BigDecimal estimatedFuelLiters) {
        this.estimatedFuelLiters = estimatedFuelLiters;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public TrafficMode getTrafficMode() {
        return trafficMode;
    }

    public void setTrafficMode(TrafficMode trafficMode) {
        this.trafficMode = trafficMode;
    }

    public PlanStatus getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(PlanStatus planStatus) {
        this.planStatus = planStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<RouteStop> getStops() {
        return stops;
    }

    public void setStops(List<RouteStop> stops) {
        this.stops = stops;
    }
}