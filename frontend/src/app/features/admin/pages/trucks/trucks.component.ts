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
  { icon: 'smart_toy', label: 'Incidents automatiques', value: '0' },
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
showReplanSuccess = false;
resolvedIncidentId: number | null = null;

  autoDetectionMessage = '';
  private alertSub = new Subscription();
  private refreshTimer: any;

constructor(
  private dashboardService: TruckDashboardService,
  private incidentService: TruckIncidentService,
  private replanService: RoutingReplanService,
  private alertService: AlertService
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
        if (!this.isToday((alert as any).createdAt ?? (alert as any).created_at)) return;

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



  private isInsideParis15(lat?: number, lng?: number): boolean {
  if (lat == null || lng == null) return false;

  return lat >= 48.815 &&
         lat <= 48.865 &&
         lng >= 2.250 &&
         lng <= 2.335;
}

private hasOpenIncident(truckCode: string): boolean {
  return !!this.incidentMap[truckCode];
}

private isMissionTruck(t: TruckCard): boolean {
  return t.inMission &&
         t.currentMissionId != null &&
         t.lat != null &&
         t.lng != null &&
         this.isInsideParis15(Number(t.lat), Number(t.lng));
}




getRouteColor(truckCode: string): string {
  const colors = ['#2563eb', '#059669', '#7c3aed', '#f97316', '#475569', '#dc2626'];
  let hash = 0;

  for (let i = 0; i < truckCode.length; i++) {
    hash = truckCode.charCodeAt(i) + ((hash << 5) - hash);
  }

  return colors[Math.abs(hash) % colors.length];
}










  loadDashboard(): void {
    this.dashboardService.getDashboard().subscribe({
      next: (data: TruckDashboardResponse) => {
        this.trucks = data.trucks.map((t: TruckItem | any) => {
          console.log('RAW TRUCK FROM DASHBOARD =>', t);
          const lat = t.lat ?? t.latitude ?? t.lastKnownLat ?? t.last_known_lat ?? null;
          const lng = t.lng ?? t.longitude ?? t.lastKnownLng ?? t.last_known_lng ?? null;
          const truckStatus = t.truckStatus ?? t.truck_status ?? 'UNKNOWN';
         const currentMissionId =
  t.currentMissionId ??
  t.current_mission_id ??
  t.missionId ??
  t.mission_id ??
  t.activeMissionId ??
  t.active_mission_id ??
  t.currentMission?.id ??
  t.current_mission?.id ??
  null;

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

this.missionTrucks = this.trucks.filter((t) => this.isMissionTruck(t));

this.offMissionTrucks = this.trucks.filter((t) => !this.isMissionTruck(t));

this.mapTrucks = this.missionTrucks
  .filter(t =>
    t.inMission &&
    t.currentMissionId != null &&
    t.lat != null &&
    t.lng != null
  )
  .map((t) => {
    const incident = this.getIncidentForTruck(t.id);

    return {
      id: String(t.driverId ?? t.id),
      truckCode: t.id,
      lat: Number(t.lat),
      lng: Number(t.lng),

      label: `${t.id} · Mission #${t.currentMissionId}`,

      progress: t.progress,
      fuelLevel: t.fuel,
      etaMinutes: t.etaMins,

      status: incident ? 'INCIDENT' : 'ON_MISSION',

      currentMissionId: t.currentMissionId,
      missionId: t.currentMissionId,
      driverName: t.driver,

      incidentType: incident?.incidentType ?? null,
      incidentLabel: incident ? this.getIncidentTypeLabel(incident.incidentType) : null,

      routeColor: this.getRouteColor(t.id),
    } as FleetMapInitialTruck;
  });

  console.log('TRUCKS NORMALIZED =>', this.trucks);
console.log('MISSION TRUCKS =>', this.missionTrucks);
console.log('MAP TRUCKS =>', this.mapTrucks);


        this.updateKpis(data);
        this.truckRoutes = [];
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
  label: 'Incidents automatiques',
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

  const normalizeCoords = (value: any): any[] => {
    if (!value) return [];

    let raw = value;

    if (typeof value === 'string') {
      try {
        raw = JSON.parse(value);
      } catch {
        return [];
      }
    }

    if (!Array.isArray(raw)) return [];

    return raw
      .map((p: any) => {
        if (Array.isArray(p)) {
          return {
            lat: Number(p[0]),
            lng: Number(p[1]),
          };
        }

        return {
          lat: Number(
            p.lat ??
            p.latitude ??
            p.y
          ),
          lng: Number(
            p.lng ??
            p.longitude ??
            p.lon ??
            p.x
          ),
        };
      })
      .filter((p: any) =>
        Number.isFinite(p.lat) &&
        Number.isFinite(p.lng)
      );
  };

  const coordsFromStops = (route: any): any[] => {
    const stops =
      route.routeStops ??
      route.route_stops ??
      route.stops ??
      route.routePlanStops ??
      route.route_plan_stops ??
      [];

    return normalizeCoords(stops);
  };

  try {
    const results = await Promise.all(
      trucksWithMission.map(async (truck) => {
        try {
          const route: any = await firstValueFrom(
            this.dashboardService.getMissionRoute(Number(truck.currentMissionId))
          );

          console.log('ROUTE RESPONSE FOR', truck.id, truck.currentMissionId, route);

          let routeCoordinates = normalizeCoords(
            route.routeCoordinates ??
            route.route_coordinates ??
            route.coordinates ??
            route.geometry ??
            route.fullRouteCoordinates ??
            route.full_route_coordinates ??
            route.routePlan?.routeCoordinates ??
            route.routePlan?.route_coordinates ??
            route.routePlan?.coordinates ??
            route.routePlan?.geometry
          );

          const collectionRouteCoordinates = normalizeCoords(
            route.collectionRouteCoordinates ??
            route.collection_route_coordinates ??
            route.routePlan?.collectionRouteCoordinates ??
            route.routePlan?.collection_route_coordinates
          );

          const transferRouteCoordinates = normalizeCoords(
            route.transferRouteCoordinates ??
            route.transfer_route_coordinates ??
            route.routePlan?.transferRouteCoordinates ??
            route.routePlan?.transfer_route_coordinates
          );

          // fallback: ken routeCoordinates fergha, nesta3mlou route stops
          if (!routeCoordinates.length) {
            routeCoordinates = coordsFromStops(route);
          }

          console.log('NORMALIZED ROUTE FOR', truck.id, {
            missionId: route.missionId ?? route.mission_id ?? truck.currentMissionId,
            routeCoordinates,
            collectionRouteCoordinates,
            transferRouteCoordinates,
          });

          return {
            truckId: String(truck.driverId ?? truck.id),
            truckCode: truck.id,
            missionId: route.missionId ?? route.mission_id ?? truck.currentMissionId,
            routeCoordinates,
            collectionRouteCoordinates,
            transferRouteCoordinates,
            routeColor: this.getRouteColor(truck.id),
          };
        } catch (err) {
          console.error('Route error for truck', truck.id, truck.currentMissionId, err);
          return null;
        }
      })
    );

    this.truckRoutes = results.filter((r): r is any => !!r);

    console.log('TRUCK ROUTES SENT TO MAP =>', this.truckRoutes);
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
  this.showReplanSuccess = false;

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
              this.showReplanSuccess = true;

              this.loadOpenIncidents();
              this.loadTruckAlerts();
              this.loadDashboard();

              setTimeout(() => {
                this.showReplanSuccess = false;
              }, 2500);
            },
            error: (err: any) => {
              console.error('Resolve incident after replan error:', err);

              this.replanningIncidentId = null;
              this.showReplanSuccess = true;

              this.loadOpenIncidents();
              this.loadTruckAlerts();
              this.loadDashboard();

              setTimeout(() => {
                this.showReplanSuccess = false;
              }, 2500);
            },
          });
      },
      error: (err: any) => {
        console.error('REPLAN ERROR FULL:', err);

        const message =
          err?.error?.message ||
          err?.error?.error ||
          (typeof err?.error === 'string' ? err.error : null) ||
          err?.message ||
          'Erreur inconnue lors de la replanification.';

        this.replanningIncidentId = null;
        this.showReplanSuccess = false;
        alert(message);
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
  private isToday(value?: string | null): boolean {
  if (!value) return false;

  const d = new Date(value);
  const today = new Date();

  return d.getFullYear() === today.getFullYear()
    && d.getMonth() === today.getMonth()
    && d.getDate() === today.getDate();
}
loadTruckAlerts(): void {
  this.alertService.searchAlerts({
    resolved: false,
    entityType: 'INCIDENT'
  }).subscribe({
    next: (alerts: AlertDto[]) => {
      this.alerts = (alerts || []).filter((a: any) => {
        const type = a.alertType ?? a.alert_type ?? '';

        const isFleetAlert = [
          'TRUCK_GPS_LOST',
          'TRUCK_OVERLOAD',
          'TRUCK_BREAKDOWN',
          'TRUCK_TRAFFIC_BLOCK',
          'TRUCK_DELAY',
          'DRIVER_UNAVAILABLE'
        ].includes(type);

        return isFleetAlert && this.isToday(a.createdAt ?? a.created_at);
      });

      console.log('Fleet truck alerts today shown:', this.alerts);
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
    ['TRAFFIC_BLOCK', 'DELAY'].includes(incident.incidentType) &&
    !this.replanningIncidentId;
}


getFleetAlertTitle(alert: AlertDto): string {
  const truck = alert.truckCode || 'Camion';
  const type = alert.alertType || '';

  switch (type) {
    case 'TRUCK_BREAKDOWN':
      return `${truck} - Panne`;
    case 'TRUCK_GPS_LOST':
      return `${truck} - GPS perdu`;
    case 'TRUCK_OVERLOAD':
      return `${truck} - Surcharge`;
    case 'TRUCK_TRAFFIC_BLOCK':
      return `${truck} - Trafic bloqué`;
    case 'TRUCK_DELAY':
      return `${truck} - Retard`;
    case 'DRIVER_UNAVAILABLE':
      return `${truck} - Chauffeur indisponible`;
    default:
      return alert.title || 'Alerte flotte';
  }
}
}