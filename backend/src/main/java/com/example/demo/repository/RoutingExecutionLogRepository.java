package com.example.demo.repository;

import com.example.demo.entity.RoutingExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutingExecutionLogRepository extends JpaRepository<RoutingExecutionLog, Long> {

    List<RoutingExecutionLog> findTop20ByOrderByCreatedAtDesc();
    Optional<RoutingExecutionLog> findTop1ByOrderByCreatedAtDesc();
}