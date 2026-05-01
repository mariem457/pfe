package com.example.demo.dto.routing;

public class RouteCoordinateDto {

    private Double lat;
    private Double lng;

    public RouteCoordinateDto() {
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
}