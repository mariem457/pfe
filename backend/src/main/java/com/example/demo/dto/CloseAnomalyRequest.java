package com.example.demo.dto;

import java.time.Instant;

public class CloseAnomalyRequest {
    public Instant endTime;     // optional
    public String details;      // optional (why closed)
}