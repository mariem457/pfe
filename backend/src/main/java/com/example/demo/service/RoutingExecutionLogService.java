package com.example.demo.service;

import com.example.demo.entity.RoutingExecutionLog;

import java.util.List;

public interface RoutingExecutionLogService {

    RoutingExecutionLog getLastExecution();

    List<RoutingExecutionLog> getLastExecutions(int limit);
}