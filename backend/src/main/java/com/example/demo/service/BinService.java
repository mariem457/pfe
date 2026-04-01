package com.example.demo.service;

import com.example.demo.dto.BinRequest;
import com.example.demo.dto.BinResponse;
import com.example.demo.dto.BinTelemetryDTO;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
                    saved.getId(),
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

    public List<BinResponse> findAll() {
        throw new UnsupportedOperationException("findAll not implemented yet");
    }

    public BinResponse findById(Long id) {
        throw new UnsupportedOperationException("findById not implemented yet");
    }

    public BinResponse create(BinRequest request) {
        throw new UnsupportedOperationException("create not implemented yet");
    }

    public BinResponse update(Long id, BinRequest request) {
        throw new UnsupportedOperationException("update not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("delete not implemented yet");
    }
}