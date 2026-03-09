package com.example.demo.dto;

import java.time.Instant;

public class AlertResponse {
    private Long id;
    private Long binId;
    private String binCode;
    private Long telemetryId;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private Instant createdAt;
    private boolean resolved;
    private Instant resolvedAt;
    private Long resolvedByUserId;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
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
	public Long getTelemetryId() {
		return telemetryId;
	}
	public void setTelemetryId(Long telemetryId) {
		this.telemetryId = telemetryId;
	}
	public String getAlertType() {
		return alertType;
	}
	public void setAlertType(String alertType) {
		this.alertType = alertType;
	}
	public String getSeverity() {
		return severity;
	}
	public void setSeverity(String severity) {
		this.severity = severity;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Instant getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
	public boolean isResolved() {
		return resolved;
	}
	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}
	public Instant getResolvedAt() {
		return resolvedAt;
	}
	public void setResolvedAt(Instant resolvedAt) {
		this.resolvedAt = resolvedAt;
	}
	public Long getResolvedByUserId() {
		return resolvedByUserId;
	}
	public void setResolvedByUserId(Long resolvedByUserId) {
		this.resolvedByUserId = resolvedByUserId;
	}

   
}