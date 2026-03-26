package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "missions",
        indexes = {
                @Index(name = "idx_missions_driver_date", columnList = "driver_id,planned_date")
        })
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_code", nullable = false, unique = true, length = 40)
    private String missionCode;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(nullable = false, length = 20)
    private String status; // CREATED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(nullable = false, length = 10)
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id")
    private Truck truck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @Column(name = "estimated_distance_km", precision = 10, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "actual_distance_km", precision = 10, scale = 2)
    private BigDecimal actualDistanceKm;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Column(name = "actual_duration_min")
    private Integer actualDurationMin;

    @Column(name = "estimated_fuel_liters", precision = 10, scale = 2)
    private BigDecimal estimatedFuelLiters;

    @Column(name = "actual_fuel_liters", precision = 10, scale = 2)
    private BigDecimal actualFuelLiters;

    @Column(name = "replanned_count", nullable = false)
    private Integer replannedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_status_detail", length = 30)
    private MissionStatusDetail missionStatusDetail;

    @OneToMany(mappedBy = "mission", fetch = FetchType.LAZY)
    private List<RoutePlan> routePlans = new ArrayList<>();
    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getMissionCode() {
		return missionCode;
	}
	public void setMissionCode(String missionCode) {
		this.missionCode = missionCode;
	}
	public Driver getDriver() {
		return driver;
	}
	public void setDriver(Driver driver) {
		this.driver = driver;
	}
	public Zone getZone() {
		return zone;
	}
	public void setZone(Zone zone) {
		this.zone = zone;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public LocalDate getPlannedDate() {
		return plannedDate;
	}
	public void setPlannedDate(LocalDate plannedDate) {
		this.plannedDate = plannedDate;
	}
	public Instant getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
	public Instant getStartedAt() {
		return startedAt;
	}
	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}
	public Instant getCompletedAt() {
		return completedAt;
	}
	public void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}
	public User getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
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
	public BigDecimal getEstimatedDistanceKm() {
		return estimatedDistanceKm;
	}
	public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) {
		this.estimatedDistanceKm = estimatedDistanceKm;
	}
	public BigDecimal getActualDistanceKm() {
		return actualDistanceKm;
	}
	public void setActualDistanceKm(BigDecimal actualDistanceKm) {
		this.actualDistanceKm = actualDistanceKm;
	}
	public Integer getEstimatedDurationMin() {
		return estimatedDurationMin;
	}
	public void setEstimatedDurationMin(Integer estimatedDurationMin) {
		this.estimatedDurationMin = estimatedDurationMin;
	}
	public Integer getActualDurationMin() {
		return actualDurationMin;
	}
	public void setActualDurationMin(Integer actualDurationMin) {
		this.actualDurationMin = actualDurationMin;
	}
	public BigDecimal getEstimatedFuelLiters() {
		return estimatedFuelLiters;
	}
	public void setEstimatedFuelLiters(BigDecimal estimatedFuelLiters) {
		this.estimatedFuelLiters = estimatedFuelLiters;
	}
	public BigDecimal getActualFuelLiters() {
		return actualFuelLiters;
	}
	public void setActualFuelLiters(BigDecimal actualFuelLiters) {
		this.actualFuelLiters = actualFuelLiters;
	}
	public Integer getReplannedCount() {
		return replannedCount;
	}
	public void setReplannedCount(Integer replannedCount) {
		this.replannedCount = replannedCount;
	}
	public MissionStatusDetail getMissionStatusDetail() {
		return missionStatusDetail;
	}
	public void setMissionStatusDetail(MissionStatusDetail missionStatusDetail) {
		this.missionStatusDetail = missionStatusDetail;
	}
	public List<RoutePlan> getRoutePlans() {
		return routePlans;
	}
	public void setRoutePlans(List<RoutePlan> routePlans) {
		this.routePlans = routePlans;
	}
	@PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) status = "CREATED";
        if (priority == null) priority = "NORMAL";
    }
	public enum MissionStatusDetail {
	    CREATED,
	    PLANNED,
	    IN_PROGRESS,
	    PARTIALLY_REASSIGNED,
	    REPLANNED,
	    COMPLETED,
	    CANCELLED,
	    FAILED
	}

    // getters/setters (IDE generate)
}