package com.example.demo.repository;

import com.example.demo.entity.MissionBin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionBinRepository extends JpaRepository<MissionBin, Long> {

    List<MissionBin> findByMissionIdOrderByVisitOrderAsc(Long missionId);

    List<MissionBin> findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(Long missionId);

    List<MissionBin> findByBinIdOrderByIdDesc(Long binId);
}