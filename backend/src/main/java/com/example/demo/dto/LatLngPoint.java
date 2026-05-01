package com.example.demo.dto;

public class LatLngPoint {
    public Double lat;
    public Double lng;

    public LatLngPoint() {
    }

    public LatLngPoint(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
}