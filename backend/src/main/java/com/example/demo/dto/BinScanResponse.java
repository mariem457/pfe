package com.example.demo.dto;

public class BinScanResponse {

    private Long missionBinId;
    private String binCode;
    private boolean collected;
    private String assignmentStatus;
    private String message;

    public BinScanResponse() {}

    public BinScanResponse(Long missionBinId, String binCode, boolean collected, String assignmentStatus, String message) {
        this.missionBinId = missionBinId;
        this.binCode = binCode;
        this.collected = collected;
        this.assignmentStatus = assignmentStatus;
        this.message = message;
    }

    public Long getMissionBinId() {
        return missionBinId;
    }

    public void setMissionBinId(Long missionBinId) {
        this.missionBinId = missionBinId;
    }

    public String getBinCode() {
        return binCode;
    }

    public void setBinCode(String binCode) {
        this.binCode = binCode;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}