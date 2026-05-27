package com.example.demo.service;

import com.example.demo.dto.BinRequest;
import com.example.demo.dto.BinResponse;
import com.example.demo.dto.BinTelemetryDTO;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinPrediction;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.entity.BinTimePrediction;
import com.example.demo.entity.Zone;
import com.example.demo.repository.BinPredictionRepository;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import com.example.demo.repository.BinTimePredictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BinService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    private final ZoneService zoneService;
    private final BinPredictionRepository binPredictionRepository;
    private final BinTimePredictionRepository binTimePredictionRepository;
    private final PythonPredictionService pythonPredictionService;

    public BinService(
            BinRepository binRepository,
            BinTelemetryRepository binTelemetryRepository,
            ZoneService zoneService,
            BinPredictionRepository binPredictionRepository,
            BinTimePredictionRepository binTimePredictionRepository,
            PythonPredictionService pythonPredictionService
    ) {
        this.binRepository = binRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.zoneService = zoneService;
        this.binPredictionRepository = binPredictionRepository;
        this.binTimePredictionRepository = binTimePredictionRepository;
        this.pythonPredictionService = pythonPredictionService;
    }

    @Transactional(readOnly = true)
    public List<BinResponse> findAll() {
        return binRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BinResponse> findAllFast() {
        List<Bin> bins = binRepository.findAll();

        Map<Long, BinTelemetry> latestTelemetryByBinId =
                binTelemetryRepository.findLatestForAllBins()
                        .stream()
                        .filter(t -> t.getBin() != null && t.getBin().getId() != null)
                        .collect(Collectors.toMap(
                                t -> t.getBin().getId(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        Map<Long, BinPrediction> latestPredictionByBinId =
                binPredictionRepository.findLatestForAllBins()
                        .stream()
                        .filter(p -> p.getBinId() != null)
                        .collect(Collectors.toMap(
                                BinPrediction::getBinId,
                                Function.identity(),
                                (a, b) -> a
                        ));

        Map<Long, BinTimePrediction> latestTimePredictionByBinId =
                binTimePredictionRepository.findLatestForAllBins()
                        .stream()
                        .filter(p -> p.getBinId() != null)
                        .collect(Collectors.toMap(
                                BinTimePrediction::getBinId,
                                Function.identity(),
                                (a, b) -> a
                        ));

        return bins.stream()
                .map(bin -> toFastResponse(
                        bin,
                        latestTelemetryByBinId.get(bin.getId()),
                        latestPredictionByBinId.get(bin.getId()),
                        latestTimePredictionByBinId.get(bin.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BinResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public BinResponse create(BinRequest req) {
        if (req.binCode == null || req.binCode.isBlank()) {
            req.binCode = generateNextBinCode();
        }

        if (binRepository.existsByBinCode(req.binCode)) {
            throw new IllegalArgumentException("bin_code already exists");
        }

        Bin b = new Bin();
        apply(b, req, true);
        return toResponse(binRepository.save(b));
    }

    @Transactional
    public BinResponse update(Long id, BinRequest req) {
        Bin b = getOrThrow(id);
        apply(b, req, false);
        return toResponse(binRepository.save(b));
    }

    @Transactional
    public void delete(Long id) {
        if (!binRepository.existsById(id)) {
            throw new IllegalArgumentException("bin not found");
        }
        binRepository.deleteById(id);
    }

    @Transactional
    public int backfillZonesForBinsWithoutZone() {
        List<Bin> bins = binRepository.findAll().stream()
                .filter(b -> b.getZone() == null)
                .toList();

        int updated = 0;

        for (Bin b : bins) {
            Double zoneLat = b.getAccessLat() != null ? b.getAccessLat() : b.getLat();
            Double zoneLng = b.getAccessLng() != null ? b.getAccessLng() : b.getLng();

            if (zoneLat == null || zoneLng == null) {
                continue;
            }

            Optional<Zone> zoneOpt = zoneService.findZoneContainingPoint(zoneLat, zoneLng);

            if (zoneOpt.isPresent()) {
                b.setZone(zoneOpt.get());
                binRepository.save(b);
                updated++;
            }
        }

        return updated;
    }

    @Transactional
    public void processTelemetry(BinTelemetryDTO dto) {
        System.out.println("processTelemetry() called for binCode=" + dto.getBinCode());

        Bin bin = binRepository.findByBinCode(dto.getBinCode())
                .orElseThrow(() -> new RuntimeException("Bin not found: " + dto.getBinCode()));

        System.out.println("Bin found id=" + bin.getId());

        BinTelemetry telemetry = new BinTelemetry();
        telemetry.setBin(bin);
        telemetry.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());
        telemetry.setFillLevel(dto.getFillLevel() != null ? dto.getFillLevel().shortValue() : (short) 0);
        telemetry.setWeightKg(dto.getWeightKg() != null ? BigDecimal.valueOf(dto.getWeightKg()) : null);
        telemetry.setBatteryLevel(dto.getBatteryLevel() != null ? dto.getBatteryLevel().shortValue() : (short) 0);
        telemetry.setStatus(dto.getStatus() != null ? dto.getStatus() : "OK");
        telemetry.setRssi(dto.getRssi() != null ? dto.getRssi().shortValue() : (short) 0);
        telemetry.setCollected(dto.getCollected() != null ? dto.getCollected() : false);

        String src = dto.getSource();
        if (!"MQTT_REAL".equals(src) && !"MQTT_SIM".equals(src) && !"PY_SIM".equals(src)) {
            src = "MQTT_SIM";
        }
        telemetry.setSource(src);

        BinTelemetry saved = binTelemetryRepository.save(telemetry);
        binTelemetryRepository.flush();

        System.out.println("Telemetry saved in DB id=" + saved.getId());

        runPredictionAfterCommit(saved.getId());
    }

    private void runPredictionAfterCommit(Long telemetryId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runPredictionSafely(telemetryId);
                }
            });
            return;
        }

        runPredictionSafely(telemetryId);
    }

    private void runPredictionSafely(Long telemetryId) {
        try {
            pythonPredictionService.runPredictionForTelemetry(telemetryId);
        } catch (Exception e) {
            System.err.println("XGBoost prediction failed for telemetryId=" + telemetryId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void apply(Bin b, BinRequest req, boolean isCreate) {
        if (isCreate || req.binCode != null) {
            b.setBinCode(req.binCode);
        }

        if (isCreate || req.type != null) {
            try {
                b.setType(Bin.BinType.valueOf(req.type.toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid bin type, expected REAL or SIM");
            }
        }

        if (isCreate || req.wasteType != null) {
            try {
                b.setWasteType(Bin.WasteType.valueOf(req.wasteType.toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid waste type, expected GRAY, GREEN, YELLOW or WHITE");
            }
        }

        if (isCreate || req.lat != null) {
            b.setLat(req.lat);
        }

        if (isCreate || req.lng != null) {
            b.setLng(req.lng);
        }

        if (isCreate || req.accessLat != null) {
            b.setAccessLat(req.accessLat);
        }

        if (isCreate || req.accessLng != null) {
            b.setAccessLng(req.accessLng);
        }

        if (req.installationDate != null) {
            b.setInstallationDate(req.installationDate);
        } else if (isCreate) {
            b.setInstallationDate(LocalDate.now());
        }

        if (req.isActive != null) {
            b.setIsActive(req.isActive);
        } else if (isCreate) {
            b.setIsActive(true);
        }

        if (req.notes != null) {
            b.setNotes(req.notes);
        }

        if (b.getLat() == null || b.getLng() == null) {
            throw new IllegalArgumentException("lat and lng are required");
        }

        if (b.getWasteType() == null) {
            throw new IllegalArgumentException("wasteType is required");
        }

        if (b.getAccessLat() == null) {
            b.setAccessLat(b.getLat());
        }

        if (b.getAccessLng() == null) {
            b.setAccessLng(b.getLng());
        }

        Double zoneLat = b.getAccessLat() != null ? b.getAccessLat() : b.getLat();
        Double zoneLng = b.getAccessLng() != null ? b.getAccessLng() : b.getLng();

        Optional<Zone> zoneOpt = zoneService.findZoneContainingPoint(zoneLat, zoneLng);

        if (zoneOpt.isPresent()) {
            b.setZone(zoneOpt.get());
        } else if (isCreate) {
            b.setZone(null);
        }
    }

    private String generateNextBinCode() {
        String maxCode = binRepository.findMaxPvpCode();

        if (maxCode != null) {
            try {
                int num = Integer.parseInt(maxCode.substring(7));
                return String.format("PVP-15-%05d", num + 1);
            } catch (Exception e) {
                return "PVP-15-00001";
            }
        }

        int max = 0;

        List<Bin> bins = binRepository.findAll();
        for (Bin b : bins) {
            String code = b.getBinCode();
            if (code == null) continue;

            code = code.trim().toUpperCase();

            if (code.startsWith("BIN-")) {
                String suffix = code.substring(4);
                try {
                    int n = Integer.parseInt(suffix);
                    if (n > max) {
                        max = n;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (max > 0) {
            return String.format("BIN-%03d", max + 1);
        }

        return "PVP-15-00001";
    }

    private Bin getOrThrow(Long id) {
        return binRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("bin not found"));
    }

    private BinResponse toFastResponse(
            Bin b,
            BinTelemetry latest,
            BinPrediction latestPrediction,
            BinTimePrediction latestTimePrediction
    ) {
        BinResponse r = new BinResponse();

        r.id = b.getId();
        r.binCode = b.getBinCode();
        r.type = b.getType() != null ? b.getType().name() : null;
        r.wasteType = b.getWasteType() != null ? b.getWasteType().name() : null;

        r.zoneId = b.getZone() != null ? b.getZone().getId() : null;
        r.zoneName = b.getZone() != null ? b.getZone().getShapeName() : null;

        r.lat = b.getLat();
        r.lng = b.getLng();
        r.accessLat = b.getAccessLat();
        r.accessLng = b.getAccessLng();

        r.installationDate = b.getInstallationDate();
        r.isActive = b.getIsActive();
        r.notes = b.getNotes();
        r.createdAt = b.getCreatedAt();
        r.updatedAt = b.getUpdatedAt();
        r.clusterId = b.getClusterId();

        if (latest != null) {
            r.fillLevel = (int) latest.getFillLevel();
            r.batteryLevel = latest.getBatteryLevel() != null ? (int) latest.getBatteryLevel() : 0;
            r.status = latest.getStatus() != null ? latest.getStatus() : "OK";
            r.lastTelemetryAt = latest.getTimestamp() != null
                    ? OffsetDateTime.ofInstant(latest.getTimestamp(), ZoneId.systemDefault())
                    : null;
        } else {
            r.fillLevel = 0;
            r.batteryLevel = 0;
            r.status = "OK";
            r.lastTelemetryAt = null;
        }

        if (latestPrediction != null && latestPrediction.getPredictedFillNext() != null) {
            r.predictedFillLevelNext = latestPrediction.getPredictedFillNext().doubleValue();
        } else {
            r.predictedFillLevelNext = null;
        }

        if (latestTimePrediction != null && latestTimePrediction.getPredictedHours() != null) {
            r.hoursToFull = latestTimePrediction.getPredictedHours().doubleValue();
        } else {
            r.hoursToFull = null;
        }

        return r;
    }

    private BinResponse toResponse(Bin b) {
        BinResponse r = new BinResponse();

        r.id = b.getId();
        r.binCode = b.getBinCode();
        r.type = b.getType() != null ? b.getType().name() : null;
        r.wasteType = b.getWasteType() != null ? b.getWasteType().name() : null;
        r.zoneId = (b.getZone() != null) ? b.getZone().getId() : null;
        r.zoneName = (b.getZone() != null) ? b.getZone().getShapeName() : null;
        r.lat = b.getLat();
        r.lng = b.getLng();
        r.accessLat = b.getAccessLat();
        r.accessLng = b.getAccessLng();
        r.installationDate = b.getInstallationDate();
        r.isActive = b.getIsActive();
        r.notes = b.getNotes();
        r.createdAt = b.getCreatedAt();
        r.updatedAt = b.getUpdatedAt();
        r.clusterId = b.getClusterId();

        Optional<BinTelemetry> latestTelemetryOpt = binTelemetryRepository.findTopByBinOrderByTimestampDesc(b);

        if (latestTelemetryOpt.isPresent()) {
            BinTelemetry latest = latestTelemetryOpt.get();

            r.fillLevel = (int) latest.getFillLevel();
            r.batteryLevel = latest.getBatteryLevel() != null ? (int) latest.getBatteryLevel() : 0;
            r.status = latest.getStatus() != null ? latest.getStatus() : "OK";
            r.lastTelemetryAt = latest.getTimestamp() != null
                    ? OffsetDateTime.ofInstant(latest.getTimestamp(), ZoneId.systemDefault())
                    : null;
        } else {
            r.fillLevel = 0;
            r.batteryLevel = 0;
            r.status = "OK";
            r.lastTelemetryAt = null;
        }

        binPredictionRepository.findTopByBinIdOrderByCreatedAtDesc(b.getId())
                .ifPresent(p -> {
                    if (p.getPredictedFillNext() != null) {
                        r.predictedFillLevelNext = p.getPredictedFillNext().doubleValue();
                    }
                });

        binTimePredictionRepository.findTopByBinIdOrderByCreatedAtDesc(b.getId())
                .ifPresent(t -> {
                    if (t.getPredictedHours() != null) {
                        r.hoursToFull = t.getPredictedHours().doubleValue();
                    }
                });

        return r;
    }
}
