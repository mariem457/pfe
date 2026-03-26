package com.example.demo.repository;

import com.example.demo.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    long countByEventTypeAndEventTimeAfter(String eventType, OffsetDateTime after);

    List<SecurityEvent> findTop20ByOrderByEventTimeDesc();

    SecurityEvent findTopByEventTypeAndEventTimeAfterOrderByEventTimeDesc(String eventType, OffsetDateTime after);
}