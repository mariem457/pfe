package com.example.demo.service;

import com.example.demo.dto.routing.RoutingDepotDto;
import com.example.demo.dto.routing.RoutingIncidentDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingTruckDto;
import com.example.demo.entity.Truck;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingPayloadBuilderServiceImpl implements RoutingPayloadBuilderService {

    private final BinPriorityService binPriorityService;

    public RoutingPayloadBuilderServiceImpl(BinPriorityService binPriorityService) {
        this.binPriorityService = binPriorityService;
    }

    @Override
    public RoutingRequestDto buildRoutingRequest(List<Truck> trucks) {
        RoutingRequestDto request = new RoutingRequestDto();

        request.setDepot(buildDefaultDepot());
        request.setTrafficMode("REAL_TIME");
        request.setBins(binPriorityService.getPriorityBinsForRouting());
        request.setTrucks(buildTrucks(trucks));
        request.setActiveIncidents(buildActiveIncidents());

        return request;
    }

    private RoutingDepotDto buildDefaultDepot() {
        RoutingDepotDto depot = new RoutingDepotDto();
        depot.setLat(35.5070);
        depot.setLng(11.0700);
        return depot;
    }

    private List<RoutingIncidentDto> buildActiveIncidents() {
        return new ArrayList<>();
    }

    private List<RoutingTruckDto> buildTrucks(List<Truck> trucks) {
        return trucks.stream()
                .map(this::mapTruckToRoutingTruckDto)
                .toList();
    }

    private RoutingTruckDto mapTruckToRoutingTruckDto(Truck truck) {
        RoutingTruckDto dto = new RoutingTruckDto();

        dto.setId(truck.getId());
        dto.setLat(truck.getLastKnownLat());
        dto.setLng(truck.getLastKnownLng());
        dto.setRemainingCapacityKg(calculateRemainingCapacity(truck));
        dto.setFuelLevelLiters(toDouble(truck.getFuelLevelLiters()));
        dto.setFuelConsumptionPerKm(toDouble(truck.getFuelConsumptionPerKm()));
        dto.setStatus(truck.getStatus() != null ? truck.getStatus().name() : null);

        return dto;
    }

    private Double calculateRemainingCapacity(Truck truck) {
        BigDecimal maxLoad = truck.getMaxLoadKg() != null ? truck.getMaxLoadKg() : BigDecimal.ZERO;
        BigDecimal currentLoad = truck.getCurrentLoadKg() != null ? truck.getCurrentLoadKg() : BigDecimal.ZERO;

        return maxLoad.subtract(currentLoad).doubleValue();
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}