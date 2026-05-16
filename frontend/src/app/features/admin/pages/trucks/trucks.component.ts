import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FleetMapComponent, FleetMapInitialTruck } from './fleet-map/fleet-map.component';
import { AlertService, AlertDto } from '../../../../services/alert.service';
import {
  TruckDashboardService,
  TruckDashboardResponse,
  TruckItem,
  MissionRouteResponse,
} from '../../../../services/truck-dashboard.service';
import {
  DriverNotificationService,
  DriverNotificationResponse
} from '../../../../services/driver-notification.service';
import {
  TruckIncident,
  TruckIncidentService,
} from '../../../../services/truck-incident.service';
import { RoutingReplanService } from '../../../../services/routing-replan.service';
import { firstValueFrom } from 'rxjs';

type TruckStatus = 'Actif' | 'Inactif';

interface TruckCard {
  driverId: number | null;
  id: string;
  driver: string;
  location: string;
  status: TruckStatus;
  truckStatus: string;
  inMission: boolean;
  progress: number;
  collected: number;
  remaining: number;
  fuel: number;
  etaMins: number;
  currentMissionId?: number | null;
  lat?: number;
  lng?: number;
}


@Component({
  selector: 'app-trucks',
  standalone: true,
  imports: [CommonModule, FleetMapComponent],
  templateUrl: './trucks.component.html',
  styleUrls: ['./trucks.component.css'],
})
export class TrucksComponent implements OnInit, OnDestroy {
  missionColors = ['#2563eb', '#059669', '#7c3aed', '#f97316', '#475569', '#ef4444'];

  kpis = [
    { icon: 'local_shipping', label: 'Camions actifs', value: '0' },
    { icon: 'assignment', label: 'Missions en cours', value: '0' },
    { icon: 'warning', label: 'Incidents ouverts', value: '0' },
    { icon: 'smart_toy', label: 'Auto-détectés', value: '0' },
  ];

  trucks: TruckCard[] = [];
  missionTrucks: TruckCard[] = [];
  offMissionTrucks: TruckCard[] = [];
  mapTrucks: FleetMapInitialTruck[] = [];
  truckRoutes: any[] = [];

  openIncidents: TruckIncident[] = [];
  alerts: AlertDto[] = [];
  incidentMap: { [truckCode: string]: TruckIncident } = {};

  loadingIncidents = false;
  runningAutoDetection = false;
  replanningIncidentId: number | null = null;
  resolvedIncidentId: number | null = null;

  autoDetectionMessage = '';
  private alertSub = new Subscription();
  private refreshTimer: any;
contactingIncidentId: number | null = null;
notificationByIncident: { [incidentId: number]: DriverNotificationResponse } = {};
  constructor(
    private dashboardService: TruckDashboardService,
    private incidentService: TruckIncidentService,
    private replanService: RoutingReplanService,
    private alertService: AlertService,
    private driverNotificationService: DriverNotificationService
  ) { }

  ngOnInit(): void {
    this.loadDashboard();
    this.loadOpenIncidents();
    this.loadTruckAlerts();

    this.alertSub.add(
      this.alertService.realtimeAlert$.subscribe(alert => {
        if (!alert || alert.resolved) return;

        const type = (alert as any).alertType ?? (alert as any).alert_type ?? '';

        const isFleetAlert =
          [
            
            'TRUCK_GPS_LOST',
            'TRUCK_OVERLOAD',
            'TRUCK_BREAKDOWN',
            'TRUCK_TRAFFIC_BLOCK',
            'TRUCK_DELAY',
            'DRIVER_UNAVAILABLE'
          ].includes(type);

        if (!isFleetAlert) return;

        const exists = this.alerts.some(a => a.id === alert.id);

        if (!exists) {
          this.alerts = [alert, ...this.alerts];
        }

        this.loadOpenIncidents();
        this.loadDashboard();
      })
    );

    this.alertSub.add(
      this.alertService.realtimeResolved$.subscribe(alert => {
        if (!alert) return;

        this.alerts = this.alerts.filter(a => a.id !== alert.id);
        this.loadTruckAlerts();
        this.loadOpenIncidents();
        this.loadDashboard();
      })
    );

    // Refresh automatique كل 30 ثانية
    this.refreshTimer = setInterval(() => {
      this.loadOpenIncidents();
      this.loadTruckAlerts();
      this.loadDashboard();
    }, 30000);
  }
  ngOnDestroy(): void {
    this.alertSub.unsubscribe();

    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  }

