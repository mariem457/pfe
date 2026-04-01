package com.example.demo.repository;

import com.example.demo.entity.BinTimePrediction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinTimePredictionRepository extends JpaRepository<BinTimePrediction, Long> {
}