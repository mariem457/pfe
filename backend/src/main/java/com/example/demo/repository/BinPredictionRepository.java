package com.example.demo.repository;

import com.example.demo.entity.BinPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinPredictionRepository extends JpaRepository<BinPrediction, Long> {
}