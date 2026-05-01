package com.example.demo.repository;

import com.example.demo.entity.MissionReassignment;

import com.example.demo.entity.Bin;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionReassignment;
import com.example.demo.entity.MissionReassignment.ReassignmentReason;
import com.example.demo.entity.Truck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionReassignmentRepository extends JpaRepository<MissionReassignment, Long> {

    List<MissionReassignment> findByOriginalMissionIdOrderByReassignedAtDesc(Long originalMissionId);

    List<MissionReassignment> findByTargetTruckIdOrderByReassignedAtDesc(Long targetTruckId);

    List<MissionReassignment> findBySourceTruckIdOrderByReassignedAtDesc(Long sourceTruckId);

    boolean existsByOriginalMissionIdAndSourceTruckIdAndTargetTruckIdAndBinId(
            Long originalMissionId,
            Long sourceTruckId,
            Long targetTruckId,
            Long binId
    );

    List<MissionReassignment> findByOriginalMission(Mission mission);

    List<MissionReassignment> findBySourceTruck(Truck sourceTruck);

    List<MissionReassignment> findByTargetTruck(Truck targetTruck);

    List<MissionReassignment> findByBin(Bin bin);

    List<MissionReassignment> findByReason(ReassignmentReason reason);

}