package com.example.demo.service;

public class PredictionResult {

    private double predictedFillNext;
    private String alertStatus;

    public PredictionResult(double predictedFillNext, String alertStatus) {
        this.predictedFillNext = predictedFillNext;
        this.alertStatus = alertStatus;
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
}