package com.example.demo.service;

import com.example.demo.dto.BinStatusDto;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BinStatusService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository telemetryRepository;

    public BinStatusService(BinRepository binRepository, BinTelemetryRepository telemetryRepository) {
        this.binRepository = binRepository;
        this.telemetryRepository = telemetryRepository;
    }

    public List<BinStatusDto> getBinsWithLatestTelemetry() {

        List<Bin> bins = binRepository.findAll();
        List<BinTelemetry> latest = telemetryRepository.findLatestForAllBins();

        Map<Long, BinTelemetry> latestByBinId = latest.stream()
                .filter(bt -> bt.getBin() != null)
                .collect(Collectors.toMap(
                        bt -> bt.getBin().getId(),
                        bt -> bt,
                        (a, b) -> a
                ));

        List<BinStatusDto> out = new ArrayList<>();

        for (Bin b : bins) {
            BinTelemetry t = latestByBinId.get(b.getId());

            BinStatusDto dto = new BinStatusDto();
            dto.binId = b.getId();
            dto.binCode = b.getBinCode();
            dto.lat = b.getLat();
            dto.lng = b.getLng();

            if (t != null) {
                dto.timestamp = t.getTimestamp();
                dto.fillLevel = (int) t.getFillLevel();
                dto.batteryLevel = (int) t.getBatteryLevel();
                dto.status = t.getStatus();
            } else {
                dto.timestamp = null;
                dto.fillLevel = 0;
                dto.batteryLevel = 0;
                dto.status = "OK";
            }

            out.add(dto);
        }

        return out;
    }
}