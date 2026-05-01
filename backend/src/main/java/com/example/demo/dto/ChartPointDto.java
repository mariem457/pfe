package com.example.demo.dto;

public class ChartPointDto {

    private String label;
    private double value;

    public ChartPointDto() {
    }

    public ChartPointDto(String label, double value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}