  loadDashboard(): void {
    this.dashboardService.getDashboard().subscribe({
      next: (data: TruckDashboardResponse) => {
        this.trucks = data.trucks.map((t: TruckItem | any) => {
          const lat = t.lat ?? t.latitude ?? t.lastKnownLat ?? t.last_known_lat ?? null;
          const lng = t.lng ?? t.longitude ?? t.lastKnownLng ?? t.last_known_lng ?? null;
          const truckStatus = t.truckStatus ?? t.truck_status ?? 'UNKNOWN';
          const currentMissionId = t.currentMissionId ?? t.current_mission_id ?? null;

          // نعرضو كان camions اللي فعلاً في مهمة
          const inMission =
            truckStatus === 'ON_MISSION' ||
            truckStatus === 'IN_PROGRESS';

          return {
            driverId: t.driverId ?? t.driver_id ?? null,
            id: t.truckCode ?? t.truck_code,
            driver: t.driverName ?? t.driver_name ?? 'Non assigné',
            location: t.locationLabel ?? t.location_label ?? '—',
            status: t.active ?? t.is_active ? 'Actif' : 'Inactif',
            truckStatus,
            inMission,
            progress: t.progress ?? 0,
            collected: t.collectedBins ?? t.collected_bins ?? 0,
            remaining: t.remainingBins ?? t.remaining_bins ?? 0,
            fuel: t.fuelLevel ?? t.fuel_level ?? 0,
            etaMins: t.etaMinutes ?? t.eta_minutes ?? 0,
            currentMissionId,
            lat,
            lng,
          };
        });

        this.missionTrucks = this.trucks.filter((t) => t.inMission);
        this.offMissionTrucks = this.trucks.filter((t) => !t.inMission);

        this.mapTrucks = this.trucks
          .filter((t) =>
            t.inMission &&
            t.lat != null &&
            t.lng != null
          )
          .map((t) => ({
            id: String(t.driverId ?? t.id),
            truckCode: t.id,
            lat: Number(t.lat),
            lng: Number(t.lng),
            label: t.id,
            progress: t.progress,
            fuelLevel: t.fuel,
            etaMinutes: t.etaMins,
            status: 'ON_MISSION',
            currentMissionId: t.currentMissionId ?? null,
          } as any));


        this.updateKpis(data);
        this.loadMissionRoutesForTrucks();
      },
      error: (err: any) => {
        console.error('Dashboard trucks error:', err);
      },
    });
  }

  updateKpis(data?: TruckDashboardResponse): void {
    const autoCount = this.openIncidents.filter(i => i.autoDetected).length;

    this.kpis = [
      {
        icon: 'local_shipping',
        label: 'Camions actifs',
        value: data ? data.activeTrucks.toString() : this.trucks.filter(t => t.status === 'Actif').length.toString(),
      },
      {
        icon: 'assignment',
        label: 'Missions en cours',
        value: this.missionTrucks.length.toString(),
      },
      {
        icon: 'warning',
        label: 'Incidents ouverts',
        value: this.openIncidents.length.toString(),
      },
      {
        icon: 'smart_toy',
        label: 'Incidents auto',
        value: autoCount.toString(),
      },
    ];
  }

  async loadMissionRoutesForTrucks(): Promise<void> {
    const trucksWithMission = this.missionTrucks.filter((t) => !!t.currentMissionId);

    if (!trucksWithMission.length) {
      this.truckRoutes = [];
      return;
    }

    try {
      const routes = await Promise.all(
        trucksWithMission.map((t) =>
          firstValueFrom(this.dashboardService.getMissionRoute(Number(t.currentMissionId)))
        )
      );

      this.truckRoutes = routes
        .filter((route: MissionRouteResponse | any) => !!route)
        .map((route: MissionRouteResponse | any, index: number) => {
          const truck = trucksWithMission[index];

          return {
            truckId: String(truck.driverId ?? truck.id),
            truckCode: truck.id,
            missionId: route.missionId,
            routeCoordinates: route.routeCoordinates || [],
            collectionRouteCoordinates: route.collectionRouteCoordinates || [],
            transferRouteCoordinates: route.transferRouteCoordinates || [],
          };
        });
    } catch (err) {
      console.error('Load truck routes error:', err);
      this.truckRoutes = [];
    }
  }

  loadOpenIncidents(): void {
    this.loadingIncidents = true;

    this.incidentService.getOpenIncidents().subscribe({
      next: (data: TruckIncident[]) => {
        this.openIncidents = (data || []).filter(i =>
  !['FUEL_LOW', 'OVERLOAD'].includes(i.incidentType)
);
        this.openIncidents.forEach(i => this.loadIncidentNotification(i));
        this.buildIncidentMap();
        this.loadingIncidents = false;
        this.updateKpis();
        this.loadDashboard();
        this.loadTruckAlerts();
      },
      error: (err: any) => {
        console.error('Truck incidents error:', err);
        this.loadingIncidents = false;
      },
    });
  }

 

