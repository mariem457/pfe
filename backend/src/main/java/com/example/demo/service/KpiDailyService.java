package com.example.demo.service;

import com.example.demo.entity.KpiDaily;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import com.example.demo.repository.KpiDailyRepository;
import com.example.demo.repository.TruckLocationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class KpiDailyService {

    private final KpiDailyRepository kpiDailyRepository;
    private final BinRepository binRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    private final AlertRepository alertRepository;
    private final TruckLocationRepository truckLocationRepository;

    public KpiDailyService(
            KpiDailyRepository kpiDailyRepository,
            BinRepository binRepository,
            BinTelemetryRepository binTelemetryRepository,
            AlertRepository alertRepository,
            TruckLocationRepository truckLocationRepository
    ) {
        this.kpiDailyRepository = kpiDailyRepository;
        this.binRepository = binRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.alertRepository = alertRepository;
        this.truckLocationRepository = truckLocationRepository;
    }

    public KpiDaily recalculateTodayKpis() {
        LocalDate today = LocalDate.now();

        KpiDaily kpi = kpiDailyRepository.findByDate(today)
                .orElseGet(() -> {
                    KpiDaily item = new KpiDaily();
                    item.setDate(today);
                    return item;
                });

        long totalBins = binRepository.count();
        long fullBins = binTelemetryRepository.countFullBins();
        double avgFill = binTelemetryRepository.getAverageFillLevel() != null
                ? binTelemetryRepository.getAverageFillLevel()
                : 0.0;
        long openAlerts = alertRepository.countByResolvedFalse();
        long activeTrucks = truckLocationRepository.countActiveTrucks();

        kpi.setTotalBins((int) totalBins);
        kpi.setFullBinsCount((int) fullBins);
        kpi.setOpenAlertsCount((int) openAlerts);
        kpi.setActiveTrucksCount((int) activeTrucks);
        kpi.setAvgFillLevel(BigDecimal.valueOf(avgFill));

        return kpiDailyRepository.save(kpi);
    }
}