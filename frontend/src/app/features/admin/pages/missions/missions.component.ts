import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  MissionBinResponse,
  MissionResponse,
  MissionRouteResponse,
  MissionRouteStop,
  MissionService,
  RouteCoordinate
} from '../../../../services/mission.service';
import {
  FleetMapComponent,
  FleetMapMissionBinItem,
  FleetMapRouteStop
} from '../trucks/fleet-map/fleet-map.component';

@Component({
  selector: 'app-missions-page',
  standalone: true,
  imports: [CommonModule, FormsModule, FleetMapComponent],
  templateUrl: './missions.component.html',
  styleUrl: './missions.component.css'
})
export class MissionsComponent implements OnInit {
  missions = signal<MissionResponse[]>([]);
  selectedMission = signal<MissionResponse | null>(null);
  missionBins = signal<MissionBinResponse[]>([]);
  missionRouteCoordinates = signal<RouteCoordinate[]>([]);
  missionRouteStops = signal<MissionRouteStop[]>([]);
  snappedWaypoints = signal<RouteCoordinate[]>([]);

  loading = signal(false);
  loadingBins = signal(false);
  loadingRoute = signal(false);
  actionLoading = signal(false);
  errorMessage = signal<string | null>(null);

  searchTerm = '';
  selectedStatus = 'ALL';

  constructor(private missionService: MissionService) {}

  ngOnInit(): void {
    this.loadMissions();
  }

