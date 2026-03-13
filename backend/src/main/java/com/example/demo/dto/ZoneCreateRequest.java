package com.example.demo.dto;

import java.util.List;

public class ZoneCreateRequest {
    public String name;
    public String description;
    public Double centerLat;
    public Double centerLng;
    public List<LatLngPoint> polygon;
}