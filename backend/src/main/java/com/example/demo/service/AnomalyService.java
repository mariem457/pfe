package com.example.demo.service;

import com.example.demo.dto.AnomalyDto;
import com.example.demo.dto.CloseAnomalyRequest;
import com.example.demo.entity.Anomaly;
import com.example.demo.repository.AnomalyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;
    private final ObjectMapper objectMapper;

    public AnomalyService(AnomalyRepository anomalyRepository,
                          ObjectMapper objectMapper) {
        this.anomalyRepository = anomalyRepository;
        this.objectMapper = objectMapper;
    }

    public List<AnomalyDto> getByBin(Long binId) {
        return anomalyRepository.findByBinIdOrderByDetectedAtDesc(binId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<AnomalyDto> getActiveByBin(Long binId) {
        return anomalyRepository.findByBinIdAndActiveTrue(binId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AnomalyDto close(Long anomalyId, CloseAnomalyRequest req) {

        Anomaly a = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new RuntimeException("Anomaly not found"));

        a.setActive(false);
        a.setEndTime(req != null && req.endTime != null
                ? req.endTime
                : Instant.now());

        // ✅ تحويل JSON String → Map
        if (req != null && req.details != null && !req.details.isBlank()) {
            try {
                Map<String, Object> map =
                        objectMapper.readValue(req.details,
                                new TypeReference<Map<String, Object>>() {});
                a.setDetails(map);
            } catch (Exception e) {
                throw new RuntimeException("Invalid JSON details");
            }
        }

        return toDto(a);
    }

    private AnomalyDto toDto(Anomaly a) {

        AnomalyDto dto = new AnomalyDto();
        dto.id = a.getId();
        dto.binId = a.getBin() != null ? a.getBin().getId() : null;
        dto.anomalyType = a.getAnomalyType();
        dto.score = a.getScore();
        dto.details = a.getDetails(); // Map → يرجع JSON في response
        dto.startTime = a.getStartTime();
        dto.endTime = a.getEndTime();
        dto.detectedAt = a.getDetectedAt();
        dto.modelName = a.getModelName();
        dto.active = a.isActive();

        return dto;
    }
}