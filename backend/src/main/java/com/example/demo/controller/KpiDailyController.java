package com.example.demo.controller;

import com.example.demo.entity.KpiDaily;
import com.example.demo.service.KpiDailyService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi-daily")
public class KpiDailyController {

    private final KpiDailyService kpiDailyService;

    public KpiDailyController(KpiDailyService kpiDailyService) {
        this.kpiDailyService = kpiDailyService;
    }

    @PostMapping("/recalculate")
    public KpiDaily recalculateTodayKpis() {
        return kpiDailyService.recalculateTodayKpis();
    }
}