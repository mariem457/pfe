package com.example.demo.repository;

import com.example.demo.entity.Bin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BinRepository extends JpaRepository<Bin, Long> {

    boolean existsByBinCode(String binCode);

    Optional<Bin> findByBinCode(String binCode);

    @Override
    @EntityGraph(attributePaths = {"zone"})
    List<Bin> findAll();

    @Query("SELECT MAX(b.binCode) FROM Bin b WHERE b.binCode LIKE 'PVP-15-%'")
    String findMaxPvpCode();

    @Override
    @EntityGraph(attributePaths = {"zone"})
    Optional<Bin> findById(Long id);
}