  filteredMissions = computed(() => {
    let data = [...this.missions()];

    if (this.selectedStatus !== 'ALL') {
      data = data.filter(m => m.status === this.selectedStatus);
    }

    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      data = data.filter(m =>
        (m.missionCode || '').toLowerCase().includes(q) ||
        (m.driverName || '').toLowerCase().includes(q) ||
        (m.zoneName || '').toLowerCase().includes(q) ||
        (m.status || '').toLowerCase().includes(q)
      );
    }

    return data.sort((a, b) => b.id - a.id);
  });

  totalMissions = computed(() => this.missions().length);

  completedMissions = computed(() =>
    this.missions().filter(m => m.status === 'COMPLETED').length
  );

  inProgressMissions = computed(() =>
    this.missions().filter(m => m.status === 'IN_PROGRESS').length
  );

  createdMissions = computed(() =>
    this.missions().filter(m => m.status === 'CREATED').length
  );

  totalBins = computed(() => this.missionBins().length);

  collectedBins = computed(() =>
    this.missionBins().filter(b => b.collected).length
  );

  progressPercent = computed(() => {
    const total = this.totalBins();
    if (!total) return 0;
    return Math.round((this.collectedBins() / total) * 100);
  });

  missionMapBins = computed<FleetMapMissionBinItem[]>(() =>
    this.missionBins()
      .filter(bin => bin.lat != null && bin.lng != null)
      .map(bin => ({
        id: bin.id,
        binId: bin.binId,
        binCode: bin.binCode,
        lat: bin.lat as number,
        lng: bin.lng as number,
        visitOrder: bin.visitOrder,
        collected: bin.collected,
        targetFillThreshold: bin.targetFillThreshold
      }))
  );

  missionMapRouteStops = computed<FleetMapRouteStop[]>(() =>
    this.missionRouteStops()
      .filter(stop => stop.lat != null && stop.lng != null)
      .map(stop => ({
        stopOrder: stop.stopOrder,
        stopType: stop.stopType,
        binId: stop.binId,
        lat: stop.lat,
        lng: stop.lng
      }))
  );

  loadMissions(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.missionService.getAllMissions().subscribe({
      next: (data) => {
        this.missions.set(data);
        this.loading.set(false);

        const currentSelected = this.selectedMission();

        if (!data.length) {
          this.selectedMission.set(null);
          this.missionBins.set([]);
          this.missionRouteCoordinates.set([]);
          this.missionRouteStops.set([]);
          this.snappedWaypoints.set([]);
          return;
        }

        if (currentSelected) {
          const updatedSelected = data.find(m => m.id === currentSelected.id);
          if (updatedSelected) {
            this.selectedMission.set(updatedSelected);
            return;
          }
        }

        this.selectMission(data[0]);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.message || 'Erreur lors du chargement des missions.'
        );
      }
    });
  }

  selectMission(mission: MissionResponse): void {
    this.selectedMission.set(mission);
    this.missionBins.set([]);
    this.missionRouteCoordinates.set([]);
    this.missionRouteStops.set([]);
    this.snappedWaypoints.set([]);
    this.loadMissionBins(mission.id);
    this.loadMissionRoute(mission.id);
  }

  loadMissionBins(missionId: number): void {
    this.loadingBins.set(true);

    this.missionService.getMissionBins(missionId).subscribe({
      next: (data) => {
        this.missionBins.set(data);
        this.loadingBins.set(false);
      },
      error: () => {
        this.loadingBins.set(false);
        this.missionBins.set([]);
      }
    });
  }

  loadMissionRoute(missionId: number): void {
    this.loadingRoute.set(true);

    this.missionService.getMissionRoute(missionId).subscribe({
      next: (data: MissionRouteResponse) => {
        this.missionRouteCoordinates.set(data?.routeCoordinates ?? []);
        this.missionRouteStops.set(data?.routeStops ?? []);
        this.snappedWaypoints.set(data?.snappedWaypoints ?? []);
        this.loadingRoute.set(false);
      },
      error: () => {
        this.missionRouteCoordinates.set([]);
        this.missionRouteStops.set([]);
        this.snappedWaypoints.set([]);
        this.loadingRoute.set(false);
      }
    });
  }

  refreshSelectedMission(): void {
    const mission = this.selectedMission();
    if (!mission) return;

    this.actionLoading.set(true);

    this.missionService.getMissionById(mission.id).subscribe({
      next: (data) => {
        this.selectedMission.set(data);
        this.loadMissionBins(data.id);
        this.loadMissionRoute(data.id);
        this.loadMissions();
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur lors du rafraîchissement.');
      }
    });
  }

  startMission(): void {
    const mission = this.selectedMission();
    if (!mission) return;

    this.actionLoading.set(true);

    this.missionService.startMission(mission.id).subscribe({
      next: (data) => {
        this.selectedMission.set(data);
        this.loadMissions();
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Impossible de démarrer la mission.');
      }
    });
  }

  completeMission(): void {
    const mission = this.selectedMission();
    if (!mission) return;

    this.actionLoading.set(true);

    this.missionService.completeMission(mission.id).subscribe({
      next: (data) => {
        this.selectedMission.set(data);
        this.loadMissions();
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Impossible de terminer la mission.');
      }
    });
  }

  collectBin(bin: MissionBinResponse): void {
    const mission = this.selectedMission();
    if (!mission || bin.collected) return;

    this.actionLoading.set(true);

    this.missionService.collectMissionBin(mission.id, bin.id, {
      driverId: mission.driverId,
      driverNote: 'Collecté depuis le dashboard',
      issueType: null,
      photoUrl: null
    }).subscribe({
      next: (data) => {
        this.selectedMission.set(data);
        this.loadMissionBins(mission.id);
        this.loadMissionRoute(mission.id);
        this.loadMissions();
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Impossible de collecter le bac.');
      }
    });
  }

  getStatusLabel(status: string | null): string {
    switch (status) {
      case 'CREATED':
        return 'Créée';
      case 'IN_PROGRESS':
        return 'En cours';
      case 'COMPLETED':
        return 'Terminée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return status || '—';
    }
  }

  getPriorityLabel(priority: string | null): string {
    switch (priority) {
      case 'HIGH':
        return 'Haute';
      case 'NORMAL':
        return 'Normale';
      case 'LOW':
        return 'Faible';
      default:
        return priority || '—';
    }
  }

  formatDate(value: string | null): string {
    if (!value) return '—';
    return new Date(value).toLocaleString('fr-FR');
  }

  statusClass(status: string | null): string {
    switch (status) {
      case 'COMPLETED':
        return 'badge success';
      case 'IN_PROGRESS':
        return 'badge primary';
      case 'CREATED':
        return 'badge warning';
      case 'CANCELLED':
        return 'badge danger';
      default:
        return 'badge';
    }
  }

  binStatusClass(bin: MissionBinResponse): string {
    if (bin.collected) return 'badge success';
    return 'badge neutral';
  }
}