package com.example.demo.service;

import com.example.demo.entity.RoutingExecutionLog;
import com.example.demo.repository.RoutingExecutionLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoutingExecutionLogServiceImpl implements RoutingExecutionLogService {

    private final RoutingExecutionLogRepository routingExecutionLogRepository;

    public RoutingExecutionLogServiceImpl(RoutingExecutionLogRepository routingExecutionLogRepository) {
        this.routingExecutionLogRepository = routingExecutionLogRepository;
    }

    @Override
    public RoutingExecutionLog getLastExecution() {
        List<RoutingExecutionLog> logs =
                routingExecutionLogRepository.findTop20ByOrderByCreatedAtDesc();

        return logs.isEmpty() ? null : logs.get(0);
    }

    @Override
    public List<RoutingExecutionLog> getLastExecutions(int limit) {
        List<RoutingExecutionLog> logs =
                routingExecutionLogRepository.findTop20ByOrderByCreatedAtDesc();

        if (limit >= logs.size()) {
            return logs;
        }

        return logs.subList(0, limit);
    }
}