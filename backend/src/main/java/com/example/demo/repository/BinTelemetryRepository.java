package com.example.demo.repository;

import com.example.demo.entity.BinTelemetry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BinTelemetryRepository extends JpaRepository<BinTelemetry, Long> {

    List<BinTelemetry> findByBinIdOrderByTimestampDesc(Long binId, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT ON (bt.bin_id) bt.*
        FROM bin_telemetry bt
        ORDER BY bt.bin_id, bt.timestamp DESC
    """, nativeQuery = true)
    List<BinTelemetry> findLatestForAllBins();

    @Query(value = """
        SELECT COUNT(*)
        FROM bins b
        JOIN LATERAL (
            SELECT bt.fill_level
            FROM bin_telemetry bt
            WHERE bt.bin_id = b.id
            ORDER BY bt.timestamp DESC
            LIMIT 1
        ) latest ON true
        WHERE latest.fill_level >= 90
    """, nativeQuery = true)
    long countFullBins();

    @Query(value = """
        SELECT COALESCE(AVG(latest.fill_level), 0)
        FROM bins b
        JOIN LATERAL (
            SELECT bt.fill_level
            FROM bin_telemetry bt
            WHERE bt.bin_id = b.id
            ORDER BY bt.timestamp DESC
            LIMIT 1
        ) latest ON true
    """, nativeQuery = true)
    Double getAverageFillLevel();
    
    
    
    
    
    @Query(value = """
    	    SELECT COUNT(*)
    	    FROM bins b
    	    JOIN LATERAL (
    	        SELECT bt.fill_level
    	        FROM bin_telemetry bt
    	        WHERE bt.bin_id = b.id
    	        ORDER BY bt.timestamp DESC
    	        LIMIT 1
    	    ) latest ON true
    	    WHERE latest.fill_level < 40
    	""", nativeQuery = true)
    	long countEmptyBins();

    	@Query(value = """
    	    SELECT COUNT(*)
    	    FROM bins b
    	    JOIN LATERAL (
    	        SELECT bt.fill_level
    	        FROM bin_telemetry bt
    	        WHERE bt.bin_id = b.id
    	        ORDER BY bt.timestamp DESC
    	        LIMIT 1
    	    ) latest ON true
    	    WHERE latest.fill_level >= 40 AND latest.fill_level < 90
    	""", nativeQuery = true)
    	long countPartialBins();
}