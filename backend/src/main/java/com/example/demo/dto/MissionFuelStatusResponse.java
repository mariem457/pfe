package com.example.demo.dto;

import com.example.demo.dto.routing.RecommendedFuelStationDto;

public class MissionFuelStatusResponse {

    private Long missionId;
    private Long truckId;
    private String fuelStatus;
    private Double safeAutonomyKm;
    private Double distanceToNearestStationKm;
    private Boolean refuelStopInserted;
    private RecommendedFuelStationDto recommendedFuelStation;
    private String message;

    // Explainability FR
    private String decisionReason;
    private Double alertThresholdKm;
    private Double criticalFuelThresholdLiters;
    private Double triggerDistanceKm;
    private Integer suggestedInsertionOrder;
    private String routeInsertionReason;

    public MissionFuelStatusResponse() {
    }

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public Long getTruckId() {
        return truckId;
    }

    public void setTruckId(Long truckId) {
        this.truckId = truckId;
    }

    public String getFuelStatus() {
        return fuelStatus;
    }

    public void setFuelStatus(String fuelStatus) {
        this.fuelStatus = fuelStatus;
    }

    public Double getSafeAutonomyKm() {
        return safeAutonomyKm;
    }

    public void setSafeAutonomyKm(Double safeAutonomyKm) {
        this.safeAutonomyKm = safeAutonomyKm;
    }

    public Double getDistanceToNearestStationKm() {
        return distanceToNearestStationKm;
    }

    public void setDistanceToNearestStationKm(Double distanceToNearestStationKm) {
        this.distanceToNearestStationKm = distanceToNearestStationKm;
    }

    public Boolean getRefuelStopInserted() {
        return refuelStopInserted;
    }

    public void setRefuelStopInserted(Boolean refuelStopInserted) {
        this.refuelStopInserted = refuelStopInserted;
    }

    public RecommendedFuelStationDto getRecommendedFuelStation() {
        return recommendedFuelStation;
    }

    public void setRecommendedFuelStation(RecommendedFuelStationDto recommendedFuelStation) {
        this.recommendedFuelStation = recommendedFuelStation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Double getAlertThresholdKm() {
        return alertThresholdKm;
    }

    public void setAlertThresholdKm(Double alertThresholdKm) {
        this.alertThresholdKm = alertThresholdKm;
    }

    public Double getCriticalFuelThresholdLiters() {
        return criticalFuelThresholdLiters;
    }

    public void setCriticalFuelThresholdLiters(Double criticalFuelThresholdLiters) {
        this.criticalFuelThresholdLiters = criticalFuelThresholdLiters;
    }

    public Double getTriggerDistanceKm() {
        return triggerDistanceKm;
    }

    public void setTriggerDistanceKm(Double triggerDistanceKm) {
        this.triggerDistanceKm = triggerDistanceKm;
    }

    public Integer getSuggestedInsertionOrder() {
        return suggestedInsertionOrder;
    }

    public void setSuggestedInsertionOrder(Integer suggestedInsertionOrder) {
        this.suggestedInsertionOrder = suggestedInsertionOrder;
    }

    public String getRouteInsertionReason() {
        return routeInsertionReason;
    }

    public void setRouteInsertionReason(String routeInsertionReason) {
        this.routeInsertionReason = routeInsertionReason;
    }
}