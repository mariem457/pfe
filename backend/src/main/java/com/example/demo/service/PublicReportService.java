package com.example.demo.service;

import com.example.demo.dto.AssignPublicReportRequest;
import com.example.demo.entity.Alert;
import com.example.demo.entity.Bin;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.BinRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import com.example.demo.dto.CreatePublicReportRequest;
import com.example.demo.dto.PublicReportDecisionResponse;
import com.example.demo.dto.PublicReportResponse;
import com.example.demo.dto.ResolvePublicReportRequest;
import com.example.demo.entity.Driver;
import com.example.demo.entity.PublicReport;
import com.example.demo.entity.PublicReportDecision;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.PublicReportDecisionRepository;
import com.example.demo.repository.PublicReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PublicReportService {

    private final PublicReportRepository publicReportRepository;
    private final DriverRepository driverRepository;
    private final FileStorageService fileStorageService;
    private final PublicReportDecisionRepository publicReportDecisionRepository;
    private final AlertRepository alertRepository;
    private final BinRepository binRepository;
    private final RoutingOptimizationService routingOptimizationService;

    public PublicReportService(PublicReportRepository publicReportRepository,
            DriverRepository driverRepository,
            FileStorageService fileStorageService,
            PublicReportDecisionRepository publicReportDecisionRepository,
            AlertRepository alertRepository,
            BinRepository binRepository,
            RoutingOptimizationService routingOptimizationService) {
					this.publicReportRepository = publicReportRepository;
					this.driverRepository = driverRepository;
					this.fileStorageService = fileStorageService;
					this.publicReportDecisionRepository = publicReportDecisionRepository;
					this.alertRepository = alertRepository;
					this.binRepository = binRepository;
					this.routingOptimizationService = routingOptimizationService;
}
    public PublicReportResponse create(CreatePublicReportRequest request, MultipartFile photo) {
        PublicReport report = new PublicReport();

        report.setReportCode(generateCode());
        report.setReportType(request.getReportType());
        report.setDescription(request.getDescription());
        report.setAddress(request.getAddress());
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setStatus("EN_ATTENTE");
        report.setPriority(calculatePriority(request.getReportType()));

        String qualificationNote = detectQualificationNote(request);
        report.setQualificationNote(qualificationNote);

        Long duplicateOf = detectPossibleDuplicate(request);
        report.setDuplicateOfReportId(duplicateOf);

        if (photo != null && !photo.isEmpty()) {
            String photoUrl = fileStorageService.save(photo);
            report.setPhotoUrl(photoUrl);
        }

        PublicReport saved = publicReportRepository.save(report);
        saveDecision(saved.getId(), "CREATED", "Signalement créé");

        if (qualificationNote != null && !qualificationNote.isBlank()) {
            saveDecision(saved.getId(), "QUALIFIED", qualificationNote);
        }

        if (duplicateOf != null) {
            saveDecision(saved.getId(), "POSSIBLE_DUPLICATE", "Possible doublon du signalement #" + duplicateOf);
        }

        return mapToResponse(saved);
    }

    public List<PublicReportResponse> getAll() {
        return publicReportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<PublicReportResponse> getValidatedForOptimization() {
    	return publicReportRepository.findByStatusOrderByCreatedAtDesc("AFFECTE")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public PublicReportResponse validate(Long reportId) {
        PublicReport report = publicReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Signalement introuvable"));

        report.setStatus("AFFECTE");
        report.setDecisionReason("Signalement validé par la municipalité");

        PublicReport saved = publicReportRepository.save(report);
        saveDecision(saved.getId(), "VALIDATED", "Signalement validé");

        Bin nearestBin = findNearestBin(saved.getLatitude(), saved.getLongitude());

        if (nearestBin == null) {
            saveDecision(saved.getId(), "OPTIMIZATION_SKIPPED", "Aucun bac proche trouvé");
            return mapToResponse(saved);
        }

        Alert alert = new Alert();
        alert.setBin(nearestBin);
        alert.setAlertType("MUNICIPAL_EXCEPTION");
        alert.setSeverity("HIGH");
        alert.setTitle("Signalement citoyen validé");
        alert.setMessage(
                "Signalement public validé #" + saved.getReportCode()
                        + " - type: " + saved.getReportType()
                        + " - adresse: " + saved.getAddress()
        );
        alert.setEntityType("MUNICIPAL_EXCEPTION");
        alert.setEntityId(saved.getId());
        alert.setRecommendation("Créer une mission exceptionnelle pour collecter le bac le plus proche.");
        alert.setActionType("CREATE_EXCEPTION_MISSION");
        alert.setExceptionZoneId(nearestBin.getZone() != null ? nearestBin.getZone().getId() : null);
        alert.setExceptionWasteType(nearestBin.getWasteType() != null ? nearestBin.getWasteType().name() : null);
        alert.setExceptionBinIds(String.valueOf(nearestBin.getId()));
        alert.setResolved(false);

        Alert savedAlert = alertRepository.save(alert);

        saveDecision(saved.getId(), "OPTIMIZATION_QUEUED",
                "Signalement validé et bac #" + nearestBin.getId()
                        + " marqué urgent pour la prochaine optimisation.");

        return mapToResponse(saved);
    }
    public PublicReportResponse reject(Long reportId, String reason) {
        PublicReport report = publicReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Signalement introuvable"));

        report.setStatus("REJETE");
        report.setDecisionReason(reason);
        report.setResolvedNote(reason);

        PublicReport saved = publicReportRepository.save(report);
        saveDecision(saved.getId(), "REJECTED", reason);

        return mapToResponse(saved);
    }

    public PublicReportResponse qualify(Long reportId, String qualificationNote, Long duplicateOfReportId) {
        PublicReport report = publicReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Signalement introuvable"));

        report.setQualificationNote(qualificationNote);
        report.setDuplicateOfReportId(duplicateOfReportId);

        PublicReport saved = publicReportRepository.save(report);

        String reason = qualificationNote != null ? qualificationNote : "Qualification mise à jour";
        if (duplicateOfReportId != null) {
            reason += " / doublon possible de #" + duplicateOfReportId;
        }

        saveDecision(saved.getId(), "QUALIFIED", reason);

        return mapToResponse(saved);
    }

    public PublicReportResponse assign(Long reportId, AssignPublicReportRequest request) {
        PublicReport report = publicReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Signalement introuvable"));

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver introuvable"));

        report.setAssignedDriver(driver);
        report.setStatus("AFFECTE");
        report.setDecisionReason("Signalement affecté au camion");

        PublicReport saved = publicReportRepository.save(report);
        saveDecision(saved.getId(), "ASSIGNED", "Affecté au driver " + driver.getFullName());

        return mapToResponse(saved);
    }

    public PublicReportResponse resolve(Long reportId, ResolvePublicReportRequest request) {
        PublicReport report = publicReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Signalement introuvable"));

        report.setStatus("RESOLU");
        report.setResolvedAt(OffsetDateTime.now());
        report.setResolvedNote(request.getResolvedNote());
        report.setDecisionReason(request.getResolvedNote());

        PublicReport saved = publicReportRepository.save(report);
        saveDecision(saved.getId(), "RESOLVED", request.getResolvedNote());

        return mapToResponse(saved);
    }

    public List<PublicReportDecisionResponse> getDecisionHistory(Long reportId) {
        return publicReportDecisionRepository.findByReportIdOrderByCreatedAtDesc(reportId)
                .stream()
                .map(this::mapDecisionToResponse)
                .toList();
    }

    private void saveDecision(Long reportId, String actionType, String reason) {
        PublicReportDecision decision = new PublicReportDecision();
        decision.setReportId(reportId);
        decision.setActionType(actionType);
        decision.setReason(reason);
        publicReportDecisionRepository.save(decision);
    }

    private Long detectPossibleDuplicate(CreatePublicReportRequest request) {
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);

        List<PublicReport> recentReports = publicReportRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);

        for (PublicReport existing : recentReports) {
            if (existing.getLatitude() == null || existing.getLongitude() == null) continue;
            if (!sameType(existing.getReportType(), request.getReportType())) continue;

            double distanceMeters = distanceMeters(
                    existing.getLatitude(),
                    existing.getLongitude(),
                    request.getLatitude(),
                    request.getLongitude()
            );

            if (distanceMeters <= 120) {
                return existing.getId();
            }
        }

        return null;
    }

    private String detectQualificationNote(CreatePublicReportRequest request) {
        if (request.getLatitude() == null || request.getLongitude() == null) {
            return "Coordonnées manquantes";
        }

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            return "Description courte ou absente";
        }

        return null;
    }

    private boolean sameType(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double aa =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa));
        return earthRadius * c;
    }

    private String generateCode() {
        return "IWT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String calculatePriority(String reportType) {
        if ("OVERFLOW".equals(reportType) || "ILLEGAL_DUMP".equals(reportType)) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private PublicReportResponse mapToResponse(PublicReport report) {
        PublicReportResponse response = new PublicReportResponse();
        response.setId(report.getId());
        response.setReportCode(report.getReportCode());
        response.setReportType(report.getReportType());
        response.setPhotoUrl(report.getPhotoUrl());
        response.setDescription(report.getDescription());
        response.setAddress(report.getAddress());
        response.setLatitude(report.getLatitude());
        response.setLongitude(report.getLongitude());
        response.setStatus(report.getStatus());
        response.setPriority(report.getPriority());
        response.setCreatedAt(report.getCreatedAt());
        response.setResolvedAt(report.getResolvedAt());
        response.setResolvedNote(report.getResolvedNote());
        response.setDuplicateOfReportId(report.getDuplicateOfReportId());
        response.setQualificationNote(report.getQualificationNote());
        response.setDecisionReason(report.getDecisionReason());

        if (report.getAssignedDriver() != null) {
            response.setAssignedDriverName(report.getAssignedDriver().getFullName());
        }

        return response;
    }

    private PublicReportDecisionResponse mapDecisionToResponse(PublicReportDecision decision) {
        PublicReportDecisionResponse response = new PublicReportDecisionResponse();
        response.setId(decision.getId());
        response.setReportId(decision.getReportId());
        response.setActionType(decision.getActionType());
        response.setReason(decision.getReason());
        response.setCreatedAt(decision.getCreatedAt());
        return response;
    }
    private Bin findNearestBin(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }

        return binRepository.findAll()
                .stream()
                .filter(bin -> Boolean.TRUE.equals(bin.getIsActive()))
                .filter(bin -> bin.getLat() != null && bin.getLng() != null)
                .min((b1, b2) -> Double.compare(
                        distanceMeters(lat, lng, b1.getLat(), b1.getLng()),
                        distanceMeters(lat, lng, b2.getLat(), b2.getLng())
                ))
                .orElse(null);
    }
}