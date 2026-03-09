package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class BinRequest {

    // optional: backend ينجم يولدها وحدو
    public String binCode;

    @NotNull(message = "type is required")
    public String type;

    // optional
    public Long zoneId;

    @NotNull(message = "lat is required")
    public Double lat;

    @NotNull(message = "lng is required")
    public Double lng;

    // optional: backend يحط today إذا null
    public LocalDate installationDate;

    // optional
    public Boolean isActive;

    // optional
    public String notes;
}