package com.example.demo.service;

import com.example.demo.dto.BinDistributionDto;
import com.example.demo.dto.ChartPointDto;
import com.example.demo.dto.DashboardChartsResponse;
import com.example.demo.dto.DashboardKpiResponse;
import com.example.demo.repository.BinRepository;
import com.example.demo.repository.BinTelemetryRepository;
import com.example.demo.repository.KpiDailyRepository;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.TruckLocationRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {

    private final BinRepository binRepository;
    private final BinTelemetryRepository binTelemetryRepository;
    private final TruckLocationRepository truckLocationRepository;
    private final KpiDailyRepository kpiDailyRepository;
    private final MissionBinRepository missionBinRepository;

    public DashboardService(
            BinRepository binRepository,
            BinTelemetryRepository binTelemetryRepository,
            TruckLocationRepository truckLocationRepository,
            KpiDailyRepository kpiDailyRepository,
            MissionBinRepository missionBinRepository
    ) {
        this.binRepository = binRepository;
        this.binTelemetryRepository = binTelemetryRepository;
        this.truckLocationRepository = truckLocationRepository;
        this.kpiDailyRepository = kpiDailyRepository;
        this.missionBinRepository = missionBinRepository;
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
        ZoneId zone = ZoneId.of("Europe/Paris");

        LocalDate endDate = LocalDate.now(zone);
        LocalDate startDate = endDate.minusDays(6);

        Instant startInstant = startDate.atStartOfDay(zone).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(zone).toInstant();

        List<ChartPointDto> fillTrend = buildFillTrend(startDate, endDate, startInstant);
        List<ChartPointDto> weeklyCollections = buildWeeklyCollections(startDate, endDate, startInstant, endInstant);

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

        return new DashboardChartsResponse(
                fillTrend,
                weeklyCollections,
                distribution
        );
    }

    private List<ChartPointDto> buildFillTrend(LocalDate startDate, LocalDate endDate, Instant startInstant) {
        List<Object[]> rows = binTelemetryRepository.findAverageFillLevelByDay(startInstant);

        Map<LocalDate, Double> valuesByDate = new HashMap<>();

        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            double value = toDouble(row[1]);

            if (date != null) {
                valuesByDate.put(date, value);
            }
        }

        List<ChartPointDto> result = new ArrayList<>();

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            double value = valuesByDate.getOrDefault(cursor, 0.0);

            result.add(new ChartPointDto(
                    dayLabel(cursor),
                    roundOneDecimal(value)
            ));

            cursor = cursor.plusDays(1);
        }

        return result;
    }

    private List<ChartPointDto> buildWeeklyCollections(
            LocalDate startDate,
            LocalDate endDate,
            Instant startInstant,
            Instant endInstant
    ) {
        List<Object[]> rows = missionBinRepository.countCollectedBinsByDay(startInstant, endInstant);

        Map<LocalDate, Double> valuesByDate = new HashMap<>();

        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            double value = toDouble(row[1]);

            if (date != null) {
                valuesByDate.put(date, value);
            }
        }

        List<ChartPointDto> result = new ArrayList<>();

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            double value = valuesByDate.getOrDefault(cursor, 0.0);

            result.add(new ChartPointDto(
                    dayLabel(cursor),
                    value
            ));

            cursor = cursor.plusDays(1);
        }

        return result;
    }

    private String dayLabel(LocalDate date) {
        String label = date
                .getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.FRENCH);

        return capitalize(label);
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        return LocalDate.parse(value.toString());
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return Double.parseDouble(value.toString());
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}