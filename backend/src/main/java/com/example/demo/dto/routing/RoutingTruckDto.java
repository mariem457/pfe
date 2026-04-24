package com.example.demo.dto.routing;

import java.util.ArrayList;
import java.util.List;

public class RoutingTruckDto {

    private Long id;
    private Double lat;
    private Double lng;
    private Double remainingCapacityKg;
    private Double fuelLevelLiters;
    private Double fuelConsumptionPerKm;
    private String status;
    private List<String> supportedWasteTypes = new ArrayList<>();
    private Long zoneId;

    public RoutingTruckDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getRemainingCapacityKg() {
        return remainingCapacityKg;
    }

    public void setRemainingCapacityKg(Double remainingCapacityKg) {
        this.remainingCapacityKg = remainingCapacityKg;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSupportedWasteTypes() {
        return supportedWasteTypes;
    }

    public void setSupportedWasteTypes(List<String> supportedWasteTypes) {
        this.supportedWasteTypes = supportedWasteTypes;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }
}