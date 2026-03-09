package com.example.demo.dto;

import java.util.List;

public class DashboardChartsResponse {

    private List<ChartPointDto> fillTrend;
    private List<ChartPointDto> weeklyCollections;
    private BinDistributionDto distribution;

    public DashboardChartsResponse() {
    }

    public DashboardChartsResponse(List<ChartPointDto> fillTrend,
                                   List<ChartPointDto> weeklyCollections,
                                   BinDistributionDto distribution) {
        this.fillTrend = fillTrend;
        this.weeklyCollections = weeklyCollections;
        this.distribution = distribution;
    }

    public List<ChartPointDto> getFillTrend() {
        return fillTrend;
    }

    public void setFillTrend(List<ChartPointDto> fillTrend) {
        this.fillTrend = fillTrend;
    }

    public List<ChartPointDto> getWeeklyCollections() {
        return weeklyCollections;
    }

    public void setWeeklyCollections(List<ChartPointDto> weeklyCollections) {
        this.weeklyCollections = weeklyCollections;
    }

    public BinDistributionDto getDistribution() {
        return distribution;
    }

    public void setDistribution(BinDistributionDto distribution) {
        this.distribution = distribution;
    }
}