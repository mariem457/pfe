package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateTruckRequest {

    @NotBlank(message = "Truck plate number is required")
    @Size(min = 3, max = 20, message = "Plate number must be between 3 and 20 characters")
    private String plateNumber;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than 0")
    private Double capacityKg;

    @NotNull(message = "Fuel level is required")
    @Min(value = 0, message = "Fuel level cannot be negative")
    @Max(value = 100, message = "Fuel level cannot exceed 100")
    private Integer fuelLevel;

    @NotBlank(message = "Status is required")
    private String status;

    public CreateTruckRequest() {
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public Double getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(Double capacityKg) {
        this.capacityKg = capacityKg;
    }

    public Integer getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(Integer fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}