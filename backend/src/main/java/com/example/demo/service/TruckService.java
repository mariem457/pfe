package com.example.demo.service;

import com.example.demo.dto.TruckRequestDto;
import com.example.demo.dto.TruckResponseDto;
import com.example.demo.dto.TruckStatusUpdateDto;

import java.util.List;

public interface TruckService {

    TruckResponseDto createTruck(TruckRequestDto request);

    TruckResponseDto updateTruck(Long truckId, TruckRequestDto request);

    TruckResponseDto updateTruckStatus(Long truckId, TruckStatusUpdateDto request);

    TruckResponseDto getTruckById(Long truckId);

    List<TruckResponseDto> getAllTrucks();

    List<TruckResponseDto> getActiveTrucks();

    void deactivateTruck(Long truckId);
}