package com.example.demo.service;

import com.example.demo.dto.BinRequest;
import com.example.demo.dto.BinResponse;
import com.example.demo.dto.BinTelemetryDTO;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.entity.Zone;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class BinService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    private final ZoneService zoneService;
    private final BinPriorityService binPriorityService;
    private final BinTimePredictionService binTimePredictionService;

    public BinService(
            BinRepository binRepository,
            BinTelemetryRepository binTelemetryRepository,
            ZoneService zoneService,
            BinPriorityService binPriorityService,
            BinTimePredictionService binTimePredictionService
    ) {
        this.binRepository = binRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.zoneService = zoneService;
        this.binPriorityService = binPriorityService;
        this.binTimePredictionService = binTimePredictionService;
    }

    @Transactional(readOnly = true)
    public List<BinResponse> findAll() {
        return binRepository.findAll().stream().map(this::toResponse).toList();
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

        if (b.getAccessLat() == null) {
            b.setAccessLat(b.getLat());
        }

        if (b.getAccessLng() == null) {
            b.setAccessLng(b.getLng());
        }

        Double zoneLat = b.getAccessLat() != null ? b.getAccessLat() : b.getLat();
        Double zoneLng = b.getAccessLng() != null ? b.getAccessLng() : b.getLng();

        Optional<Zone> zoneOpt = zoneService.findZoneContainingPoint(zoneLat, zoneLng);

        Zone zone = zoneOpt.orElseThrow(() ->
                new IllegalArgumentException("Aucune zone trouvée pour cette position")
        );

        b.setZone(zone);
    }

    private String generateNextBinCode() {
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

        return String.format("BIN-%03d", max + 1);
    }

    private Bin getOrThrow(Long id) {
        return binRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("bin not found"));
    }

    private BinResponse toResponse(Bin b) {
        BinResponse r = new BinResponse();
        r.id = b.getId();
        r.binCode = b.getBinCode();
        r.type = b.getType().name();
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
        return r;
    }

    @Transactional
    public void processTelemetry(BinTelemetryDTO dto) {
        System.out.println("processTelemetry() called for binCode=" + dto.getBinCode());

        Bin bin = binRepository.findByBinCode(dto.getBinCode())
                .orElseThrow(() -> new RuntimeException("Bin not found: " + dto.getBinCode()));

        System.out.println("Bin found id=" + bin.getId());

        Optional<BinTelemetry> previousOpt = binTelemetryRepository.findTopByBinOrderByTimestampDesc(bin);

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
        if (!"MQTT_REAL".equals(src) && !"MQTT_SIM".equals(src)) {
            src = "MQTT_SIM";
        }
        telemetry.setSource(src);

        BinTelemetry saved = binTelemetryRepository.save(telemetry);
        binTelemetryRepository.flush();

        System.out.println("Telemetry saved in DB id=" + saved.getId());

        BinTelemetry previousTelemetry = previousOpt.orElse(null);

        BinTelemetry secondPreviousTelemetry = previousTelemetry != null
                ? binTelemetryRepository
                .findTopByBinIdAndIdNotOrderByTimestampDesc(bin.getId(), previousTelemetry.getId())
                .orElse(null)
                : null;

        double hour = saved.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .getHour();

        double fillDelta = 0.0;
        double fillRatePerHour = 0.0;

        if (previousTelemetry != null) {
            double currentFill = saved.getFillLevel();
            double previousFill = previousTelemetry.getFillLevel();

            fillDelta = Math.max(0.0, currentFill - previousFill);

            long seconds = saved.getTimestamp().getEpochSecond()
                    - previousTelemetry.getTimestamp().getEpochSecond();

            double hoursDiff = seconds / 3600.0;

            if (hoursDiff > 0) {
                fillRatePerHour = fillDelta / hoursDiff;
            }
        }

        double fillLevelLag1 = previousTelemetry != null
                ? previousTelemetry.getFillLevel()
                : saved.getFillLevel();

        double fillLevelLag2 = secondPreviousTelemetry != null
                ? secondPreviousTelemetry.getFillLevel()
                : fillLevelLag1;

        double fillRateLag1 = 0.0;
        if (previousTelemetry != null && secondPreviousTelemetry != null) {
            fillRateLag1 = Math.max(
                    0.0,
                    previousTelemetry.getFillLevel() - secondPreviousTelemetry.getFillLevel()
            );
        }

        double weightKgLag1 = previousTelemetry != null && previousTelemetry.getWeightKg() != null
                ? previousTelemetry.getWeightKg().doubleValue()
                : (saved.getWeightKg() != null ? saved.getWeightKg().doubleValue() : 0.0);

        double rssiLag1 = previousTelemetry != null
                ? previousTelemetry.getRssi()
                : (saved.getRssi() != null ? saved.getRssi() : 0);

        try {
            System.out.println("BEFORE model1 prediction");

            binPriorityService.predictAndSave(
                    bin.getId(),
                    saved,
                    hour,
                    saved.getFillLevel(),
                    fillDelta,
                    saved.getBatteryLevel() != null ? saved.getBatteryLevel() : 0,
                    saved.getWeightKg() != null ? saved.getWeightKg().doubleValue() : 0.0,
                    saved.getRssi() != null ? saved.getRssi() : 0,
                    Boolean.TRUE.equals(saved.getCollected()),
                    fillLevelLag1,
                    fillLevelLag2,
                    fillRateLag1,
                    weightKgLag1,
                    rssiLag1
            );

            System.out.println("AFTER model1 prediction");
        } catch (Exception e) {
            System.err.println("MODEL 1 FAILED for telemetryId=" + saved.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("BEFORE model2 prediction");

            binTimePredictionService.predictAndSave(
                    bin.getId(),
                    saved.getId(),
                    hour,
                    saved.getFillLevel(),
                    fillRatePerHour,
                    saved.getBatteryLevel() != null ? saved.getBatteryLevel() : 0,
                    saved.getWeightKg() != null ? saved.getWeightKg().doubleValue() : 0.0,
                    saved.getRssi() != null ? saved.getRssi() : 0,
                    Boolean.TRUE.equals(saved.getCollected())
            );

            System.out.println("AFTER model2 prediction");
        } catch (Exception e) {
            System.err.println("MODEL 2 FAILED for telemetryId=" + saved.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}