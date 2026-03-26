package com.example.demo.dto;

import com.example.demo.entity.Truck;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;

public class TruckStatusUpdateDto {

    private Truck.TruckStatus status;

    @PositiveOrZero(message = "Fuel level cannot be negative")
    private Double fuelLevelLiters;

    @PositiveOrZero(message = "Current load cannot be negative")
    private Double currentLoadKg;

    @Digits(integer = 3, fraction = 6, message = "Latitude format is invalid")
    private Double lastKnownLat;

    @Digits(integer = 3, fraction = 6, message = "Longitude format is invalid")
    private Double lastKnownLng;

    public TruckStatusUpdateDto() {
    }

    public Truck.TruckStatus getStatus() {
        return status;
    }

    public void setStatus(Truck.TruckStatus status) {
        this.status = status;
    }

    public Double getFuelLevelLiters() {
        return fuelLevelLiters;
    }

    public void setFuelLevelLiters(Double fuelLevelLiters) {
        this.fuelLevelLiters = fuelLevelLiters;
    }

    public Double getCurrentLoadKg() {
        return currentLoadKg;
    }

    public void setCurrentLoadKg(Double currentLoadKg) {
        this.currentLoadKg = currentLoadKg;
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
}