package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;
import com.example.demo.entity.Alert;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MunicipalExceptionAlertService {

    private static final int FULL_THRESHOLD = 95;
    private static final int MIN_BINS_FOR_EXCEPTION = 3;

    private final BinRepository binRepository;
    private final BinTelemetryRepository telemetryRepository;
    private final AlertRepository alertRepository;
    private final AlertRealtimeService alertRealtimeService;
    private final AlertService alertService;
    private final CollectionScheduleService collectionScheduleService;

    public MunicipalExceptionAlertService(
            BinRepository binRepository,
            BinTelemetryRepository telemetryRepository,
            AlertRepository alertRepository,
            AlertRealtimeService alertRealtimeService,
            AlertService alertService,
            CollectionScheduleService collectionScheduleService
    ) {
        this.binRepository = binRepository;
        this.telemetryRepository = telemetryRepository;
        this.alertRepository = alertRepository;
        this.alertRealtimeService = alertRealtimeService;
        this.alertService = alertService;
        this.collectionScheduleService = collectionScheduleService;
    }

    public void evaluateAfterTelemetry(Bin triggerBin) {
        if (triggerBin == null || triggerBin.getZone() == null || triggerBin.getWasteType() == null) return;

        Long zoneId = triggerBin.getZone().getId();
        String wasteType = triggerBin.getWasteType().name();

        RoutingBinDto scheduleDto = new RoutingBinDto();
        scheduleDto.setWasteType(wasteType);

        if (collectionScheduleService.isCollectionAllowed(scheduleDto)) {
            return;
        }

        Alert existingAlert = alertRepository.findOpenMunicipalException(
                "MUNICIPAL_EXCEPTION",
                zoneId,
                "SCHEDULE_EXCEPTION",
                wasteType
        );

        List<Bin> sameZoneBins = binRepository.findAll()
                .stream()
                .filter(b -> b.getZone() != null && zoneId.equals(b.getZone().getId()))
                .filter(b -> b.getWasteType() != null && wasteType.equalsIgnoreCase(b.getWasteType().name()))
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .toList();

        List<Long> fullBinIds = new ArrayList<>();

        for (Bin bin : sameZoneBins) {
            BinTelemetry latest = telemetryRepository
                    .findTopByBinIdOrderByTimestampDesc(bin.getId())
                    .orElse(null);

            if (latest != null && latest.getFillLevel() >= FULL_THRESHOLD) {
                fullBinIds.add(bin.getId());
            }
        }

        if (fullBinIds.size() < MIN_BINS_FOR_EXCEPTION) return;

        String ids = fullBinIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Alert alert = existingAlert != null ? existingAlert : new Alert();

        alert.setEntityType("MUNICIPAL_EXCEPTION");
        alert.setEntityId(zoneId);
        alert.setAlertType("SCHEDULE_EXCEPTION");
        alert.setSeverity("HIGH");
        alert.setTitle("Collecte exceptionnelle proposée");
        alert.setMessage(fullBinIds.size() + " poubelles sont très remplies hors créneau normal de collecte.");
        alert.setRecommendation("Créer une mission exceptionnelle si la municipalité valide.");
        alert.setActionType("CREATE_EXCEPTION_MISSION");
        alert.setExceptionZoneId(zoneId);
        alert.setExceptionWasteType(wasteType);
        alert.setExceptionBinIds(ids);
        alert.setResolved(false);

        Alert saved = alertRepository.save(alert);
        alertRealtimeService.publishCreated(alertService.toResponse(saved));
    }
}