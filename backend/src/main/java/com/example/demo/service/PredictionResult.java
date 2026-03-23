package com.example.demo.service;

public class PredictionResult {

    private double predictedFillNext;
    private String alertStatus;
    private double priorityScore;
    private boolean shouldCollect;

    public PredictionResult(double predictedFillNext, String alertStatus, double priorityScore, boolean shouldCollect) {
        this.predictedFillNext = predictedFillNext;
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