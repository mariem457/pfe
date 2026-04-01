package com.example.demo.repository;

import com.example.demo.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepotRepository extends JpaRepository<Depot, Long> {

    List<Depot> findByIsActiveTrue();

    List<Depot> findByNameContainingIgnoreCase(String name);
}