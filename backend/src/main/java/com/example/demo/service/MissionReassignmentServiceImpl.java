package com.example.demo.service;

import com.example.demo.dto.MissionReassignmentResponseDto;
import com.example.demo.entity.MissionReassignment;
import com.example.demo.repository.MissionReassignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MissionReassignmentServiceImpl implements MissionReassignmentService {

    private final MissionReassignmentRepository missionReassignmentRepository;

    public MissionReassignmentServiceImpl(MissionReassignmentRepository missionReassignmentRepository) {
        this.missionReassignmentRepository = missionReassignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionReassignmentResponseDto> getByOriginalMissionId(Long originalMissionId) {
        return missionReassignmentRepository.findByOriginalMissionIdOrderByReassignedAtDesc(originalMissionId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private MissionReassignmentResponseDto mapToDto(MissionReassignment entity) {
        MissionReassignmentResponseDto dto = new MissionReassignmentResponseDto();

        dto.setId(entity.getId());
        dto.setOriginalMissionId(
                entity.getOriginalMission() != null ? entity.getOriginalMission().getId() : null
        );
        dto.setSourceTruckId(
                entity.getSourceTruck() != null ? entity.getSourceTruck().getId() : null
        );
        dto.setTargetTruckId(
                entity.getTargetTruck() != null ? entity.getTargetTruck().getId() : null
        );
        dto.setBinId(
                entity.getBin() != null ? entity.getBin().getId() : null
        );
        dto.setReason(entity.getReason() != null ? entity.getReason().name() : null);
        dto.setReassignedAt(entity.getReassignedAt());
        dto.setAlgorithmVersion(entity.getAlgorithmVersion());
        dto.setNotes(entity.getNotes());

        return dto;
    }
}