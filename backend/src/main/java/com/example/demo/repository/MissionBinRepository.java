package com.example.demo.repository;

import com.example.demo.dto.DriverBinDto;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.MissionBin.AssignmentStatus;
import com.example.demo.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MissionBinRepository extends JpaRepository<MissionBin, Long> {

    List<MissionBin> findByMissionIdOrderByVisitOrderAsc(Long missionId);

    List<MissionBin> findByMissionIdAndCollectedFalseOrderByVisitOrderAsc(Long missionId);

    List<MissionBin> findByBinIdOrderByIdDesc(Long binId);

    List<MissionBin> findByMissionOrderByVisitOrderAsc(Mission mission);

    List<MissionBin> findByMissionAndAssignmentStatus(Mission mission, AssignmentStatus assignmentStatus);

    List<MissionBin> findByReassignedToTruck(Truck truck);

    List<MissionBin> findByReassignedFromTruck(Truck truck);

    long countByMissionId(Long missionId);

    @Query("""
        SELECT new com.example.demo.dto.DriverBinDto(
            mb.id,
            b.id,
            b.binCode,
            b.lat,
            b.lng,
            mb.visitOrder,
            mb.collected,
            CASE
                WHEN mb.assignmentStatus IS NOT NULL THEN CAST(mb.assignmentStatus as string)
                ELSE null
            END,
            CASE
                WHEN b.wasteType IS NOT NULL THEN CAST(b.wasteType as string)
                ELSE null
            END
        )
        FROM MissionBin mb
        JOIN mb.bin b
        WHERE mb.mission.id = :missionId
        ORDER BY mb.visitOrder ASC
    """)
    List<DriverBinDto> findDriverBinsDtoByMissionId(@Param("missionId") Long missionId);

    Optional<MissionBin> findFirstByBinBinCodeAndMissionDriverIdAndMissionStatusAndAssignmentStatusOrderByVisitOrderAsc(
            String binCode,
            Long driverId,
            String status,
            AssignmentStatus assignmentStatus
    );

    Optional<MissionBin> findFirstByBinBinCodeAndMissionDriverIdAndMissionStatusOrderByVisitOrderAsc(
            String binCode,
            Long driverId,
            String status
    );
}