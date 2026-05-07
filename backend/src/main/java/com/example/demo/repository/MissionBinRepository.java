package com.example.demo.repository;

import com.example.demo.dto.DriverBinDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsByMissionIdAndBinId(Long missionId, Long binId);

    Optional<MissionBin> findByMissionIdAndBinId(Long missionId, Long binId);

    long countByMissionId(Long missionId);

    @Query("""
        SELECT new com.example.demo.dto.DriverBinDto(
            mb.mission.id,
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

    long countByMissionIdAndCollectedTrue(Long missionId);

    long countByMissionIdAndCollectedFalse(Long missionId);
    @Query("""
    	    SELECT CASE WHEN COUNT(mb) > 0 THEN true ELSE false END
    	    FROM MissionBin mb
    	    WHERE mb.bin.id = :binId
    	      AND mb.collected = false
    	      AND mb.mission.status IN ('CREATED', 'PLANNED', 'IN_PROGRESS')
    	""")
    	boolean existsActiveUncollectedBinAssignment(@Param("binId") Long binId);
    
    
    
    
    @Query(value = """
    	    SELECT
    	        CAST(mb.collected_at AT TIME ZONE 'Europe/Paris' AS date) AS day,
    	        COUNT(*) AS collected_count
    	    FROM mission_bins mb
    	    WHERE mb.collected = true
    	      AND mb.collected_at IS NOT NULL
    	      AND mb.collected_at >= :startInstant
    	      AND mb.collected_at < :endInstant
    	    GROUP BY day
    	    ORDER BY day
    	""", nativeQuery = true)
    	List<Object[]> countCollectedBinsByDay(
    	        @Param("startInstant") java.time.Instant startInstant,
    	        @Param("endInstant") java.time.Instant endInstant
    	);
    
}