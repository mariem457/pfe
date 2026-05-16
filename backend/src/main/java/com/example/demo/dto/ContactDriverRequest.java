package com.example.demo.dto;

public class ContactDriverRequest {
    private Long incidentId;
    private String message;

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}