package com.example.demo.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class BinResponse {
    public Long id;
    public String binCode;
    public String type;
    public Long zoneId;
    public Double lat;
    public Double lng;
    public LocalDate installationDate;
    public Boolean isActive;
    public String notes;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}