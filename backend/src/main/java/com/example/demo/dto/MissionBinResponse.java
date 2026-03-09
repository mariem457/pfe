package com.example.demo.dto;

import java.time.Instant;

public class MissionBinResponse {
    private Long id;
    private Long missionId;

    private Long binId;
    private String binCode;

    private int visitOrder;
    private Short targetFillThreshold;
    private String assignedReason;

    private boolean collected;
    private Instant collectedAt;

    private Long collectedByDriverId;
    private String driverNote;

    private String issueType;
    private String photoUrl;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getMissionId() {
		return missionId;
	}
	public void setMissionId(Long missionId) {
		this.missionId = missionId;
	}
	public Long getBinId() {
		return binId;
	}
	public void setBinId(Long binId) {
		this.binId = binId;
	}
	public String getBinCode() {
		return binCode;
	}
	public void setBinCode(String binCode) {
		this.binCode = binCode;
	}
	public int getVisitOrder() {
		return visitOrder;
	}
	public void setVisitOrder(int visitOrder) {
		this.visitOrder = visitOrder;
	}
	public Short getTargetFillThreshold() {
		return targetFillThreshold;
	}
	public void setTargetFillThreshold(Short targetFillThreshold) {
		this.targetFillThreshold = targetFillThreshold;
	}
	public String getAssignedReason() {
		return assignedReason;
	}
	public void setAssignedReason(String assignedReason) {
		this.assignedReason = assignedReason;
	}
	public boolean isCollected() {
		return collected;
	}
	public void setCollected(boolean collected) {
		this.collected = collected;
	}
	public Instant getCollectedAt() {
		return collectedAt;
	}
	public void setCollectedAt(Instant collectedAt) {
		this.collectedAt = collectedAt;
	}
	public Long getCollectedByDriverId() {
		return collectedByDriverId;
	}
	public void setCollectedByDriverId(Long collectedByDriverId) {
		this.collectedByDriverId = collectedByDriverId;
	}
	public String getDriverNote() {
		return driverNote;
	}
	public void setDriverNote(String driverNote) {
		this.driverNote = driverNote;
	}
	public String getIssueType() {
		return issueType;
	}
	public void setIssueType(String issueType) {
		this.issueType = issueType;
	}
	public String getPhotoUrl() {
		return photoUrl;
	}
	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

    // getters/setters
}