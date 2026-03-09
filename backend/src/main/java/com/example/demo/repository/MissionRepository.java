package com.example.demo.repository;

import com.example.demo.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    Optional<Mission> findByMissionCode(String missionCode);

    List<Mission> findByDriverIdAndPlannedDateOrderByCreatedAtDesc(Long driverId, LocalDate plannedDate);

    List<Mission> findByStatusOrderByPlannedDateDesc(String status);

    List<Mission> findByPlannedDateOrderByCreatedAtDesc(LocalDate plannedDate);
}