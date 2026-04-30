package com.example.demo.dto;

public class DriverBinDto {
	  private Long missionId;
    private Long missionBinId;
    private Long binId;
    private String binCode;
    private Double lat;
    private Double lng;
    private Integer visitOrder;
    private Boolean collected;
    private String assignmentStatus;
    private String wasteType;
  

    public DriverBinDto() {
    }

    public DriverBinDto(
            Long missionId,
            Long missionBinId,
            Long binId,
            String binCode,
            Double lat,
            Double lng,
            Integer visitOrder,
            Boolean collected,
            String assignmentStatus,
            String wasteType
    ) {
        this.missionId = missionId;
        this.missionBinId = missionBinId;
        this.binId = binId;
        this.binCode = binCode;
        this.lat = lat;
        this.lng = lng;
        this.visitOrder = visitOrder;
        this.collected = collected;
        this.assignmentStatus = assignmentStatus;
        this.wasteType = wasteType;
    }
    
    
    
    
    
    
    
    public DriverBinDto(
            
            Long missionBinId,
            Long binId,
            String binCode,
            Double lat,
            Double lng,
            Integer visitOrder,
            Boolean collected,
            String assignmentStatus,
            String wasteType
    ) {
       
        this.missionBinId = missionBinId;
        this.binId = binId;
        this.binCode = binCode;
        this.lat = lat;
        this.lng = lng;
        this.visitOrder = visitOrder;
        this.collected = collected;
        this.assignmentStatus = assignmentStatus;
        this.wasteType = wasteType;
    }

    public Long getMissionBinId() {
        return missionBinId;
    }

    public void setMissionBinId(Long missionBinId) {
        this.missionBinId = missionBinId;
    }

    public Long getBinId() {
        return binId;
    }

    public void setBinId(Long binId) {
        this.binId = binId;
    }

    public String getBinCode() {
        return binCode;
    }

    public void setBinCode(String binCode) {
        this.binCode = binCode;
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

    public Integer getVisitOrder() {
        return visitOrder;
    }

    public void setVisitOrder(Integer visitOrder) {
        this.visitOrder = visitOrder;
    }

    public Boolean getCollected() {
        return collected;
    }

    public void setCollected(Boolean collected) {
        this.collected = collected;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public String getWasteType() {
        return wasteType;
    }

    public void setWasteType(String wasteType) {
        this.wasteType = wasteType;
    }
    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }
}