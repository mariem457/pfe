package com.example.demo.dto.routing;

public class RoutingBinDto {

    private Long id;
    private Double lat;
    private Double lng;
    private Double fillLevel;
    private Double predictedPriority;
    private Double estimatedLoadKg;

    public RoutingBinDto() {
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

    public Double getFillLevel() {
        return fillLevel;
    }

    public void setFillLevel(Double fillLevel) {
        this.fillLevel = fillLevel;
    }

    public Double getPredictedPriority() {
        return predictedPriority;
    }

    public void setPredictedPriority(Double predictedPriority) {
        this.predictedPriority = predictedPriority;
    }

    public Double getEstimatedLoadKg() {
        return estimatedLoadKg;
    }

    public void setEstimatedLoadKg(Double estimatedLoadKg) {
        this.estimatedLoadKg = estimatedLoadKg;
    }
}