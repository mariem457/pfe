package com.example.demo.service;

import com.example.demo.entity.Alert;
import com.example.demo.entity.Bin;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.TruckIncident;
import com.example.demo.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import com.example.demo.entity.Mission;

import com.example.demo.service.AlertRealtimeService;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class SmartAlertService {

	private final AlertRepository alertRepository;
	private final AlertRealtimeService alertRealtimeService;
	private final AlertService alertService;

	public SmartAlertService(
	        AlertRepository alertRepository,
	        AlertRealtimeService alertRealtimeService,
	        AlertService alertService
	) {
	    this.alertRepository = alertRepository;
	    this.alertRealtimeService = alertRealtimeService;
	    this.alertService = alertService;
	}
	@Transactional
	public void createTruckIncidentAlert(TruckIncident incident) {
	    if (incident == null || incident.getId() == null) {
	        return;
	    }

	    String alertType = resolveAlertType(incident);

	    boolean exists = alertRepository.existsByIncidentIdAndAlertTypeAndResolvedFalse(
	            incident.getId(),
	            alertType
	    );

	    if (exists) {
	        return;
	    }

	    Alert alert = new Alert();
	    alert.setAlertType(alertType);
	    alert.setSeverity(incident.getSeverity() != null ? incident.getSeverity().name() : "MEDIUM");

	    alert.setEntityType("INCIDENT");
	    alert.setEntityId(incident.getId());
	    alert.setIncident(incident);

	    if (incident.getTruck() != null) {
	        alert.setTruck(incident.getTruck());
	    }

	    if (incident.getMission() != null) {
	        alert.setMission(incident.getMission());
	    }

	    alert.setTitle(buildIncidentTitle(incident));
	    alert.setMessage(buildIncidentMessage(incident));
	    alert.setRecommendation(buildIncidentRecommendation(incident));
	    alert.setActionType(resolveActionType(incident));
	    alert.setResolved(false);

	    Alert saved = alertRepository.save(alert);
	    alertRepository.flush();

	    Alert loaded = alertRepository.findCreatedAlertWithRelations(saved.getId());
	    var response = alertService.toResponse(loaded);

	    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
	        @Override
	        public void afterCommit() {
	            alertRealtimeService.publishCreated(response);
	            System.out.println("REALTIME TRUCK INCIDENT ALERT SENT => " + response.getId());
	        }
	    });
	}

    @Transactional
    public void createDriverBinIssueAlert(MissionBin missionBin) {
        if (missionBin == null || missionBin.getId() == null || missionBin.getBin() == null) {
            return;
        }

        String alertType = "DRIVER_BIN_ISSUE";

        boolean exists = alertRepository.existsByEntityTypeAndEntityIdAndAlertTypeAndResolvedFalse(
                "MISSION_BIN",
                missionBin.getId(),
                alertType
        );

        if (exists) {
            return;
        }

        Bin bin = missionBin.getBin();

        Alert alert = new Alert();
        alert.setAlertType(alertType);
        alert.setSeverity("HIGH");
        alert.setEntityType("MISSION_BIN");
        alert.setEntityId(missionBin.getId());
        alert.setBin(bin);
        alert.setMission(missionBin.getMission());
        alert.setTitle(buildBinIssueTitle(missionBin));
        alert.setMessage(buildBinIssueMessage(missionBin));
        alert.setRecommendation(buildBinIssueRecommendation(missionBin));
        alert.setActionType("INSPECT");
        alert.setResolved(false);

        Alert saved = alertRepository.save(alert);
        alertRepository.flush();

        Alert loaded = alertRepository.findCreatedAlertWithRelations(saved.getId());
        var response = alertService.toResponse(loaded);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                alertRealtimeService.publishCreated(response);
                System.out.println("REALTIME DRIVER BIN ISSUE ALERT SENT => " + response.getId());
            }
        });
    }

    @Transactional
    public int backfillOpenTruckIncidentAlerts(List<TruckIncident> incidents) {
        int created = 0;

        if (incidents == null || incidents.isEmpty()) {
            return 0;
        }

        for (TruckIncident incident : incidents) {
            if (incident == null || incident.getId() == null) {
                continue;
            }

            String alertType = resolveAlertType(incident);

            boolean exists = alertRepository.existsByIncidentIdAndAlertTypeAndResolvedFalse(
                    incident.getId(),
                    alertType
            );

            if (exists) {
                continue;
            }

            createTruckIncidentAlert(incident);
            created++;
        }

        return created;
    }
    @Transactional
    public void resolveAlertsByIncident(Long incidentId) {
        List<Alert> alerts = alertRepository.findByIncidentIdAndResolvedFalse(incidentId);

        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        for (Alert alert : alerts) {
            alert.setResolved(true);
            alert.setResolvedAt(Instant.now());
        }

        List<Alert> savedAlerts = alertRepository.saveAll(alerts);
        alertRepository.flush();

        for (Alert alert : savedAlerts) {
            Alert loaded = alertRepository.findCreatedAlertWithRelations(alert.getId());
            var response = alertService.toResponse(loaded);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    alertRealtimeService.publishResolved(response);
                }
            });
        }
    }

    private String resolveAlertType(TruckIncident incident) {
        if (incident.getIncidentType() == null) {
            return "TRUCK_INCIDENT";
        }

        if (isQrCodeTruckProblem(incident)) {
            return "TRUCK_QR_CODE_PROBLEM";
        }

        return switch (incident.getIncidentType()) {
            case BREAKDOWN -> "TRUCK_BREAKDOWN";
            case FUEL_LOW -> "TRUCK_FUEL_LOW";
            case GPS_LOST -> "TRUCK_GPS_LOST";
            case TRAFFIC_BLOCK -> "TRUCK_TRAFFIC_BLOCK";
            case DELAY -> "TRUCK_DELAY";
            case OVERLOAD -> "TRUCK_OVERLOAD";
            case DRIVER_UNAVAILABLE -> "DRIVER_UNAVAILABLE";
            default -> "TRUCK_INCIDENT";
        };
    }

    private String buildIncidentTitle(TruckIncident incident) {
        String truckCode = incident.getTruck() != null && incident.getTruck().getTruckCode() != null
                ? incident.getTruck().getTruckCode()
                : "Camion";

        if (isQrCodeTruckProblem(incident)) {
            return truckCode + " - Problème QR code";
        }

        String type = incident.getIncidentType() != null
                ? incident.getIncidentType().name()
                : "INCIDENT";

        return truckCode + " - " + type;
    }

    private String buildIncidentMessage(TruckIncident incident) {
        if (incident.getDescription() != null && !incident.getDescription().isBlank()) {
            return incident.getDescription();
        }

        String truckCode = incident.getTruck() != null && incident.getTruck().getTruckCode() != null
                ? incident.getTruck().getTruckCode()
                : "camion";

        return "Incident détecté sur " + truckCode + ".";
    }

    private String buildIncidentRecommendation(TruckIncident incident) {
        if (isQrCodeTruckProblem(incident)) {
            return "Vérifier le QR code signalé par le chauffeur et corriger l'association si nécessaire.";
        }

        if (incident.getIncidentType() == null) {
            return "Vérifier l'incident et contacter le chauffeur si nécessaire.";
        }

        return switch (incident.getIncidentType()) {
            case BREAKDOWN ->
                    "Contacter le chauffeur, immobiliser le camion et lancer une replanification des bacs restants.";
            case FUEL_LOW ->
                    "Envoyer le camion vers la station-service compatible la plus proche ou insérer un arrêt carburant.";
            case GPS_LOST ->
                    "Vérifier la connexion GPS/mobile du chauffeur. Si le camion est en mission, contacter le chauffeur.";
            case TRAFFIC_BLOCK ->
                    "Analyser le blocage et relancer une optimisation de l'itinéraire si le retard devient important.";
            case DELAY ->
                    "Contrôler l'avancement de la mission et replanifier les arrêts restants si nécessaire.";
            case OVERLOAD ->
                    "Diriger le camion vers un site de déchargement avant de continuer la collecte.";
            case DRIVER_UNAVAILABLE ->
                    "Réaffecter la mission à un autre chauffeur/camion disponible.";
            default ->
                    "Vérifier l'incident et prendre une décision opérationnelle.";
        };
    }

    private String resolveActionType(TruckIncident incident) {
        if (isQrCodeTruckProblem(incident)) {
            return "INSPECT";
        }

        if (incident.getIncidentType() == null) {
            return "INSPECT";
        }

        return switch (incident.getIncidentType()) {
            case BREAKDOWN, TRAFFIC_BLOCK, DELAY, DRIVER_UNAVAILABLE -> "REPLAN";
            case FUEL_LOW -> "REFUEL";
            case GPS_LOST -> "CALL_DRIVER";
            case OVERLOAD -> "DISPOSAL_SITE";
            default -> "INSPECT";
        };
    }

    private boolean isQrCodeTruckProblem(TruckIncident incident) {
        String text = ((incident.getDescription() != null ? incident.getDescription() : "") + " "
                + (incident.getIncidentType() != null ? incident.getIncidentType().name() : "")).toLowerCase();

        return text.contains("qr code") || text.contains("qrcode");
    }

    private String buildBinIssueTitle(MissionBin missionBin) {
        String binCode = missionBin.getBin() != null && missionBin.getBin().getBinCode() != null
                ? missionBin.getBin().getBinCode()
                : "Poubelle";

        return binCode + " - Problème signalé par chauffeur";
    }

    private String buildBinIssueMessage(MissionBin missionBin) {
        String issueType = missionBin.getIssueType() != null ? missionBin.getIssueType() : "OTHER";
        String note = missionBin.getDriverNote() != null && !missionBin.getDriverNote().isBlank()
                ? missionBin.getDriverNote()
                : "Aucune description ajoutée.";

        return "Le chauffeur a signalé un problème sur cette poubelle. Type: "
                + issueType
                + ". Détail: "
                + note;
    }

    private String buildBinIssueRecommendation(MissionBin missionBin) {
        String issueType = missionBin.getIssueType() != null ? missionBin.getIssueType() : "OTHER";

        return switch (issueType) {
            case "BLOCKED" -> "Vérifier l'accès à la poubelle et envoyer une équipe si nécessaire.";
            case "DAMAGED" -> "Planifier une intervention de maintenance pour inspecter ou remplacer la poubelle.";
            case "SENSOR_ERROR" -> "Vérifier le capteur de la poubelle et créer une intervention maintenance.";
            default -> "Vérifier le signalement chauffeur et décider de l'action appropriée.";
        };
    }
    @Transactional
    public void createMissionAlert(
            Mission mission,
            String alertType,
            String severity,
            String title,
            String message,
            String recommendation,
            String actionType
    ) {
        if (mission == null || mission.getId() == null) return;

        boolean exists = alertRepository.existsByMissionIdAndAlertTypeAndResolvedFalse(
                mission.getId(),
                alertType
        );

        if (exists) return;

        Alert alert = new Alert();
        alert.setMission(mission);
        alert.setEntityType("MISSION");
        alert.setEntityId(mission.getId());
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setRecommendation(recommendation);
        alert.setActionType(actionType);
        alert.setResolved(false);


        Alert saved = alertRepository.save(alert);
        alertRepository.flush();

        Alert loaded = alertRepository.findCreatedAlertWithRelations(saved.getId());
        var response = alertService.toResponse(loaded);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                alertRealtimeService.publishCreated(response);
                System.out.println("REALTIME INCIDENT ALERT SENT => " + response.getId());
            }
        });}
}
