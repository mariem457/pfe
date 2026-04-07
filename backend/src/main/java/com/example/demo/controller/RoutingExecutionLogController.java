package com.example.demo.controller;

import com.example.demo.entity.RoutingExecutionLog;
import com.example.demo.service.RoutingExecutionLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routing/execution")
public class RoutingExecutionLogController {

    private final RoutingExecutionLogService routingExecutionLogService;

    public RoutingExecutionLogController(RoutingExecutionLogService routingExecutionLogService) {
        this.routingExecutionLogService = routingExecutionLogService;
    }

    // 🔥 آخر execution
    @GetMapping("/last")
    public RoutingExecutionLog getLastExecution() {
        return routingExecutionLogService.getLastExecution();
    }

    // 🔥 history (default 10)
    @GetMapping("/history")
    public List<RoutingExecutionLog> getHistory(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return routingExecutionLogService.getLastExecutions(limit);
    }
}