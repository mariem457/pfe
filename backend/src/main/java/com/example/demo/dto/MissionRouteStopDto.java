package com.example.demo.dto;

public class MissionRouteStopDto {

    private Integer stopOrder;
    private String stopType;
    private Long binId;
    private Long fuelStationId;
    private String fuelStationName;
    private Double lat;
    private Double lng;

    public MissionRouteStopDto() {
    }

    public Integer getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Integer stopOrder) {
        this.stopOrder = stopOrder;
    }

    public String getStopType() {
        return stopType;
    }

    public void setStopType(String stopType) {
        this.stopType = stopType;
    }

    public Long getBinId() {
        return binId;
    }

    public void setBinId(Long binId) {
        this.binId = binId;
    }

    public Long getFuelStationId() {
        return fuelStationId;
    }

    public void setFuelStationId(Long fuelStationId) {
        this.fuelStationId = fuelStationId;
    }

    public String getFuelStationName() {
        return fuelStationName;
    }

    public void setFuelStationName(String fuelStationName) {
        this.fuelStationName = fuelStationName;
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