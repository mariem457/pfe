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
  styleUrls: ['./missions.component.css']
})
export class MissionsComponent implements OnInit {

  missions = signal<MissionResponse[]>([]);
  selectedMission = signal<MissionResponse | null>(null);
  missionBins = signal<MissionBinResponse[]>([]);
  missionRouteCoordinates = signal<RouteCoordinate[]>([]);
  missionRouteStops = signal<MissionRouteStop[]>([]);
  snappedWaypoints = signal<RouteCoordinate[]>([]);

  routeMatrixSource = signal<string | null>(null);
  routeGeometrySource = signal<string>('OSRM');

  loading = signal(false);
  loadingBins = signal(false);
  loadingRoute = signal(false);
  actionLoading = signal(false);
  generatingAi = signal(false);

  errorMessage = signal<string | null>(null);
  aiMessage = signal<string | null>(null);

  // IA batch
  latestAiBatchLabel = signal<string | null>(null);
  latestAiMissionIds = signal<number[]>([]);
  showOnlyLatestAi = signal(false);

  searchTerm = '';
  selectedStatus = 'ALL';

  constructor(private missionService: MissionService) {}

  ngOnInit(): void {
    this.loadMissions();
  }

  filteredMissions = computed(() => {
    let data = [...this.missions()];

    if (this.showOnlyLatestAi()) {
      const latestIds = this.latestAiMissionIds();
      data = data.filter(m => latestIds.includes(m.id));
    }

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

        if (!data.length) return;

        this.selectMission(data[0]);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur chargement missions');
      }
    });
  }

  selectMission(mission: MissionResponse): void {
    this.selectedMission.set(mission);
    this.loadMissionBins(mission.id);
    this.loadMissionRoute(mission.id);
  }

  loadMissionBins(id: number): void {
    this.loadingBins.set(true);

    this.missionService.getMissionBins(id).subscribe({
      next: (data) => {
        this.missionBins.set(data);
        this.loadingBins.set(false);
      },
      error: () => {
        this.loadingBins.set(false);
      }
    });
  }

  loadMissionRoute(id: number): void {
    this.loadingRoute.set(true);

    this.missionService.getMissionRoute(id).subscribe({
      next: (data: MissionRouteResponse) => {
        this.missionRouteCoordinates.set(data?.routeCoordinates ?? []);
        this.missionRouteStops.set(data?.routeStops ?? []);
        this.snappedWaypoints.set(data?.snappedWaypoints ?? []);
        this.routeMatrixSource.set(data?.matrixSource ?? null);
        this.routeGeometrySource.set(data?.geometrySource ?? 'OSRM');
        this.loadingRoute.set(false);
      },
      error: () => {
        this.loadingRoute.set(false);
      }
    });
  }

  generateAiMissions(): void {
    if (this.generatingAi()) return;

    this.generatingAi.set(true);

    setTimeout(() => {
      const ids = this.missions()
        .slice(0, 3)
        .map(m => m.id);

      this.latestAiMissionIds.set(ids);
      this.latestAiBatchLabel.set('IA-BATCH');
      this.showOnlyLatestAi.set(true);

      this.generatingAi.set(false);
      this.aiMessage.set('Missions IA générées');
    }, 700);
  }

  toggleLatestAiFilter(): void {
    this.showOnlyLatestAi.set(!this.showOnlyLatestAi());
  }

  clearLatestAiBatch(): void {
    this.latestAiMissionIds.set([]);
    this.latestAiBatchLabel.set(null);
    this.showOnlyLatestAi.set(false);
  }

  isInLatestAiBatch(id: number): boolean {
    return this.latestAiMissionIds().includes(id);
  }

  getStatusLabel(status: string | null): string {
    return status || '—';
  }

  getPriorityLabel(priority: string | null): string {
    return priority || '—';
  }

  formatDate(v: string | null): string {
    return v ? new Date(v).toLocaleString() : '—';
  }

  statusClass(): string {
    return 'badge';
  }

  binStatusClass(bin: MissionBinResponse): string {
    return bin.collected ? 'badge success' : 'badge';
  }
}