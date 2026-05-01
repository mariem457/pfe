package com.example.demo.dto;

import com.example.demo.entity.Truck;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TruckResponseDto {

    private Long id;
    private String truckCode;
    private String plateNumber;
    private String model;
    private String brand;
    private Truck.FuelType fuelType;
    private BigDecimal tankCapacityLiters;
    private BigDecimal fuelLevelLiters;
    private BigDecimal fuelConsumptionPerKm;
    private BigDecimal maxLoadKg;
    private Integer maxBinCapacity;
    private BigDecimal currentLoadKg;
    private Truck.TruckStatus status;
    private Long assignedDriverId;
    private String assignedDriverName;
    private Double lastKnownLat;
    private Double lastKnownLng;
    private OffsetDateTime lastStatusUpdate;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public TruckResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public String getTruckCode() {
        return truckCode;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Truck.FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(Truck.FuelType fuelType) {
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

    public Truck.TruckStatus getStatus() {
        return status;
    }

    public void setStatus(Truck.TruckStatus status) {
        this.status = status;
    }

    public Long getAssignedDriverId() {
        return assignedDriverId;
    }

    public void setAssignedDriverId(Long assignedDriverId) {
        this.assignedDriverId = assignedDriverId;
    }

    public String getAssignedDriverName() {
        return assignedDriverName;
    }

    public void setAssignedDriverName(String assignedDriverName) {
        this.assignedDriverName = assignedDriverName;
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

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}