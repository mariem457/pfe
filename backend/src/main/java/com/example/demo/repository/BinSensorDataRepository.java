package com.example.demo.repository;

import com.example.demo.entity.BinSensorData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BinSensorDataRepository extends JpaRepository<BinSensorData, Long> {

    Optional<BinSensorData> findTopByBinIdOrderByCreatedAtDesc(String binId);
}