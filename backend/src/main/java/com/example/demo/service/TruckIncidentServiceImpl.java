package com.example.demo.service;

import com.example.demo.dto.IncidentStatusUpdateDto;
import com.example.demo.dto.TruckIncidentRequestDto;
import com.example.demo.dto.TruckIncidentResponseDto;
import com.example.demo.entity.Mission;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckIncident;
import com.example.demo.entity.User;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.TruckIncidentRepository;
import com.example.demo.repository.TruckRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.TruckIncidentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class TruckIncidentServiceImpl implements TruckIncidentService {

    private final TruckIncidentRepository truckIncidentRepository;
    private final TruckRepository truckRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    public TruckIncidentServiceImpl(TruckIncidentRepository truckIncidentRepository,
                                    TruckRepository truckRepository,
                                    MissionRepository missionRepository,
                                    UserRepository userRepository) {
        this.truckIncidentRepository = truckIncidentRepository;
        this.truckRepository = truckRepository;
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public TruckIncidentResponseDto createIncident(TruckIncidentRequestDto request) {
        if (request.getTruckId() == null) {
            throw new RuntimeException("Truck id is required");
        }
        if (request.getIncidentType() == null) {
            throw new RuntimeException("Incident type is required");
        }
        if (request.getSeverity() == null) {
            throw new RuntimeException("Severity is required");
        }

        Truck truck = truckRepository.findById(request.getTruckId())
                .orElseThrow(() -> new RuntimeException("Truck not found"));

        TruckIncident incident = new TruckIncident();
        incident.setTruck(truck);
        incident.setIncidentType(request.getIncidentType());
        incident.setSeverity(request.getSeverity());
        incident.setDescription(request.getDescription());
        incident.setLat(request.getLat());
        incident.setLng(request.getLng());
        incident.setAutoDetected(request.getAutoDetected() != null ? request.getAutoDetected() : false);

        if (request.getStatus() != null) {
            incident.setStatus(request.getStatus());
        } else {
            incident.setStatus(TruckIncident.IncidentStatus.OPEN);
        }

        if (request.getMissionId() != null) {
            Mission mission = missionRepository.findById(request.getMissionId())
                    .orElseThrow(() -> new RuntimeException("Mission not found"));
            incident.setMission(mission);
        }

        if (request.getReportedByUserId() != null) {
            User user = userRepository.findById(request.getReportedByUserId())
                    .orElseThrow(() -> new RuntimeException("Reporting user not found"));
            incident.setReportedByUser(user);
        }

        applyTruckStatusFromIncident(truck, incident.getIncidentType());

        TruckIncident saved = truckIncidentRepository.save(incident);
        truckRepository.save(truck);

        return mapToResponse(saved);
    }

    @Override
    public TruckIncidentResponseDto updateIncidentStatus(Long incidentId, IncidentStatusUpdateDto request) {
        TruckIncident incident = truckIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        if (request.getStatus() != null) {
            incident.setStatus(request.getStatus());

            if (request.getStatus() == TruckIncident.IncidentStatus.RESOLVED) {
                incident.setResolvedAt(OffsetDateTime.now());
            }
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            incident.setDescription(request.getDescription());
        }

        TruckIncident updated = truckIncidentRepository.save(incident);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public TruckIncidentResponseDto getIncidentById(Long incidentId) {
        TruckIncident incident = truckIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));
        return mapToResponse(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckIncidentResponseDto> getAllIncidents() {
        return truckIncidentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckIncidentResponseDto> getIncidentsByTruck(Long truckId) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new RuntimeException("Truck not found"));

        return truckIncidentRepository.findByTruck(truck)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckIncidentResponseDto> getOpenIncidents() {
        return truckIncidentRepository.findByStatus(TruckIncident.IncidentStatus.OPEN)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void applyTruckStatusFromIncident(Truck truck, TruckIncident.IncidentType incidentType) {
        switch (incidentType) {
            case BREAKDOWN -> truck.setStatus(Truck.TruckStatus.BREAKDOWN);
            case FUEL_LOW -> truck.setStatus(Truck.TruckStatus.REFUELING);
            case DRIVER_UNAVAILABLE, OVERLOAD -> truck.setStatus(Truck.TruckStatus.UNAVAILABLE);
            default -> {
                // نخلي status كما هو
            }
        }
        truck.setLastStatusUpdate(OffsetDateTime.now());
    }

    private TruckIncidentResponseDto mapToResponse(TruckIncident incident) {
        TruckIncidentResponseDto dto = new TruckIncidentResponseDto();
        dto.setId(incident.getId());
        dto.setIncidentType(incident.getIncidentType());
        dto.setSeverity(incident.getSeverity());
        dto.setDescription(incident.getDescription());
        dto.setStatus(incident.getStatus());
        dto.setAutoDetected(incident.getAutoDetected());
        dto.setLat(incident.getLat());
        dto.setLng(incident.getLng());
        dto.setReportedAt(incident.getReportedAt());
        dto.setResolvedAt(incident.getResolvedAt());

        if (incident.getTruck() != null) {
            dto.setTruckId(incident.getTruck().getId());
            dto.setTruckCode(incident.getTruck().getTruckCode());
        }

        if (incident.getMission() != null) {
            dto.setMissionId(incident.getMission().getId());
        }

        if (incident.getReportedByUser() != null) {
            dto.setReportedByUserId(incident.getReportedByUser().getId());
            dto.setReportedByUsername(incident.getReportedByUser().getUsername());
        }

        return dto;
    }
}