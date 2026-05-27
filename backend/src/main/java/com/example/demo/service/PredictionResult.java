package com.example.demo.service;

public class PredictionResult {

    private double predictedFillNext;
    private double predictedHours;
    private String alertStatus;
    private double priorityScore;
    private boolean shouldCollect;

    public PredictionResult() {
    }

    // Constructor jdida lel XGBoost dual-output
    public PredictionResult(
            double predictedFillNext,
            double predictedHours,
            String alertStatus,
            double priorityScore,
            boolean shouldCollect
    ) {
        this.predictedFillNext = predictedFillNext;
        this.predictedHours = predictedHours;
        this.alertStatus = alertStatus;
        this.priorityScore = priorityScore;
        this.shouldCollect = shouldCollect;
    }

    // Constructor 9dima bach ma ykaserch code l9dim
    public PredictionResult(
            double predictedFillNext,
            String alertStatus,
            double priorityScore,
            boolean shouldCollect
    ) {
        this.predictedFillNext = predictedFillNext;
        this.predictedHours = 0.0;
        this.alertStatus = alertStatus;
        this.priorityScore = priorityScore;
        this.shouldCollect = shouldCollect;
    }

    public double getPredictedFillNext() {
        return predictedFillNext;
    }

    public void setPredictedFillNext(double predictedFillNext) {
        this.predictedFillNext = predictedFillNext;
    }

    public double getPredictedHours() {
        return predictedHours;
    }

    public void setPredictedHours(double predictedHours) {
        this.predictedHours = predictedHours;
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public void setAlertStatus(String alertStatus) {
        this.alertStatus = alertStatus;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public boolean isShouldCollect() {
        return shouldCollect;
    }

    public void setShouldCollect(boolean shouldCollect) {
        this.shouldCollect = shouldCollect;
    }
}