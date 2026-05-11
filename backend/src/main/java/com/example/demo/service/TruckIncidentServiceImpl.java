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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import com.example.demo.dto.routing.ReplanRequestDto;
@Service
@Transactional
public class TruckIncidentServiceImpl implements TruckIncidentService {

    private final TruckIncidentRepository truckIncidentRepository;
    private final TruckRepository truckRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final SmartAlertService smartAlertService;
    private final DynamicReplanningService dynamicReplanningService;

    public TruckIncidentServiceImpl(
            TruckIncidentRepository truckIncidentRepository,
            TruckRepository truckRepository,
            MissionRepository missionRepository,
            UserRepository userRepository,
            SmartAlertService smartAlertService,
            DynamicReplanningService dynamicReplanningService
    ) {
        this.truckIncidentRepository = truckIncidentRepository;
        this.truckRepository = truckRepository;
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
        this.smartAlertService = smartAlertService;
        this.dynamicReplanningService = dynamicReplanningService;
    }

    @Override
    public TruckIncidentResponseDto createIncident(TruckIncidentRequestDto request) {
        if (request.getTruckId() == null) throw new RuntimeException("Truck id is required");
        if (request.getIncidentType() == null) throw new RuntimeException("Incident type is required");
        if (request.getSeverity() == null) throw new RuntimeException("Severity is required");

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
        TruckIncident.IncidentStatus incidentStatus =
                request.getStatus() != null ? request.getStatus() : TruckIncident.IncidentStatus.OPEN;
        incident.setStatus(incidentStatus);

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

        applyTruckStatusFromIncident(truck, incident.getIncidentType(), incidentStatus);

        TruckIncident saved = truckIncidentRepository.save(incident);
        truckRepository.saveAndFlush(truck);

        smartAlertService.createTruckIncidentAlert(saved);

        triggerAutomaticReplanIfNeeded(saved);

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

                if (incident.getTruck() != null) {
                    Truck truck = incident.getTruck();

                    boolean hasOtherOpenIncidents = truckIncidentRepository
                            .findByTruckAndStatus(truck, TruckIncident.IncidentStatus.OPEN)
                            .stream()
                            .anyMatch(other -> !other.getId().equals(incident.getId()));

                    if (!hasOtherOpenIncidents) {
                        truck.setStatus(Truck.TruckStatus.AVAILABLE);
                        truck.setLastStatusUpdate(OffsetDateTime.now());
                        truckRepository.save(truck);
                    }
                }

                smartAlertService.resolveAlertsByIncident(incident.getId());
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
        return truckIncidentRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckIncidentResponseDto> getIncidentsByTruck(Long truckId) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new RuntimeException("Truck not found"));

        return truckIncidentRepository.findByTruck(truck).stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckIncidentResponseDto> getOpenIncidents() {
        return truckIncidentRepository.findByStatus(TruckIncident.IncidentStatus.OPEN)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    
    
    private void triggerAutomaticReplanIfNeeded(TruckIncident incident) {
        if (incident == null) {
            return;
        }

        if (incident.getStatus() != TruckIncident.IncidentStatus.OPEN) {
            return;
        }

        if (incident.getTruck() == null || incident.getTruck().getId() == null) {
            System.out.println("AUTO REPLAN SKIPPED => incident has no truckId, incidentId=" + incident.getId());
            return;
        }

        if (!isReplanningIncident(incident.getIncidentType())) {
            System.out.println(
                    "AUTO REPLAN SKIPPED => incidentType="
                            + incident.getIncidentType()
                            + ", incidentId="
                            + incident.getId()
            );
            return;
        }

        Mission missionForReplan = resolveMissionForAutomaticReplan(incident);

        if (missionForReplan == null || missionForReplan.getId() == null) {
            System.out.println(
                    "AUTO REPLAN SKIPPED => no matching active mission found for truckId="
                            + incident.getTruck().getId()
                            + ", incidentId="
                            + incident.getId()
            );
            return;
        }

        ReplanRequestDto replanRequest = new ReplanRequestDto();
        replanRequest.setAffectedTruckId(incident.getTruck().getId());
        replanRequest.setIncidentType(incident.getIncidentType().name());
        replanRequest.setReason(
                incident.getDescription() != null && !incident.getDescription().isBlank()
                        ? incident.getDescription()
                        : "Incident camion déclaré par chauffeur"
        );

        try {
            dynamicReplanningService.replanMission(missionForReplan.getId(), replanRequest);

            System.out.println(
                    "✅ AUTO REPLAN DONE => incidentId="
                            + incident.getId()
                            + ", missionId="
                            + missionForReplan.getId()
                            + ", truckId="
                            + incident.getTruck().getId()
                            + ", incidentType="
                            + incident.getIncidentType()
            );
        } catch (Exception e) {
            System.out.println(
                    "⚠️ AUTO REPLAN FAILED BUT INCIDENT SAVED => incidentId="
                            + incident.getId()
                            + ", missionId="
                            + missionForReplan.getId()
                            + ", reason="
                            + e.getMessage()
            );
        }
    }
    
    private Mission resolveMissionForAutomaticReplan(TruckIncident incident) {
        if (incident == null || incident.getTruck() == null || incident.getTruck().getId() == null) {
            return null;
        }

        Long incidentTruckId = incident.getTruck().getId();

        /*
         * First choice: use the mission sent by the chauffeur app,
         * but only if it is really assigned to the same truck.
         */
        if (incident.getMission() != null
                && incident.getMission().getTruck() != null
                && incident.getMission().getTruck().getId() != null
                && incident.getMission().getTruck().getId().equals(incidentTruckId)) {
            return incident.getMission();
        }

        if (incident.getMission() != null) {
            Long missionTruckId = incident.getMission().getTruck() != null
                    ? incident.getMission().getTruck().getId()
                    : null;

            System.out.println(
                    "AUTO REPLAN MISSION/TRUCK MISMATCH => incidentId="
                            + incident.getId()
                            + ", sentMissionId="
                            + incident.getMission().getId()
                            + ", sentMissionTruckId="
                            + missionTruckId
                            + ", incidentTruckId="
                            + incidentTruckId
                            + ". Searching active mission for this truck..."
            );
        }

        return missionRepository
                .findTopByTruckAndStatusInOrderByCreatedAtDesc(
                        incident.getTruck(),
                        List.of("IN_PROGRESS", "CREATED", "PLANNED")
                )
                .orElse(null);
    }

    private boolean isReplanningIncident(TruckIncident.IncidentType incidentType) {
        if (incidentType == null) {
            return false;
        }

        return switch (incidentType) {
            case BREAKDOWN, FUEL_LOW, TRAFFIC_BLOCK, DELAY, DRIVER_UNAVAILABLE -> true;
            default -> false;
        };
    }

    private void applyTruckStatusFromIncident(
            Truck truck,
            TruckIncident.IncidentType incidentType,
            TruckIncident.IncidentStatus incidentStatus
    ) {
        if (truck == null || incidentType == null) {
            return;
        }

        if (incidentStatus == TruckIncident.IncidentStatus.RESOLVED
                || incidentStatus == TruckIncident.IncidentStatus.CANCELLED) {
            return;
        }

        switch (incidentType) {
            case BREAKDOWN -> truck.setStatus(Truck.TruckStatus.BREAKDOWN);
            case FUEL_LOW -> truck.setStatus(Truck.TruckStatus.REFUELING);
            case GPS_LOST -> truck.setStatus(Truck.TruckStatus.BREAKDOWN);
            case DRIVER_UNAVAILABLE, OVERLOAD -> truck.setStatus(Truck.TruckStatus.UNAVAILABLE);
            default -> {
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
