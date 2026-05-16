package com.example.demo.dto;

import java.time.Instant;

public class MissionRealtimeEvent {

    private String type;
    private Long missionId;
    private Long missionBinId;
    private Long binId;
    private String binCode;
    private String status;
    private String missionStatusDetail;
    private Long alertId;
    private Long oldMissionId;
    private Long newMissionId;
    private Integer collectedCount;
    private Integer totalBins;
    private String message;
    private Instant timestamp;

    public MissionRealtimeEvent() {
        this.timestamp = Instant.now();
    }

    public static MissionRealtimeEvent of(String type, Long missionId) {
        MissionRealtimeEvent event = new MissionRealtimeEvent();
        event.setType(type);
        event.setMissionId(missionId);
        return event;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getMissionId() { return missionId; }
    public void setMissionId(Long missionId) { this.missionId = missionId; }

    public Long getMissionBinId() { return missionBinId; }
    public void setMissionBinId(Long missionBinId) { this.missionBinId = missionBinId; }

    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }

    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMissionStatusDetail() { return missionStatusDetail; }
    public void setMissionStatusDetail(String missionStatusDetail) { this.missionStatusDetail = missionStatusDetail; }

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }

    public Long getOldMissionId() { return oldMissionId; }
    public void setOldMissionId(Long oldMissionId) { this.oldMissionId = oldMissionId; }

    public Long getNewMissionId() { return newMissionId; }
    public void setNewMissionId(Long newMissionId) { this.newMissionId = newMissionId; }

    public Integer getCollectedCount() { return collectedCount; }
    public void setCollectedCount(Integer collectedCount) { this.collectedCount = collectedCount; }

    public Integer getTotalBins() { return totalBins; }
    public void setTotalBins(Integer totalBins) { this.totalBins = totalBins; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}