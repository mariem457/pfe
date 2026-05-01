package com.example.demo.repository;

import com.example.demo.entity.Mission;
import com.example.demo.entity.RoutePlan;
import com.example.demo.entity.RoutePlan.PlanStatus;
import com.example.demo.entity.RoutePlan.PlanType;
import com.example.demo.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {

    List<RoutePlan> findByMission(Mission mission);

    List<RoutePlan> findByTruck(Truck truck);

    List<RoutePlan> findByPlanStatus(PlanStatus planStatus);

    List<RoutePlan> findByPlanType(PlanType planType);

    List<RoutePlan> findByMissionOrderByCreatedAtDesc(Mission mission);

    List<RoutePlan> findByTruckOrderByCreatedAtDesc(Truck truck);
}