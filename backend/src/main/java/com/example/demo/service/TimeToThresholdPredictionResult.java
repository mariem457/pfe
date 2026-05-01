// TimeToThresholdPredictionResult.java
package com.example.demo.service;

public class TimeToThresholdPredictionResult {

    private double predictedHours;
    private String alertStatus;
    private double priorityScore;
    private boolean shouldCollect;

    public TimeToThresholdPredictionResult(
            double predictedHours,
            String alertStatus,
            double priorityScore,
            boolean shouldCollect
    ) {
        this.predictedHours = predictedHours;
        this.alertStatus = alertStatus;
        this.priorityScore = priorityScore;
        this.shouldCollect = shouldCollect;
    }

    public double getPredictedHours() {
        return predictedHours;
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public boolean isShouldCollect() {
        return shouldCollect;
    }
}