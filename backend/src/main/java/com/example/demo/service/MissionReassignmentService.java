package com.example.demo.service;

import com.example.demo.dto.MissionReassignmentResponseDto;

import java.util.List;

public interface MissionReassignmentService {

    List<MissionReassignmentResponseDto> getByOriginalMissionId(Long originalMissionId);
}