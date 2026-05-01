package com.example.demo.repository;

import com.example.demo.entity.SecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {
}