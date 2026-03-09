package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mission_bins",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mission_bin", columnNames = {"mission_id","bin_id"})
        },
        indexes = {
                @Index(name = "idx_mission_bins_mission_order", columnList = "mission_id,visit_order")
        })
public class MissionBin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id", nullable = false)
    private Bin bin;

    @Column(name = "visit_order", nullable = false)
    private int visitOrder;

    @Column(name = "target_fill_threshold")
    private Short targetFillThreshold;

    @Column(name = "assigned_reason", length = 20)
    private String assignedReason; // THRESHOLD, PREDICTION, MANUAL

    @Column(nullable = false)
    private boolean collected = false;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by")
    private Driver collectedBy;

    @Column(name = "driver_note", columnDefinition = "TEXT")
    private String driverNote;

    @Column(name = "issue_type", length = 30)
    private String issueType; // BLOCKED, DAMAGED, SENSOR_ERROR, OTHER

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Mission getMission() {
		return mission;
	}

	public void setMission(Mission mission) {
		this.mission = mission;
	}

	public Bin getBin() {
		return bin;
	}

	public void setBin(Bin bin) {
		this.bin = bin;
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

	public Driver getCollectedBy() {
		return collectedBy;
	}

	public void setCollectedBy(Driver collectedBy) {
		this.collectedBy = collectedBy;
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