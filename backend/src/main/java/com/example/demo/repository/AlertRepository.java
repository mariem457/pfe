package com.example.demo.repository;

import com.example.demo.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("""
        SELECT a FROM Alert a
        JOIN FETCH a.bin
        LEFT JOIN FETCH a.telemetry
        LEFT JOIN FETCH a.resolvedBy
        WHERE a.resolved = false
        ORDER BY a.createdAt DESC
    """)
    List<Alert> findOpenAlertsWithRelations();

    @Query("""
        SELECT a FROM Alert a
        JOIN FETCH a.bin
        LEFT JOIN FETCH a.telemetry
        LEFT JOIN FETCH a.resolvedBy
        WHERE a.bin.id = :binId
        ORDER BY a.createdAt DESC
    """)
    List<Alert> findByBinIdWithRelations(@Param("binId") Long binId);

    @Query("""
        SELECT a FROM Alert a
        JOIN FETCH a.bin
        LEFT JOIN FETCH a.telemetry
        LEFT JOIN FETCH a.resolvedBy
        WHERE a.bin.id = :binId AND a.resolved = false
        ORDER BY a.createdAt DESC
    """)
    List<Alert> findByBinIdAndResolvedFalseWithRelations(@Param("binId") Long binId);

    boolean existsByBinIdAndResolvedFalseAndAlertTypeAndSeverity(
            Long binId, String alertType, String severity
    );

    @Query("""
        SELECT a FROM Alert a
        JOIN FETCH a.bin
        LEFT JOIN FETCH a.telemetry
        LEFT JOIN FETCH a.resolvedBy
        WHERE a.id = :id
    """)
    Alert findByIdWithRelations(@Param("id") Long id);

    @Query(value = """
        SELECT a.id
        FROM alerts a
        JOIN bins b ON b.id = a.bin_id
        WHERE (:resolved IS NULL OR a.resolved = :resolved)
          AND (:severity IS NULL OR a.severity = :severity)
          AND (:alertType IS NULL OR a.alert_type = :alertType)
          AND (
                :q IS NULL OR
                b.bin_code ILIKE ('%' || :q || '%') OR
                a.title   ILIKE ('%' || :q || '%') OR
                a.message ILIKE ('%' || :q || '%')
              )
        ORDER BY a.created_at DESC
    """, nativeQuery = true)
    List<Long> searchIdsNative(@Param("resolved") Boolean resolved,
                               @Param("severity") String severity,
                               @Param("alertType") String alertType,
                               @Param("q") String q);

    @Query("""
        SELECT DISTINCT a FROM Alert a
        JOIN FETCH a.bin
        LEFT JOIN FETCH a.telemetry
        LEFT JOIN FETCH a.resolvedBy
        WHERE a.id IN :ids
        ORDER BY a.createdAt DESC
    """)
    List<Alert> findByIdInWithRelations(@Param("ids") List<Long> ids);

    long countByResolvedFalse();
}