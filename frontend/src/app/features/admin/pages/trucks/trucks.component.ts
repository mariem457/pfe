import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FleetMapComponent, FleetMapInitialTruck } from './fleet-map/fleet-map.component';
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
export class TrucksComponent implements OnInit {
  missionColors = ['#2563eb', '#059669', '#7c3aed', '#f97316', '#475569', '#ef4444'];

  kpis = [
    { icon: 'local_shipping', label: 'Camions actifs', value: '0' },
    { icon: 'assignment', label: 'Missions en cours', value: '0' },
    { icon: 'check_circle', label: 'Camions hors mission', value: '0' },
    { icon: 'schedule', label: 'Progression moyenne', value: '0%' },
  ];

  trucks: TruckCard[] = [];
  missionTrucks: TruckCard[] = [];
  offMissionTrucks: TruckCard[] = [];
  mapTrucks: FleetMapInitialTruck[] = [];
  truckRoutes: any[] = [];

  openIncidents: TruckIncident[] = [];
  incidentMap: { [truckCode: string]: TruckIncident } = {};

  loadingIncidents = false;
  replanningIncidentId: number | null = null;
  resolvedIncidentId: number | null = null;

  constructor(
    private dashboardService: TruckDashboardService,
    private incidentService: TruckIncidentService,
    private replanService: RoutingReplanService
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
    this.loadOpenIncidents();
  }

  loadDashboard(): void {
    this.dashboardService.getDashboard().subscribe({
      next: (data: TruckDashboardResponse) => {
        this.trucks = data.trucks.map((t: TruckItem | any) => {
          const lat = t.lat ?? t.latitude ?? t.lastKnownLat ?? t.last_known_lat ?? null;
          const lng = t.lng ?? t.longitude ?? t.lastKnownLng ?? t.last_known_lng ?? null;
          const truckStatus = t.truckStatus ?? t.truck_status ?? 'UNKNOWN';
          const inMission = truckStatus === 'ON_MISSION';

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
            currentMissionId: t.currentMissionId ?? t.current_mission_id ?? null,
            lat,
            lng,
          };
        });

        this.missionTrucks = this.trucks.filter((t) => t.inMission);
        this.offMissionTrucks = this.trucks.filter((t) => !t.inMission);

        this.mapTrucks = this.missionTrucks
          .filter((t) => t.lat != null && t.lng != null)
          .map((t) => ({
            id: String(t.driverId ?? t.id),
            truckCode: t.id,
            lat: Number(t.lat),
            lng: Number(t.lng),
            label: t.id,
            progress: t.progress,
            fuelLevel: t.fuel,
            etaMinutes: t.etaMins,
            status: t.truckStatus,
            currentMissionId: t.currentMissionId ?? null,
          } as any));

        this.kpis = [
          {
            icon: 'local_shipping',
            label: 'Camions actifs',
            value: data.activeTrucks.toString(),
          },
          {
            icon: 'assignment',
            label: 'Missions en cours',
            value: this.missionTrucks.length.toString(),
          },
          {
            icon: 'check_circle',
            label: 'Camions hors mission',
            value: this.offMissionTrucks.length.toString(),
          },
          {
            icon: 'schedule',
            label: 'Progression moyenne',
            value: data.averageProgress + '%',
          },
        ];

        this.loadMissionRoutesForTrucks();

        console.log('DASHBOARD RAW:', data);
        console.log('MISSION TRUCKS:', this.missionTrucks);
        console.log('MAP TRUCKS:', this.mapTrucks);
      },
      error: (err: any) => {
        console.error('Dashboard trucks error:', err);
      },
    });
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

      console.log('TRUCK ROUTES:', this.truckRoutes);
    } catch (err) {
      console.error('Load truck routes error:', err);
      this.truckRoutes = [];
    }
  }

  getMissionPlace(index: number): string {
    const places = [
      'Porte de Versailles',
      'Champs-Élysées',
      'Alésia',
      'Place d’Italie',
      'Ménilmontant',
      'Montparnasse',
      'Bercy',
      'Nation',
    ];

    return places[index % places.length];
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
      default:
        return status || 'Inconnu';
    }
  }

  loadOpenIncidents(): void {
    this.loadingIncidents = true;

    this.incidentService.getOpenIncidents().subscribe({
      next: (data: TruckIncident[]) => {
        this.openIncidents = data;
        this.buildIncidentMap();
        this.loadingIncidents = false;
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

      if (!!incident.missionId && !existing.missionId) {
        this.incidentMap[incident.truckCode] = incident;
        return;
      }

      if (!!incident.missionId === !!existing.missionId && incident.id > existing.id) {
        this.incidentMap[incident.truckCode] = incident;
      }
    });
  }

  getIncidentForTruck(truckCode: string): TruckIncident | undefined {
    return this.incidentMap[truckCode];
  }

  resolveIncident(incident: TruckIncident): void {
    this.resolvedIncidentId = incident.id;

    this.incidentService.resolveIncident(incident.id, incident.description).subscribe({
      next: () => {
        alert('Incident marqué comme résolu.');
        this.resolvedIncidentId = null;
        this.loadOpenIncidents();
        this.loadDashboard();
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
                alert(`Replanification réussie pour ${incident.truckCode}.`);
                this.replanningIncidentId = null;
                this.loadOpenIncidents();
                this.loadDashboard();
              },
              error: () => {
                alert(
                  `Replanification réussie pour ${incident.truckCode}, mais l’incident n’a pas été fermé.`
                );
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
}