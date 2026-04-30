package com.example.demo.dto.routing;

import java.util.ArrayList;
import java.util.List;

public class RoutingDisposalSiteDto {

    private Long id;
    private String name;
    private Double lat;
    private Double lng;
    private List<String> acceptedWasteTypes = new ArrayList<>();

    public RoutingDisposalSiteDto() {
    }

    public RoutingDisposalSiteDto(Long id, String name, Double lat, Double lng, List<String> acceptedWasteTypes) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.acceptedWasteTypes = acceptedWasteTypes != null ? acceptedWasteTypes : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public List<String> getAcceptedWasteTypes() {
        return acceptedWasteTypes;
    }

    public void setAcceptedWasteTypes(List<String> acceptedWasteTypes) {
        this.acceptedWasteTypes = acceptedWasteTypes;
    }
}