package com.example.demo.repository;

import com.example.demo.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
}