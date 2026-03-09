package com.example.demo.dto;

public class MissionBinAddRequest {
    private Long binId;
    private int visitOrder;
    private Short targetFillThreshold;
    private String assignedReason;
	public Long getBinId() {
		return binId;
	}
	public void setBinId(Long binId) {
		this.binId = binId;
	}
	public int getVisitOrder() {
		return visitOrder;
	}
	public void setVisitOrder(int visitOrder) {
		this.visitOrder = visitOrder;
	}
	public Short getTargetFillThreshold() {
		return targetFillThreshold;
	}
	public void setTargetFillThreshold(Short targetFillThreshold) {
		this.targetFillThreshold = targetFillThreshold;
	}
	public String getAssignedReason() {
		return assignedReason;
	}
	public void setAssignedReason(String assignedReason) {
		this.assignedReason = assignedReason;
	}

    // getters/setters
}