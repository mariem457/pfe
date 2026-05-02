package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrgentBinService {

    private final DynamicReplanningService dynamicReplanningService;

    public UrgentBinService(DynamicReplanningService dynamicReplanningService) {
        this.dynamicReplanningService = dynamicReplanningService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUrgentBin(Long binId, Long telemetryId) {
        System.out.println("🚨 URGENT BIN DETECTED => binId=" + binId + ", telemetryId=" + telemetryId);

        dynamicReplanningService.handleUrgentBin(binId, telemetryId);
    }
}