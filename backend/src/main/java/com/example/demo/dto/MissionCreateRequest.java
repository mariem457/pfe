package com.example.demo.dto;

import java.time.LocalDate;

public class MissionCreateRequest {
    private String missionCode;
    private Long driverId;
    private Long zoneId;
    private String priority;   // optional
    private LocalDate plannedDate;
    private Long createdByUserId;
    private String notes;
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
	public Long getZoneId() {
		return zoneId;
	}
	public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
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