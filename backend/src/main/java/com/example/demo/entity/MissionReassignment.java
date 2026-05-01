package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "mission_reassignments")
public class MissionReassignment {

    public enum ReassignmentReason {
        BREAKDOWN,
        TRAFFIC,
        FUEL_LOW,
        DELAY,
        MANUAL,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_mission_id", nullable = false)
    private Mission originalMission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_truck_id")
    private Truck sourceTruck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_truck_id")
    private Truck targetTruck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id")
    private Bin bin;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReassignmentReason reason;

    @Column(name = "reassigned_at", nullable = false, updatable = false)
    private OffsetDateTime reassignedAt;

    @Column(name = "algorithm_version", length = 50)
    private String algorithmVersion;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist() {
        if (this.reassignedAt == null) {
            this.reassignedAt = OffsetDateTime.now();
        }
    }

    public MissionReassignment() {
    }

    public Long getId() {
        return id;
    }

    public Mission getOriginalMission() {
        return originalMission;
    }

    public void setOriginalMission(Mission originalMission) {
        this.originalMission = originalMission;
    }

    public Truck getSourceTruck() {
        return sourceTruck;
    }

    public void setSourceTruck(Truck sourceTruck) {
        this.sourceTruck = sourceTruck;
    }

    public Truck getTargetTruck() {
        return targetTruck;
    }

    public void setTargetTruck(Truck targetTruck) {
        this.targetTruck = targetTruck;
    }

    public Bin getBin() {
        return bin;
    }

    public void setBin(Bin bin) {
        this.bin = bin;
    }

    public ReassignmentReason getReason() {
        return reason;
    }

    public void setReason(ReassignmentReason reason) {
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