  buildIncidentMap(): void {
    this.incidentMap = {};

    this.openIncidents.forEach((incident) => {
      if (!incident.truckCode || incident.status !== 'OPEN') return;

      const existing = this.incidentMap[incident.truckCode];

      if (!existing) {
        this.incidentMap[incident.truckCode] = incident;
        return;
      }

      if (this.getSeverityWeight(incident.severity) > this.getSeverityWeight(existing.severity)) {
        this.incidentMap[incident.truckCode] = incident;
        return;
      }

      if (
        this.getSeverityWeight(incident.severity) === this.getSeverityWeight(existing.severity) &&
        incident.id > existing.id
      ) {
        this.incidentMap[incident.truckCode] = incident;
      }
    });
  }

  getIncidentForTruck(truckCode: string): TruckIncident | undefined {
    return this.incidentMap[truckCode];
  }

  resolveIncident(incident: TruckIncident): void {
    const ok = confirm(
      `Voulez-vous vraiment résoudre l’incident du camion ${incident.truckCode} ?\n\nLe camion sera remis disponible s’il n’a pas d’autre incident ouvert.`
    );

    if (!ok) return;

    this.resolvedIncidentId = incident.id;

    this.incidentService
      .resolveIncident(
        incident.id,
        incident.description || 'Incident résolu par la municipalité'
      )
      .subscribe({
        next: () => {
          this.resolvedIncidentId = null;

          this.autoDetectionMessage =
            `Incident du camion ${incident.truckCode} résolu. Le statut du camion a été mis à jour.`;

          this.loadOpenIncidents();
          this.loadDashboard();
          this.loadTruckAlerts();
        },
        error: (err: any) => {
          console.error('Resolve incident error:', err);
          alert('Erreur lors de la résolution de l’incident.');
          this.resolvedIncidentId = null;
        },
      });
  }

  replanIncident(incident: TruckIncident): void {
    if (this.replanningIncidentId === incident.id) return;

    if (!incident.missionId) {
      alert('Impossible de replanifier: missionId manquant pour cet incident.');
      return;
    }

    if (!incident.truckId) {
      alert('Impossible de replanifier: truckId manquant.');
      return;
    }

    this.replanningIncidentId = incident.id;

    this.replanService
      .replanMission(incident.missionId, {
        affectedTruckId: incident.truckId,
        incidentType: incident.incidentType,
        reason: incident.description || 'Incident camion',
      })
      .subscribe({
        next: () => {
          this.incidentService
            .resolveIncident(
              incident.id,
              incident.description || 'Incident replanifié avec succès'
            )
            .subscribe({
              next: () => {
                this.replanningIncidentId = null;
                this.loadOpenIncidents();
                this.loadDashboard();
              },
              error: () => {
                this.replanningIncidentId = null;
                this.loadOpenIncidents();
                this.loadDashboard();
              },
            });
        },
        error: (err: any) => {
          console.error('REPLAN ERROR:', err);
          alert('Erreur lors de la replanification.');
          this.replanningIncidentId = null;
        },
      });
  }

  getTruckStatusLabel(status: string): string {
    switch (status) {
      case 'ON_MISSION':
        return 'En mission';
      case 'AVAILABLE':
        return 'Disponible';
      case 'REFUELING':
        return 'Carburant';
      case 'BREAKDOWN':
        return 'Panne';
      case 'MAINTENANCE':
        return 'Maintenance';
      case 'UNAVAILABLE':
        return 'Indisponible';
      case 'OUT_OF_SERVICE':
        return 'Hors service';
      case 'INCIDENT':
        return 'Incident';
      default:
        return status || 'Inconnu';
    }
  }

  getIncidentTypeLabel(type: string): string {
    switch (type) {
      case 'GPS_LOST':
        return 'GPS perdu';
      case 'FUEL_LOW':
        return 'Carburant faible';
      case 'BREAKDOWN':
        return 'Panne';
      case 'OVERLOAD':
        return 'Surcharge';
      case 'DRIVER_UNAVAILABLE':
        return 'Chauffeur indisponible';
      case 'TRAFFIC_BLOCK':
        return 'Trafic bloqué';
      case 'DELAY':
        return 'Retard';
      default:
        return type;
    }
  }

  getSeverityLabel(severity: string): string {
    switch (severity) {
      case 'LOW':
        return 'Faible';
      case 'MEDIUM':
        return 'Moyenne';
      case 'HIGH':
        return 'Élevée';
      case 'CRITICAL':
        return 'Critique';
      default:
        return severity;
    }
  }

