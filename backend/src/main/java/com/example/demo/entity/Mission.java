package com.example.demo.entity;

import jakarta.persistence.*;
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

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) status = "CREATED";
        if (priority == null) priority = "NORMAL";
    }

    // getters/setters (IDE generate)
}