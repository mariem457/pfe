package com.example.demo.service;

import com.example.demo.dto.routing.RoutingDepotDto;
import com.example.demo.dto.routing.RoutingIncidentDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingTruckDto;
import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.entity.Truck;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingPayloadBuilderServiceImpl implements RoutingPayloadBuilderService {

    private static final double DEFAULT_BIN_MAX_CAPACITY_KG = 50.0;

    private final BinPriorityService binPriorityService;

    public RoutingPayloadBuilderServiceImpl(BinPriorityService binPriorityService) {
        this.binPriorityService = binPriorityService;
    }

    @Override
    public RoutingRequestDto buildRoutingRequest(List<Truck> trucks) {
        RoutingRequestDto request = new RoutingRequestDto();

        request.setDepot(buildDefaultDepot());

        // valeurs supportées côté Python: LIGHT / NORMAL / HEAVY
        request.setTrafficMode("NORMAL");

        request.setBins(buildRoutingBins());
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

    private List<RoutingBinDto> buildRoutingBins() {
        List<RoutingBinDto> bins = binPriorityService.getPriorityBinsForRouting();

        if (bins == null) {
            return new ArrayList<>();
        }

        for (RoutingBinDto bin : bins) {
            double fillLevel = safeDouble(bin.getFillLevel());
            double estimatedLoadKg = computeEstimatedLoadKg(fillLevel, DEFAULT_BIN_MAX_CAPACITY_KG);
            bin.setEstimatedLoadKg(estimatedLoadKg);

            System.out.println(
                "Routing bin " + bin.getId()
                    + " | fillLevel=" + fillLevel
                    + " | estimatedLoadKg=" + estimatedLoadKg
            );
        }

        return bins;
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

    private double computeEstimatedLoadKg(double fillLevel, double maxCapacityKg) {
        double normalizedFillLevel = Math.max(0.0, Math.min(fillLevel, 100.0));
        double estimated = (normalizedFillLevel / 100.0) * maxCapacityKg;

        // minimum 1 kg pour éviter demands = 0 si le bac est sélectionné
        return Math.max(1.0, Math.round(estimated));
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}