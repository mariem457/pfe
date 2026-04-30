package com.example.demo.dto;

public class TruckDashboardItemResponse {

    private Long driverId;
    private String truckCode;
    private String driverName;
    private Double lat;
    private Double lng;
    private String locationLabel;
    private Integer progress;
    private Integer collectedBins;
    private Integer remainingBins;
    private Integer fuelLevel;
    private Integer etaMinutes;
    private Boolean active;
    private String truckStatus;
    private Long currentMissionId;

    public TruckDashboardItemResponse() {
    }

    public TruckDashboardItemResponse(
            Long driverId,
            String truckCode,
            String driverName,
            Double lat,
            Double lng,
            String locationLabel,
            Integer progress,
            Integer collectedBins,
            Integer remainingBins,
            Integer fuelLevel,
            Integer etaMinutes,
            Boolean active,
            String truckStatus,
            Long currentMissionId
    ) {
        this.driverId = driverId;
        this.truckCode = truckCode;
        this.driverName = driverName;
        this.lat = lat;
        this.lng = lng;
        this.locationLabel = locationLabel;
        this.progress = progress;
        this.collectedBins = collectedBins;
        this.remainingBins = remainingBins;
        this.fuelLevel = fuelLevel;
        this.etaMinutes = etaMinutes;
        this.active = active;
        this.truckStatus = truckStatus;
        this.currentMissionId = currentMissionId;
    }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public String getTruckCode() { return truckCode; }
    public void setTruckCode(String truckCode) { this.truckCode = truckCode; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Integer getCollectedBins() { return collectedBins; }
    public void setCollectedBins(Integer collectedBins) { this.collectedBins = collectedBins; }

    public Integer getRemainingBins() { return remainingBins; }
    public void setRemainingBins(Integer remainingBins) { this.remainingBins = remainingBins; }

    public Integer getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(Integer fuelLevel) { this.fuelLevel = fuelLevel; }

    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getTruckStatus() { return truckStatus; }
    public void setTruckStatus(String truckStatus) { this.truckStatus = truckStatus; }

    public Long getCurrentMissionId() { return currentMissionId; }
    public void setCurrentMissionId(Long currentMissionId) { this.currentMissionId = currentMissionId; }
}