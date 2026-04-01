package com.example.demo.repository;

import com.example.demo.entity.PublicReportDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicReportDecisionRepository extends JpaRepository<PublicReportDecision, Long> {
    List<PublicReportDecision> findByReportIdOrderByCreatedAtDesc(Long reportId);
}