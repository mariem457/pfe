package com.example.demo.dto;

import java.time.Instant;
import java.time.LocalDate;

public class MissionResponse {
    private Long id;
    private String missionCode;

    private Long driverId;
    private String driverName;

    private Long zoneId;
    private String zoneName;

    private String status;
    private String priority;

    private LocalDate plannedDate;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    private Long createdByUserId;
    private String notes;
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
	public Long getDriverId() {
		return driverId;
	}
	public void setDriverId(Long driverId) {
		this.driverId = driverId;
	}
	public String getDriverName() {
		return driverName;
	}
	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}
	public Long getZoneId() {
		return zoneId;
	}
	public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
	}
	public String getZoneName() {
		return zoneName;
	}
	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
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
	public Long getCreatedByUserId() {
		return createdByUserId;
	}
	public void setCreatedByUserId(Long createdByUserId) {
		this.createdByUserId = createdByUserId;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}

    // getters/setters
}