package com.example.demo.dto;

import com.example.demo.entity.Truck;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class TruckRequestDto {

    @NotBlank(message = "Truck code is required")
    @Size(max = 50, message = "Truck code must not exceed 50 characters")
    private String truckCode;

    @Size(max = 50, message = "Plate number must not exceed 50 characters")
    private String plateNumber;

    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    private Truck.FuelType fuelType;

    @Positive(message = "Tank capacity must be greater than 0")
    private Double tankCapacityLiters;

    @PositiveOrZero(message = "Fuel level cannot be negative")
    private Double fuelLevelLiters;

    @DecimalMin(value = "0.0", inclusive = false, message = "Fuel consumption per km must be greater than 0")
    private Double fuelConsumptionPerKm;

    @Positive(message = "Max load must be greater than 0")
    private Double maxLoadKg;

    @Positive(message = "Max bin capacity must be greater than 0")
    private Integer maxBinCapacity;

    @PositiveOrZero(message = "Current load cannot be negative")
    private Double currentLoadKg;

    private Truck.TruckStatus status;

    @Digits(integer = 3, fraction = 6, message = "Latitude format is invalid")
    private Double lastKnownLat;

    @Digits(integer = 3, fraction = 6, message = "Longitude format is invalid")
    private Double lastKnownLng;

    private Boolean isActive;

    @Positive(message = "Assigned driver id must be positive")
    private Long assignedDriverId;

    public TruckRequestDto() {
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

    public Truck.FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(Truck.FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public Double getTankCapacityLiters() {
        return tankCapacityLiters;
    }

    public void setTankCapacityLiters(Double tankCapacityLiters) {
        this.tankCapacityLiters = tankCapacityLiters;
    }

    public Double getFuelLevelLiters() {
        return fuelLevelLiters;
    }

    public void setFuelLevelLiters(Double fuelLevelLiters) {
        this.fuelLevelLiters = fuelLevelLiters;
    }

    public Double getFuelConsumptionPerKm() {
        return fuelConsumptionPerKm;
    }

    public void setFuelConsumptionPerKm(Double fuelConsumptionPerKm) {
        this.fuelConsumptionPerKm = fuelConsumptionPerKm;
    }

    public Double getMaxLoadKg() {
        return maxLoadKg;
    }

    public void setMaxLoadKg(Double maxLoadKg) {
        this.maxLoadKg = maxLoadKg;
    }

    public Integer getMaxBinCapacity() {
        return maxBinCapacity;
    }

    public void setMaxBinCapacity(Integer maxBinCapacity) {
        this.maxBinCapacity = maxBinCapacity;
    }

    public Double getCurrentLoadKg() {
        return currentLoadKg;
    }

    public void setCurrentLoadKg(Double currentLoadKg) {
        this.currentLoadKg = currentLoadKg;
    }

    public Truck.TruckStatus getStatus() {
        return status;
    }

    public void setStatus(Truck.TruckStatus status) {
        this.status = status;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Long getAssignedDriverId() {
        return assignedDriverId;
    }

    public void setAssignedDriverId(Long assignedDriverId) {
        this.assignedDriverId = assignedDriverId;
    }
}