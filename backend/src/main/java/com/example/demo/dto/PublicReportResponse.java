package com.example.demo.dto;

import java.time.OffsetDateTime;

public class PublicReportResponse {
    private Long id;
    private String reportCode;
    private String reportType;
    private String photoUrl;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private String status;
    private String priority;
    private String assignedDriverName;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
    private String resolvedNote;
    private Long duplicateOfReportId;
    private String qualificationNote;
    private String decisionReason;
	public Long getDuplicateOfReportId() {
		return duplicateOfReportId;
	}
	public void setDuplicateOfReportId(Long duplicateOfReportId) {
		this.duplicateOfReportId = duplicateOfReportId;
	}
	public String getQualificationNote() {
		return qualificationNote;
	}
	public void setQualificationNote(String qualificationNote) {
		this.qualificationNote = qualificationNote;
	}
	public String getDecisionReason() {
		return decisionReason;
	}
	public void setDecisionReason(String decisionReason) {
		this.decisionReason = decisionReason;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getReportCode() {
		return reportCode;
	}
	public void setReportCode(String reportCode) {
		this.reportCode = reportCode;
	}
	public String getReportType() {
		return reportType;
	}
	public void setReportType(String reportType) {
		this.reportType = reportType;
	}
	public String getPhotoUrl() {
		return photoUrl;
	}
	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public Double getLatitude() {
		return latitude;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public String getAssignedDriverName() {
		return assignedDriverName;
	}
	public void setAssignedDriverName(String assignedDriverName) {
		this.assignedDriverName = assignedDriverName;
	}
	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public OffsetDateTime getResolvedAt() {
		return resolvedAt;
	}
	public void setResolvedAt(OffsetDateTime resolvedAt) {
		this.resolvedAt = resolvedAt;
	}
	public String getResolvedNote() {
		return resolvedNote;
	}
	public void setResolvedNote(String resolvedNote) {
		this.resolvedNote = resolvedNote;
	}

    // getters and setters
}