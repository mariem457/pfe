package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class BinRequest {

    public String binCode;

    @NotNull(message = "type is required")
    public String type;

    @NotNull(message = "lat is required")
    public Double lat;

    @NotNull(message = "lng is required")
    public Double lng;

    public LocalDate installationDate;
    public Boolean isActive;
    public String notes;
}