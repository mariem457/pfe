package com.example.demo.dto.routing;

public class WarningTruckDto {

    private Long truckId;
    private String reason;

    public WarningTruckDto() {
    }

    public Long getTruckId() {
        return truckId;
    }

    public void setTruckId(Long truckId) {
        this.truckId = truckId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}