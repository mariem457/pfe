package com.example.demo.service;

import com.example.demo.dto.routing.RecommendedFuelStationDto;
import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.dto.routing.RoutingDepotDto;
import com.example.demo.dto.routing.RoutingIncidentDto;
import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingTruckDto;
import com.example.demo.entity.Bin;
import com.example.demo.entity.FuelStation;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckIncident;
import com.example.demo.repository.TruckIncidentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoutingPayloadBuilderServiceImpl implements RoutingPayloadBuilderService {

    private static final double DEFAULT_BIN_MAX_CAPACITY_KG = 50.0;

    private final BinPriorityService binPriorityService;
    private final TruckIncidentRepository truckIncidentRepository;
    private final FuelManagementService fuelManagementService;
    private final FuelStationService fuelStationService;

    private final List<RecommendedFuelStationDto> lastRecommendedFuelStations = new ArrayList<>();

    public RoutingPayloadBuilderServiceImpl(BinPriorityService binPriorityService,
                                            TruckIncidentRepository truckIncidentRepository,
                                            FuelManagementService fuelManagementService,
                                            FuelStationService fuelStationService) {
        this.binPriorityService = binPriorityService;
        this.truckIncidentRepository = truckIncidentRepository;
        this.fuelManagementService = fuelManagementService;
        this.fuelStationService = fuelStationService;
    }

    @Override
    public RoutingRequestDto buildRoutingRequest(List<Truck> trucks) {
        lastRecommendedFuelStations.clear();

        RoutingRequestDto request = new RoutingRequestDto();
        request.setDepot(buildDefaultDepot());
        request.setTrafficMode("NORMAL");
        request.setBins(buildRoutingBins());
        request.setTrucks(buildTrucks(trucks));
        request.setActiveIncidents(buildActiveIncidents(trucks));

        System.out.println(
                "Routing payload built | trucks=" + request.getTrucks().size()
                        + " | bins=" + request.getBins().size()
                        + " | activeIncidents=" + request.getActiveIncidents().size()
                        + " | recommendedFuelStations=" + lastRecommendedFuelStations.size()
        );

        return request;
    }

    @Override
    public RoutingRequestDto buildReplanRequest(List<Truck> trucks, List<MissionBin> remainingMissionBins) {
        lastRecommendedFuelStations.clear();

        RoutingRequestDto request = new RoutingRequestDto();
        request.setDepot(buildDefaultDepot());
        request.setTrafficMode("NORMAL");
        request.setBins(buildRoutingBinsFromMissionBins(remainingMissionBins));
        request.setTrucks(buildTrucks(trucks));
        request.setActiveIncidents(buildActiveIncidents(trucks));

        return request;
    }

    @Override
    public List<RecommendedFuelStationDto> getLastRecommendedFuelStations() {
        return new ArrayList<>(lastRecommendedFuelStations);
    }

    private RoutingDepotDto buildDefaultDepot() {
        RoutingDepotDto depot = new RoutingDepotDto();
        depot.setLat(35.5070);
        depot.setLng(11.0700);
        return depot;
    }

    private List<RoutingIncidentDto> buildActiveIncidents(List<Truck> trucks) {
        List<RoutingIncidentDto> incidents = new ArrayList<>();
        Set<Long> refuelRequiredTruckIds = new HashSet<>();

        List<TruckIncident> allActiveIncidents = truckIncidentRepository.findByStatusIn(
                List.of(
                        TruckIncident.IncidentStatus.OPEN,
                        TruckIncident.IncidentStatus.IN_PROGRESS
                )
        );

        for (TruckIncident incident : allActiveIncidents) {
            if (incident.getTruck() == null || incident.getTruck().getId() == null) {
                continue;
            }

            RoutingIncidentDto dto = new RoutingIncidentDto();
            dto.setId(incident.getId());
            dto.setTruckId(incident.getTruck().getId());
            dto.setType(incident.getIncidentType() != null ? incident.getIncidentType().name() : null);
            dto.setSeverity(incident.getSeverity() != null ? incident.getSeverity().name() : null);
            dto.setDescription(incident.getDescription());

            incidents.add(dto);
        }

        for (Truck truck : trucks) {
            if (truck == null || truck.getId() == null) continue;

            if (!fuelManagementService.isFuelCritical(truck)) continue;

            if (refuelRequiredTruckIds.contains(truck.getId())) continue;

            RoutingIncidentDto dto = new RoutingIncidentDto();
            dto.setId(-truck.getId());
            dto.setTruckId(truck.getId());
            dto.setType("REFUEL_REQUIRED");
            dto.setSeverity("CRITICAL");
            dto.setDescription("Truck fuel is critical");

            incidents.add(dto);
            refuelRequiredTruckIds.add(truck.getId());

            FuelStation station = fuelStationService.findNearestCompatibleStation(truck);
            if (station != null && station.getId() != null) {
                RecommendedFuelStationDto recommendation = new RecommendedFuelStationDto();
                recommendation.setTruckId(truck.getId());
                recommendation.setStationId(station.getId());
                recommendation.setStationName(station.getName());
                recommendation.setLat(station.getLat());
                recommendation.setLng(station.getLng());

                lastRecommendedFuelStations.add(recommendation);
            }
        }

        return incidents;
    }

    private List<RoutingBinDto> buildRoutingBins() {
        List<RoutingBinDto> bins = binPriorityService.getPriorityBinsForRouting();

        if (bins == null) return new ArrayList<>();

        for (RoutingBinDto bin : bins) {
            double fillLevel = safeDouble(bin.getFillLevel());
            double estimatedLoadKg = computeEstimatedLoadKg(fillLevel, DEFAULT_BIN_MAX_CAPACITY_KG);
            bin.setEstimatedLoadKg(estimatedLoadKg);
        }

        return bins;
    }

    private List<RoutingBinDto> buildRoutingBinsFromMissionBins(List<MissionBin> missionBins) {
        List<RoutingBinDto> bins = new ArrayList<>();

        if (missionBins == null || missionBins.isEmpty()) return bins;

        for (MissionBin missionBin : missionBins) {
            if (missionBin == null || missionBin.getBin() == null) continue;

            Bin bin = missionBin.getBin();

            RoutingBinDto dto = new RoutingBinDto();
            dto.setId(bin.getId());
            dto.setLat(resolveBinLat(bin));
            dto.setLng(resolveBinLng(bin));

            double fillLevel = missionBin.getTargetFillThreshold() != null
                    ? missionBin.getTargetFillThreshold()
                    : 80.0;

            dto.setFillLevel(fillLevel);
            dto.setPredictedPriority(1.0);
            dto.setEstimatedLoadKg(computeEstimatedLoadKg(fillLevel, DEFAULT_BIN_MAX_CAPACITY_KG));

            bins.add(dto);
        }

        return bins;
    }

    private List<RoutingTruckDto> buildTrucks(List<Truck> trucks) {
        return trucks.stream().map(this::mapTruckToRoutingTruckDto).toList();
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
        double normalized = Math.max(0.0, Math.min(fillLevel, 100.0));
        double estimated = (normalized / 100.0) * maxCapacityKg;
        return Math.max(1.0, Math.round(estimated));
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private double resolveBinLat(Bin bin) {
        return bin.getAccessLat() != null ? bin.getAccessLat() : bin.getLat();
    }

    private double resolveBinLng(Bin bin) {
        return bin.getAccessLng() != null ? bin.getAccessLng() : bin.getLng();
    }
}