package com.example.demo.repository;

import com.example.demo.entity.BinPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BinPredictionRepository extends JpaRepository<BinPrediction, Long> {

    Optional<BinPrediction> findTopByBinIdOrderByCreatedAtDesc(Long binId);

    @Query(value = """
        SELECT DISTINCT ON (bp.bin_id) bp.*
        FROM bin_predictions bp
        ORDER BY bp.bin_id, bp.created_at DESC, bp.id DESC
    """, nativeQuery = true)
    List<BinPrediction> findLatestForAllBins();
}