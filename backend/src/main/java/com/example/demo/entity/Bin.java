package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "bins")
public class Bin {

    public enum BinType { REAL, SIM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bin_code", nullable = false, unique = true, length = 30)
    private String binCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BinType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "access_lat")
    private Double accessLat;

    @Column(name = "access_lng")
    private Double accessLng;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }

    public BinType getType() { return type; }
    public void setType(BinType type) { this.type = type; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Double getAccessLat() { return accessLat; }
    public void setAccessLat(Double accessLat) { this.accessLat = accessLat; }

    public Double getAccessLng() { return accessLng; }
    public void setAccessLng(Double accessLng) { this.accessLng = accessLng; }

    public LocalDate getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}