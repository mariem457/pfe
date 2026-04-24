package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trucks")
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "truck_code", nullable = false, unique = true, length = 50)
    private String truckCode;

    @Column(name = "plate_number", unique = true, length = 30)
    private String plateNumber;

    @Column(length = 80)
    private String model;

    @Column(length = 80)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Column(name = "tank_capacity_liters", precision = 10, scale = 2)
    private BigDecimal tankCapacityLiters;

    @Column(name = "fuel_level_liters", precision = 10, scale = 2)
    private BigDecimal fuelLevelLiters;

    @Column(name = "fuel_consumption_per_km", precision = 10, scale = 4)
    private BigDecimal fuelConsumptionPerKm;

    @Column(name = "max_load_kg", precision = 10, scale = 2)
    private BigDecimal maxLoadKg;

    @Column(name = "max_bin_capacity")
    private Integer maxBinCapacity;

    @Column(name = "current_load_kg", precision = 10, scale = 2, nullable = false)
    private BigDecimal currentLoadKg = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TruckStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    private Driver assignedDriver;

    @Column(name = "last_known_lat")
    private Double lastKnownLat;

    @Column(name = "last_known_lng")
    private Double lastKnownLng;

    @Column(name = "last_status_update")
    private OffsetDateTime lastStatusUpdate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "truck", fetch = FetchType.LAZY)
    private List<Mission> missions = new ArrayList<>();

    @OneToMany(mappedBy = "truck", fetch = FetchType.LAZY)
    private List<TruckIncident> incidents = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @OneToMany(mappedBy = "truck", fetch = FetchType.LAZY)
    private List<RoutePlan> routePlans = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "truck_supported_waste_types",
            joinColumns = @JoinColumn(name = "truck_id")
    )
    @Column(name = "waste_type", length = 20)
    @Enumerated(EnumType.STRING)
    private List<Bin.WasteType> supportedWasteTypes = new ArrayList<>();

    public Truck() {
    }

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastStatusUpdate == null) {
            this.lastStatusUpdate = now;
        }
        if (this.currentLoadKg == null) {
            this.currentLoadKg = BigDecimal.ZERO;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public enum FuelType {
        DIESEL, ESSENCE, ELECTRIC, HYBRID
    }

    public enum TruckStatus {
        AVAILABLE,
        ON_MISSION,
        BREAKDOWN,
        MAINTENANCE,
        REFUELING,
        UNAVAILABLE,
        OUT_OF_SERVICE
    }

    public Long getId() {
        return id;
    }

    public String getTruckCode() {
        return truckCode;
    }

    public void setTruckCode(String truckCode) {
        this.truckCode = truckCode;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public BigDecimal getTankCapacityLiters() {
        return tankCapacityLiters;
    }

    public void setTankCapacityLiters(BigDecimal tankCapacityLiters) {
        this.tankCapacityLiters = tankCapacityLiters;
    }

    public BigDecimal getFuelLevelLiters() {
        return fuelLevelLiters;
    }

    public void setFuelLevelLiters(BigDecimal fuelLevelLiters) {
        this.fuelLevelLiters = fuelLevelLiters;
    }

    public BigDecimal getFuelConsumptionPerKm() {
        return fuelConsumptionPerKm;
    }

    public void setFuelConsumptionPerKm(BigDecimal fuelConsumptionPerKm) {
        this.fuelConsumptionPerKm = fuelConsumptionPerKm;
    }

    public BigDecimal getMaxLoadKg() {
        return maxLoadKg;
    }

    public void setMaxLoadKg(BigDecimal maxLoadKg) {
        this.maxLoadKg = maxLoadKg;
    }

    public Integer getMaxBinCapacity() {
        return maxBinCapacity;
    }

    public void setMaxBinCapacity(Integer maxBinCapacity) {
        this.maxBinCapacity = maxBinCapacity;
    }

    public BigDecimal getCurrentLoadKg() {
        return currentLoadKg;
    }

    public void setCurrentLoadKg(BigDecimal currentLoadKg) {
        this.currentLoadKg = currentLoadKg;
    }

    public TruckStatus getStatus() {
        return status;
    }

    public void setStatus(TruckStatus status) {
        this.status = status;
    }

    public Driver getAssignedDriver() {
        return assignedDriver;
    }

    public void setAssignedDriver(Driver assignedDriver) {
        this.assignedDriver = assignedDriver;
    }

    public Double getLastKnownLat() {
        return lastKnownLat;
    }

    public void setLastKnownLat(Double lastKnownLat) {
        this.lastKnownLat = lastKnownLat;
    }

    public Double getLastKnownLng() {
        return lastKnownLng;
    }

    public void setLastKnownLng(Double lastKnownLng) {
        this.lastKnownLng = lastKnownLng;
    }

    public OffsetDateTime getLastStatusUpdate() {
        return lastStatusUpdate;
    }

    public void setLastStatusUpdate(OffsetDateTime lastStatusUpdate) {
        this.lastStatusUpdate = lastStatusUpdate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Mission> getMissions() {
        return missions;
    }

    public List<TruckIncident> getIncidents() {
        return incidents;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public List<RoutePlan> getRoutePlans() {
        return routePlans;
    }

    public List<Bin.WasteType> getSupportedWasteTypes() {
        return supportedWasteTypes;
    }

    public void setSupportedWasteTypes(List<Bin.WasteType> supportedWasteTypes) {
        this.supportedWasteTypes = supportedWasteTypes;
    }
}