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
import java.util.List;
import java.util.Optional;

@Service
public class BinService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    private final ZoneService zoneService;

    public BinService(
            BinRepository binRepository,
            BinTelemetryRepository binTelemetryRepository,
            ZoneService zoneService
    ) {
        this.binRepository = binRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.zoneService = zoneService;
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

        Optional<Zone> zoneOpt = zoneService.findZoneContainingPoint(b.getLat(), b.getLng());

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
        r.installationDate = b.getInstallationDate();
        r.isActive = b.getIsActive();
        r.notes = b.getNotes();
        r.createdAt = b.getCreatedAt();
        r.updatedAt = b.getUpdatedAt();
        return r;
    }

    @Transactional
    public void processTelemetry(BinTelemetryDTO dto) {
        System.out.println("📥 processTelemetry() called for binCode=" + dto.getBinCode());

        Bin bin = binRepository.findByBinCode(dto.getBinCode())
                .orElseThrow(() -> new RuntimeException("❌ Bin not found: " + dto.getBinCode()));

        System.out.println("✅ Bin found id=" + bin.getId());

        BinTelemetry telemetry = new BinTelemetry();
        telemetry.setBin(bin);
        telemetry.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());
        telemetry.setFillLevel(dto.getFillLevel() != null ? dto.getFillLevel().shortValue() : (short) 0);
        telemetry.setWeightKg(dto.getWeightKg() != null ? BigDecimal.valueOf(dto.getWeightKg()) : null);
        telemetry.setBatteryLevel(dto.getBatteryLevel() != null ? dto.getBatteryLevel().shortValue() : (short) 0);
        telemetry.setStatus(dto.getStatus() != null ? dto.getStatus() : "OK");
        telemetry.setRssi(dto.getRssi() != null ? dto.getRssi().shortValue() : (short) 0);

        String src = dto.getSource();
        if (!"MQTT_REAL".equals(src) && !"MQTT_SIM".equals(src)) {
            src = "MQTT_SIM";
        }
        telemetry.setSource(src);

        BinTelemetry saved = binTelemetryRepository.save(telemetry);

        System.out.println("💾 Telemetry saved in DB"
                + (saved.getId() != null ? (" id=" + saved.getId()) : ""));
    }
}