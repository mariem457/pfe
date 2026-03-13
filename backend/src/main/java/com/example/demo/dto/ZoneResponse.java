package com.example.demo.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class ZoneResponse {
    public Long id;
    public String name;
    public String description;
    public Double centerLat;
    public Double centerLng;
    public List<LatLngPoint> polygon;
    public OffsetDateTime createdAt;
}