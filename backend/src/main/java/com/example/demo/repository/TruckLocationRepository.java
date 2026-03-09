package com.example.demo.repository;

import com.example.demo.entity.TruckLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TruckLocationRepository extends JpaRepository<TruckLocation, Long> {

    @Query("select tl from TruckLocation tl where tl.driver.id = :driverId order by tl.timestamp desc")
    Optional<TruckLocation> findLatestByDriverId(Long driverId);

    @Query(value = """
        SELECT COUNT(*)
        FROM (
            SELECT tl.driver_id, MAX(tl.timestamp) AS last_time
            FROM truck_locations tl
            GROUP BY tl.driver_id
        ) t
        WHERE t.last_time >= NOW() - INTERVAL '10 minutes'
    """, nativeQuery = true)
    long countActiveTrucks();
}