  getSeverityClass(severity: string): string {
    switch (severity) {
      case 'LOW':
        return 'sev-low';
      case 'MEDIUM':
        return 'sev-medium';
      case 'HIGH':
        return 'sev-high';
      case 'CRITICAL':
        return 'sev-critical';
      default:
        return 'sev-medium';
    }
  }

  getSeverityWeight(severity: string): number {
    switch (severity) {
      case 'LOW':
        return 1;
      case 'MEDIUM':
        return 2;
      case 'HIGH':
        return 3;
      case 'CRITICAL':
        return 4;
      default:
        return 0;
    }
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    return new Date(value).toLocaleString('fr-FR');
  }
  loadTruckAlerts(): void {
    this.alertService.searchAlerts({
      resolved: false,
      entityType: 'INCIDENT'
    }).subscribe({
      next: (alerts: AlertDto[]) => {
        this.alerts = (alerts || []).filter((a: any) => {
          const type = a.alertType ?? a.alert_type ?? '';

          return [
            
            'TRUCK_GPS_LOST',
            'TRUCK_OVERLOAD',
            'TRUCK_BREAKDOWN',
            'TRUCK_TRAFFIC_BLOCK',
            'TRUCK_DELAY',
            'DRIVER_UNAVAILABLE'
          ].includes(type);
        });

        console.log('Fleet truck alerts shown:', this.alerts);
      },
      error: (err: any) => {
        console.error('Truck alerts error:', err);
        this.alerts = [];
      }
    });
  }











  hasAutomaticReplanInfo(incident: TruckIncident): boolean {
    return !!incident.missionId && ['BREAKDOWN', 'TRAFFIC_BLOCK', 'DELAY' ,'DRIVER_UNAVAILABLE'].includes(incident.incidentType);
  }

  getIncidentMissionLabel(incident: TruckIncident): string {
    return incident.missionId ? `Mission #${incident.missionId}` : 'Aucune mission liée';
  }

  getIncidentReplanMessage(incident: TruckIncident): string {
    if (!incident.missionId) {
      return 'Incident enregistré sans mission active associée.';
    }

    if (incident.incidentType === 'BREAKDOWN') {
      return 'Replanification automatique lancée: les bacs restants sont transférés vers un camion disponible.';
    }

   

    return 'Incident opérationnel détecté: la mission peut être adaptée dynamiquement.';
  }

shouldShowManualReplan(incident: TruckIncident): boolean {
  return !!incident.missionId &&
    ['BREAKDOWN', 'DRIVER_UNAVAILABLE', 'TRAFFIC_BLOCK', 'DELAY'].includes(incident.incidentType) &&
    !this.replanningIncidentId;
}


contactDriver(incident: TruckIncident): void {
  if (!incident?.id) return;

  this.contactingIncidentId = incident.id;

  const message = this.buildDriverContactMessage(incident);

  this.driverNotificationService.contactDriver(incident.id, message).subscribe({
    next: (notification) => {
      this.contactingIncidentId = null;
      this.notificationByIncident[incident.id] = notification;
      this.autoDetectionMessage = `Notification envoyée au chauffeur du camion ${incident.truckCode}.`;
    },
    error: (err) => {
      console.error('Contact driver error:', err);
      this.contactingIncidentId = null;
      alert('Impossible de contacter le chauffeur.');
    }
  });
}

loadIncidentNotification(incident: TruckIncident): void {
  if (!incident?.id) return;

  this.driverNotificationService.getLatestByIncident(incident.id).subscribe({
    next: (notification) => {
      if (notification) {
        this.notificationByIncident[incident.id] = notification;
      }
    },
    error: () => {}
  });
}

getDriverResponseLabel(response?: string | null): string {
  switch (response) {
    case 'POSITION_CONFIRMED':
      return 'Position confirmée';
    case 'PROBLEM_RESOLVED':
      return 'Problème résolu';
    case 'NEED_ASSISTANCE':
      return 'Besoin d’assistance';
    default:
      return 'En attente de réponse';
  }
}

private buildDriverContactMessage(incident: TruckIncident): string {
  switch (incident.incidentType) {
    case 'GPS_LOST':
      return 'Perte GPS détectée. Merci de vérifier votre connexion et confirmer votre position.';
    
     
    case 'BREAKDOWN':
      return 'Panne camion signalée. Merci de confirmer si vous avez besoin d’assistance.';
    case 'TRAFFIC_BLOCK':
    case 'DELAY':
      return 'Retard ou blocage détecté. Merci de confirmer votre situation.';
    default:
      return 'Incident détecté. Merci de confirmer votre situation.';
  }
}
}