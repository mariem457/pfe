package com.example.demo.repository;

import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.MissionBin.AssignmentStatus;
import com.example.demo.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionBinRepository extends JpaRepository<MissionBin, Long> {

    List<MissionBin> findByMissionIdOrderByVisitOrderAsc(Long missionId);

    List<MissionBin> findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(Long missionId);

    List<MissionBin> findByBinIdOrderByIdDesc(Long binId);

    List<MissionBin> findByMissionOrderByVisitOrderAsc(Mission mission);

    List<MissionBin> findByMissionAndAssignmentStatus(Mission mission, AssignmentStatus assignmentStatus);

    List<MissionBin> findByReassignedToTruck(Truck truck);

    List<MissionBin> findByReassignedFromTruck(Truck truck);

    long countByMissionId(Long missionId);
}