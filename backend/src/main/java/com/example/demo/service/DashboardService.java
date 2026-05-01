package com.example.demo.service;

import com.example.demo.dto.BinDistributionDto;
import com.example.demo.dto.ChartPointDto;
import com.example.demo.dto.DashboardChartsResponse;
import com.example.demo.dto.DashboardKpiResponse;
import com.example.demo.entity.KpiDaily;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import com.example.demo.repository.KpiDailyRepository;
import com.example.demo.repository.TruckLocationRepository;
import org.springframework.stereotype.Service;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    private final TruckLocationRepository truckLocationRepository;
    private final KpiDailyRepository kpiDailyRepository;

    public DashboardService(
            BinRepository binRepository,
            BinTelemetryRepository binTelemetryRepository,
            TruckLocationRepository truckLocationRepository,
            KpiDailyRepository kpiDailyRepository
    ) {
        this.binRepository = binRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.truckLocationRepository = truckLocationRepository;
        this.kpiDailyRepository = kpiDailyRepository;
    }

    public DashboardKpiResponse getDashboardKpis() {
        long totalBins = binRepository.count();
        long fullBins = binTelemetryRepository.countFullBins();
        long activeTrucks = truckLocationRepository.countActiveTrucks();
        Double avg = binTelemetryRepository.getAverageFillLevel();
        double averageFillLevel = avg != null ? avg : 0.0;

        return new DashboardKpiResponse(
                totalBins,
                fullBins,
                activeTrucks,
                averageFillLevel
        );
    }

    public DashboardChartsResponse getDashboardCharts() {
        List<KpiDaily> latestDays = kpiDailyRepository.findTop7ByOrderByDateDesc();
        Collections.reverse(latestDays);

        List<ChartPointDto> fillTrend = new ArrayList<>();
        List<ChartPointDto> weeklyCollections = new ArrayList<>();

        for (KpiDaily day : latestDays) {
            String label = day.getDate()
                    .getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.FRENCH);

            fillTrend.add(new ChartPointDto(
                    capitalize(label),
                    day.getAvgFillLevel() != null ? day.getAvgFillLevel().doubleValue() : 0.0
            ));

            weeklyCollections.add(new ChartPointDto(
                    capitalize(label),
                    day.getCollectedBinsCount() != null ? day.getCollectedBinsCount() : 0
            ));
        }

        long emptyBins = binTelemetryRepository.countEmptyBins();
        long partialBins = binTelemetryRepository.countPartialBins();
        long fullBins = binTelemetryRepository.countFullBins();
        long totalBins = binRepository.count();

        BinDistributionDto distribution = new BinDistributionDto(
                emptyBins,
                partialBins,
                fullBins,
                totalBins
        );

        return new DashboardChartsResponse(fillTrend, weeklyCollections, distribution);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}