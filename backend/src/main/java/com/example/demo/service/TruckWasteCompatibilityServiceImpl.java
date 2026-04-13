package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.dto.routing.RoutingTruckDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TruckWasteCompatibilityServiceImpl implements TruckWasteCompatibilityService {

    @Override
    public boolean canTruckCollectBin(RoutingTruckDto truck, RoutingBinDto bin) {
        if (truck == null || bin == null) {
            return false;
        }

        if (bin.getWasteType() == null || bin.getWasteType().isBlank()) {
            return false;
        }

        if (truck.getSupportedWasteTypes() == null || truck.getSupportedWasteTypes().isEmpty()) {
            return false;
        }

        String wasteType = normalize(bin.getWasteType());

        return truck.getSupportedWasteTypes().stream()
                .filter(type -> type != null && !type.isBlank())
                .map(this::normalize)
                .anyMatch(type -> type.equals(wasteType));
    }

    @Override
    public boolean hasAtLeastOneCompatibleTruck(List<RoutingTruckDto> trucks, RoutingBinDto bin) {
        if (trucks == null || trucks.isEmpty() || bin == null) {
            return false;
        }

        return trucks.stream().anyMatch(truck -> canTruckCollectBin(truck, bin));
    }

    @Override
    public String explainTruckCompatibility(RoutingTruckDto truck, RoutingBinDto bin) {
        if (truck == null || truck.getId() == null) {
            return "Aucun camion valide n'est disponible pour évaluer la compatibilité.";
        }

        if (bin == null || bin.getWasteType() == null || bin.getWasteType().isBlank()) {
            return "Le type de déchet du bac n'est pas renseigné, la compatibilité ne peut pas être vérifiée.";
        }

        if (canTruckCollectBin(truck, bin)) {
            return "Le camion " + truck.getId()
                    + " est compatible avec le type de bac " + normalize(bin.getWasteType()) + ".";
        }

        return "Le camion " + truck.getId()
                + " n'est pas compatible avec le type de bac " + normalize(bin.getWasteType()) + ".";
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}