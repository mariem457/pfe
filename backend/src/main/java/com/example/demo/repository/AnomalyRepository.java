package com.example.demo.repository;

import com.example.demo.entity.Anomaly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {

    List<Anomaly> findByBinIdOrderByDetectedAtDesc(Long binId);

    List<Anomaly> findByBinIdAndActiveTrue(Long binId);
    

    Optional<Anomaly> findFirstByBinIdAndAnomalyTypeAndActiveTrue(Long binId, String anomalyType);

    // آخر N anomalies (اختياري)
    List<Anomaly> findByBinId(Long binId, Pageable pageable);

    // anomalies في فترة (اختياري)
    List<Anomaly> findByBinIdAndDetectedAtBetweenOrderByDetectedAtDesc(Long binId, Instant from, Instant to);
   
}