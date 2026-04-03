package com.example.demo.repository;

import com.example.demo.entity.MissionReassignment;
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
}