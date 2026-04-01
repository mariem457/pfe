package com.example.demo.service;

import com.example.demo.dto.IncidentStatusUpdateDto;
import com.example.demo.dto.TruckIncidentRequestDto;
import com.example.demo.dto.TruckIncidentResponseDto;

import java.util.List;

public interface TruckIncidentService {

    TruckIncidentResponseDto createIncident(TruckIncidentRequestDto request);

    TruckIncidentResponseDto updateIncidentStatus(Long incidentId, IncidentStatusUpdateDto request);

    TruckIncidentResponseDto getIncidentById(Long incidentId);

    List<TruckIncidentResponseDto> getAllIncidents();

    List<TruckIncidentResponseDto> getIncidentsByTruck(Long truckId);

    List<TruckIncidentResponseDto> getOpenIncidents();
}