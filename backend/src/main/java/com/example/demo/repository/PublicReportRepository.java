package com.example.demo.repository;

import com.example.demo.entity.PublicReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface PublicReportRepository extends JpaRepository<PublicReport, Long> {
    List<PublicReport> findByStatusOrderByCreatedAtDesc(String status);
    List<PublicReport> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
   

    

    List<PublicReport> findByStatusInOrderByCreatedAtDesc(List<String> statuses);

    List<PublicReport> findByCreatedAtAfterOrderByCreatedAtDesc(OffsetDateTime after);
}