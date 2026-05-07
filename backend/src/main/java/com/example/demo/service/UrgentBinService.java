package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UrgentBinService {

    private static final long COOLDOWN_MINUTES = 10;

    private final DynamicReplanningService dynamicReplanningService;
    private final Map<Long, Instant> lastReplanByBin = new ConcurrentHashMap<>();

    public UrgentBinService(DynamicReplanningService dynamicReplanningService) {
        this.dynamicReplanningService = dynamicReplanningService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUrgentBin(Long binId, Long telemetryId) {
        if (binId == null) {
            return;
        }

        Instant now = Instant.now();
        Instant last = lastReplanByBin.get(binId);

        if (last != null && Duration.between(last, now).toMinutes() < COOLDOWN_MINUTES) {
            System.out.println(
                    "URGENT BIN REPLAN SKIPPED => cooldown active, binId="
                            + binId
                            + ", telemetryId="
                            + telemetryId
            );
            return;
        }

        lastReplanByBin.put(binId, now);

        try {
            System.out.println("🚨 URGENT BIN DETECTED => binId=" + binId + ", telemetryId=" + telemetryId);
            dynamicReplanningService.handleUrgentBin(binId, telemetryId);
        } catch (Exception e) {
            lastReplanByBin.remove(binId);
            throw e;
        }
    }
}