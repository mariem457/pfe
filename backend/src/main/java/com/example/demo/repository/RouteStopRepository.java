package com.example.demo.repository;

import com.example.demo.entity.Bin;
import com.example.demo.entity.RoutePlan;
import com.example.demo.entity.RouteStop;
import com.example.demo.entity.RouteStop.StopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByRoutePlanOrderByStopOrderAsc(RoutePlan routePlan);

    Optional<RouteStop> findByRoutePlanAndStopOrder(RoutePlan routePlan, Integer stopOrder);

    List<RouteStop> findByBin(Bin bin);

    List<RouteStop> findByStatus(StopStatus status);

    List<RouteStop> findByRoutePlanAndStatus(RoutePlan routePlan, StopStatus status);
}