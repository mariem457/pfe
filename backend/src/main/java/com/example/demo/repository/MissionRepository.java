package com.example.demo.repository;

import com.example.demo.entity.Mission;
import com.example.demo.entity.Truck;
import com.example.demo.entity.Zone;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    Optional<Mission> findByMissionCode(String missionCode);

    List<Mission> findByDriverIdAndPlannedDateOrderByCreatedAtDesc(Long driverId, LocalDate plannedDate);

    List<Mission> findByStatusOrderByPlannedDateDesc(String status);

    List<Mission> findByPlannedDateOrderByCreatedAtDesc(LocalDate plannedDate);
    List<Mission> findByTruck(Truck truck);

    List<Mission> findByTruckAndPlannedDate(Truck truck, LocalDate plannedDate);

    List<Mission> findByZone(Zone zone);
    List<Mission> findByDriverId(Long driverId);
    List<Mission> findByPlannedDate(LocalDate plannedDate);
   

    List<Mission> findByStatusOrderByPlannedDateAsc(String status);
}