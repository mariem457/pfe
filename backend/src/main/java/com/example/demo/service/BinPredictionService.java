package com.example.demo.service;

import com.example.demo.entity.BinPrediction;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.BinPredictionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class BinPredictionService {

    private final BinPredictionRepository repo;

    public BinPredictionService(BinPredictionRepository repo) {
        this.repo = repo;
    }

    public void save(Long binId, BinTelemetry telemetry, PredictionResult r) {

        BinPrediction p = new BinPrediction();
        p.setBinId(binId);
        p.setTelemetry(telemetry);
        p.setPredictedFillNext(BigDecimal.valueOf(r.getPredictedFillNext()));
        p.setAlertStatus(r.getAlertStatus());
        p.setPriorityScore(BigDecimal.valueOf(r.getPriorityScore()));
        p.setShouldCollect(r.isShouldCollect());
        p.setCreatedAt(OffsetDateTime.now());

        repo.save(p);
    }
}