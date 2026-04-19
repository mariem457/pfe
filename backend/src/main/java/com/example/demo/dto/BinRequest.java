package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class BinRequest {

    public String binCode;

    @NotNull(message = "type is required")
    public String type;

    @NotNull(message = "wasteType is required")
    public String wasteType;

    @NotNull(message = "lat is required")
    public Double lat;

    @NotNull(message = "lng is required")
    public Double lng;

    public Double accessLat;
    public Double accessLng;

    public LocalDate installationDate;
    public Boolean isActive;
    public String notes;
}