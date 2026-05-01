package com.example.demo.dto;

import java.time.Instant;
import java.time.OffsetDateTime;

public class MissionBinResponse {

    private Long id;
    private Long missionId;
    private Long binId;
    private String binCode;
    private Double lat;
    private Double lng;
    private Integer visitOrder;
    private Short targetFillThreshold;
    private String assignedReason;
    private Boolean collected;
    private Instant collectedAt;
    private Long collectedByDriverId;
    private String driverNote;
    private String issueType;
    private String photoUrl;
    private String assignmentStatus;
    private Long reassignedFromTruckId;
    private Long reassignedToTruckId;
    private OffsetDateTime plannedArrival;
    private OffsetDateTime actualArrival;
    private String skippedReason;

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

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Integer getVisitOrder() {
        return visitOrder;
    }

    public void setVisitOrder(Integer visitOrder) {
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

    public Boolean getCollected() {
        return collected;
    }

    public void setCollected(Boolean collected) {
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

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public Long getReassignedFromTruckId() {
        return reassignedFromTruckId;
    }

    public void setReassignedFromTruckId(Long reassignedFromTruckId) {
        this.reassignedFromTruckId = reassignedFromTruckId;
    }

    public Long getReassignedToTruckId() {
        return reassignedToTruckId;
    }

    public void setReassignedToTruckId(Long reassignedToTruckId) {
        this.reassignedToTruckId = reassignedToTruckId;
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
}