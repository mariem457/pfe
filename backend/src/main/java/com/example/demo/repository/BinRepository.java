package com.example.demo.repository;

import com.example.demo.entity.Bin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BinRepository extends JpaRepository<Bin, Long> {
    boolean existsByBinCode(String binCode);
    Optional<Bin> findByBinCode(String binCode);
}