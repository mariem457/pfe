package com.example.demo.repository;

import com.example.demo.entity.KpiDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface KpiDailyRepository extends JpaRepository<KpiDaily, Long> {
    Optional<KpiDaily> findByDate(LocalDate date);
    List<KpiDaily> findTop7ByOrderByDateDesc();
}