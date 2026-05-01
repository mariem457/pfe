package com.example.demo.dto;

import java.time.OffsetDateTime;

public class MissionReassignmentResponseDto {

    private Long id;
    private Long originalMissionId;
    private Long sourceTruckId;
    private Long targetTruckId;
    private Long binId;
    private String reason;
    private OffsetDateTime reassignedAt;
    private String algorithmVersion;
    private String notes;

    public MissionReassignmentResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOriginalMissionId() {
        return originalMissionId;
    }

    public void setOriginalMissionId(Long originalMissionId) {
        this.originalMissionId = originalMissionId;
    }

    public Long getSourceTruckId() {
        return sourceTruckId;
    }

    public void setSourceTruckId(Long sourceTruckId) {
        this.sourceTruckId = sourceTruckId;
    }

    public Long getTargetTruckId() {
        return targetTruckId;
    }

    public void setTargetTruckId(Long targetTruckId) {
        this.targetTruckId = targetTruckId;
    }

    public Long getBinId() {
        return binId;
    }

    public void setBinId(Long binId) {
        this.binId = binId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getReassignedAt() {
        return reassignedAt;
    }

    public void setReassignedAt(OffsetDateTime reassignedAt) {
        this.reassignedAt = reassignedAt;
    }

    public String getAlgorithmVersion() {
        return algorithmVersion;
    }

    public void setAlgorithmVersion(String algorithmVersion) {
        this.algorithmVersion = algorithmVersion;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}