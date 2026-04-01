package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
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
    
    
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 20)
    private AssignmentStatus assignmentStatus = AssignmentStatus.PLANNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reassigned_from_truck_id")
    private Truck reassignedFromTruck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reassigned_to_truck_id")
    private Truck reassignedToTruck;

    @Column(name = "planned_arrival")
    private OffsetDateTime plannedArrival;

    @Column(name = "actual_arrival")
    private OffsetDateTime actualArrival;

    @Column(name = "skipped_reason", length = 50)
    private String skippedReason;
    public enum AssignmentStatus {
        PLANNED,
        COLLECTED,
        SKIPPED,
        REASSIGNED,
        CANCELLED
    }
	public AssignmentStatus getAssignmentStatus() {
		return assignmentStatus;
	}

	public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
		this.assignmentStatus = assignmentStatus;
	}

	public Truck getReassignedFromTruck() {
		return reassignedFromTruck;
	}

	public void setReassignedFromTruck(Truck reassignedFromTruck) {
		this.reassignedFromTruck = reassignedFromTruck;
	}

	public Truck getReassignedToTruck() {
		return reassignedToTruck;
	}

	public void setReassignedToTruck(Truck reassignedToTruck) {
		this.reassignedToTruck = reassignedToTruck;
	}

	public OffsetDateTime getPlannedArrival() {
		return plannedArrival;
	}

	public void setPlannedArrival(OffsetDateTime plannedArrival) {
		this.plannedArrival = plannedArrival;
	}

	public OffsetDateTime getActualArrival() {
		return actualArrival;
	}

	public void setActualArrival(OffsetDateTime actualArrival) {
		this.actualArrival = actualArrival;
	}

	public String getSkippedReason() {
		return skippedReason;
	}

	public void setSkippedReason(String skippedReason) {
		this.skippedReason = skippedReason;
	}

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