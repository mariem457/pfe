package com.example.demo.dto;

import jakarta.validation.constraints.Size;

public class ZoneUpdateRequest {
    @Size(max = 80)
    public String name;

    public String description;
    public Double centerLat;
    public Double centerLng;
}