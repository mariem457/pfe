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
    private String collectedByDriverName;
    private String driverNote;
    private String issueType;
    private String photoUrl;
    private String assignmentStatus;
    private Long reassignedFromTruckId;
    private Long reassignedToTruckId;
    private OffsetDateTime plannedArrival;
    private OffsetDateTime actualArrival;
    private String skippedReason;
    private String wasteType;

    // ===== Map details / IA details =====
    private Integer fillLevel;
    private Integer batteryLevel;
    private Double weightKg;
    private String status;
    private String zoneName;
    private Integer clusterId;

    private Double priorityScore;
    private Double predictedFillLevelNext;
    private Double hoursToFull;
    private String alertStatus;
    private Boolean shouldCollect;

    private String decisionReason;
    private String scoreExplanation;
    private String urgencyExplanation;
    private String classificationExplanation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMissionId() { return missionId; }
    public void setMissionId(Long missionId) { this.missionId = missionId; }

    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }

    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Integer getVisitOrder() { return visitOrder; }
    public void setVisitOrder(Integer visitOrder) { this.visitOrder = visitOrder; }

    public Short getTargetFillThreshold() { return targetFillThreshold; }
    public void setTargetFillThreshold(Short targetFillThreshold) { this.targetFillThreshold = targetFillThreshold; }

    public String getAssignedReason() { return assignedReason; }
    public void setAssignedReason(String assignedReason) { this.assignedReason = assignedReason; }

    public Boolean getCollected() { return collected; }
    public void setCollected(Boolean collected) { this.collected = collected; }

    public Instant getCollectedAt() { return collectedAt; }
    public void setCollectedAt(Instant collectedAt) { this.collectedAt = collectedAt; }

    public Long getCollectedByDriverId() { return collectedByDriverId; }
    public void setCollectedByDriverId(Long collectedByDriverId) { this.collectedByDriverId = collectedByDriverId; }

    public String getCollectedByDriverName() { return collectedByDriverName; }
    public void setCollectedByDriverName(String collectedByDriverName) { this.collectedByDriverName = collectedByDriverName; }

    public String getDriverNote() { return driverNote; }
    public void setDriverNote(String driverNote) { this.driverNote = driverNote; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(String assignmentStatus) { this.assignmentStatus = assignmentStatus; }

    public Long getReassignedFromTruckId() { return reassignedFromTruckId; }
    public void setReassignedFromTruckId(Long reassignedFromTruckId) { this.reassignedFromTruckId = reassignedFromTruckId; }

    public Long getReassignedToTruckId() { return reassignedToTruckId; }
    public void setReassignedToTruckId(Long reassignedToTruckId) { this.reassignedToTruckId = reassignedToTruckId; }

    public OffsetDateTime getPlannedArrival() { return plannedArrival; }
    public void setPlannedArrival(OffsetDateTime plannedArrival) { this.plannedArrival = plannedArrival; }

    public OffsetDateTime getActualArrival() { return actualArrival; }
    public void setActualArrival(OffsetDateTime actualArrival) { this.actualArrival = actualArrival; }

    public String getSkippedReason() { return skippedReason; }
    public void setSkippedReason(String skippedReason) { this.skippedReason = skippedReason; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }

    public Integer getFillLevel() { return fillLevel; }
    public void setFillLevel(Integer fillLevel) { this.fillLevel = fillLevel; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

    public Integer getClusterId() { return clusterId; }
    public void setClusterId(Integer clusterId) { this.clusterId = clusterId; }

    public Double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Double priorityScore) { this.priorityScore = priorityScore; }

    public Double getPredictedFillLevelNext() { return predictedFillLevelNext; }
    public void setPredictedFillLevelNext(Double predictedFillLevelNext) { this.predictedFillLevelNext = predictedFillLevelNext; }

    public Double getHoursToFull() { return hoursToFull; }
    public void setHoursToFull(Double hoursToFull) { this.hoursToFull = hoursToFull; }

    public String getAlertStatus() { return alertStatus; }
    public void setAlertStatus(String alertStatus) { this.alertStatus = alertStatus; }

    public Boolean getShouldCollect() { return shouldCollect; }
    public void setShouldCollect(Boolean shouldCollect) { this.shouldCollect = shouldCollect; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }

    public String getScoreExplanation() { return scoreExplanation; }
    public void setScoreExplanation(String scoreExplanation) { this.scoreExplanation = scoreExplanation; }

    public String getUrgencyExplanation() { return urgencyExplanation; }
    public void setUrgencyExplanation(String urgencyExplanation) { this.urgencyExplanation = urgencyExplanation; }

    public String getClassificationExplanation() { return classificationExplanation; }
    public void setClassificationExplanation(String classificationExplanation) { this.classificationExplanation = classificationExplanation; }
}