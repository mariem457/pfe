package com.example.demo.repository;

import com.example.demo.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long> {
    List<ApiKeyEntity> findAllByOrderByCreatedAtDesc();
}