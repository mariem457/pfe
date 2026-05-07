package com.example.demo.repository;

import com.example.demo.entity.BinTimePrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BinTimePredictionRepository extends JpaRepository<BinTimePrediction, Long> {

    Optional<BinTimePrediction> findTopByBinIdOrderByCreatedAtDesc(Long binId);

    @Query(value = """
        SELECT DISTINCT ON (btp.bin_id) btp.*
        FROM bin_time_predictions btp
        ORDER BY btp.bin_id, btp.created_at DESC, btp.id DESC
    """, nativeQuery = true)
    List<BinTimePrediction> findLatestForAllBins();
}