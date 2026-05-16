import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AlertService, AlertDto } from '../../../../services/alert.service';
import { RealtimeService, MissionRealtimeEvent } from '../../../../services/realtime.service';
import { FormsModule } from '@angular/forms';
import {
  MissionBinResponse,
  MissionReassignmentResponse,
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

type MissionTab = 'overview' | 'map' | 'bins' | 'alerts';
type MissionJournalKind =
  | 'created'
  | 'started'
  | 'collection'
  | 'replan'
  | 'replacement'
  | 'alert'
  | 'completed'
  | 'system';

interface MissionJournalItem {
  kind: MissionJournalKind;
  time: string | null;
  title: string;
  message: string;
  actor: string;
}

@Component({
  selector: 'app-missions-page',
  standalone: true,
  imports: [CommonModule, FormsModule, FleetMapComponent],
  templateUrl: './missions.component.html',
  styleUrls: ['./missions.component.css']
})
export class MissionsComponent implements OnInit, OnDestroy {
  missions = signal<MissionResponse[]>([]);
  selectedMission = signal<MissionResponse | null>(null);
  missionBins = signal<MissionBinResponse[]>([]);
  missionRouteCoordinates = signal<RouteCoordinate[]>([]);
  collectionRouteCoordinates = signal<RouteCoordinate[]>([]);
  transferRouteCoordinates = signal<RouteCoordinate[]>([]);
  missionRouteStops = signal<MissionRouteStop[]>([]);
  snappedWaypoints = signal<RouteCoordinate[]>([]);

  routeMatrixSource = signal<string | null>(null);
  routeGeometrySource = signal<string>('OSRM');
  routeTotalDistanceKm = signal<number>(0);

  loading = signal(false);
  loadingBins = signal(false);
  loadingRoute = signal(false);
  actionLoading = signal(false);
  generatingAi = signal(false);

  errorMessage = signal<string | null>(null);
  aiMessage = signal<string | null>(null);

  latestAiBatchLabel = signal<string | null>(null);
  latestAiMissionIds = signal<number[]>([]);
  showOnlyLatestAi = signal(false);

  activeTab = signal<MissionTab>('overview');

  searchTerm = signal('');
  selectedStatus = signal('ALL');
  showHistory = signal(false);

  debugMode = signal(false);
  routeDistances = signal<number[]>([]);
  missionAlerts = signal<AlertDto[]>([]);
  loadingMissionAlerts = signal(false);
  missionReassignments = signal<MissionReassignmentResponse[]>([]);
loadingReassignments = signal(false);
  private alertSub = new Subscription();

  trafficEnabled = signal(false);
  trafficSource = signal<string | null>(null);
  trafficDelayMin = signal<number>(0);
  trafficLevel = signal<string>('FLUID');
  roadClosed = signal(false);
  estimatedBaseDurationMin = signal<number | null>(null);
  estimatedRealDurationMin = signal<number | null>(null);

  constructor(
    private missionService: MissionService,
    private alertService: AlertService,
    private realtimeService: RealtimeService
  ) { }

 ngOnInit(): void {
  this.loadMissions();

  this.realtimeService.connectAll();
  this.subscribeToMissionRealtime();

  this.alertSub.add(
    this.alertService.realtimeAlert$.subscribe(alert => {
      const mission = this.selectedMission();
      if (!mission) return;

      if (alert.missionId === mission.id && !alert.resolved) {
        const exists = this.missionAlerts().some(a => a.id === alert.id);
        if (!exists) {
          this.missionAlerts.update(list => [alert, ...list]);
        }
      }
    })
  );

  this.alertSub.add(
    this.alertService.realtimeResolved$.subscribe(alert => {
      this.missionAlerts.update(list => list.filter(a => a.id !== alert.id));
    })
  );

  }

  ngOnDestroy(): void {
    this.alertSub.unsubscribe();
  }
  private subscribeToMissionRealtime(): void {
  this.alertSub.add(
    this.realtimeService.missionEvents$.subscribe(event => {
      if (!event || !event.type) return;
      this.handleMissionRealtimeEvent(event);
    })
  );
}

private handleMissionRealtimeEvent(event: MissionRealtimeEvent): void {
  const eventMissionId = Number(event.missionId ?? event.oldMissionId ?? 0);
  if (!eventMissionId) return;

  const selected = this.selectedMission();
  const isSelectedMission = selected?.id === eventMissionId;

  switch (event.type) {
    case 'MISSION_STATUS_CHANGED':
    case 'MISSION_COMPLETED':
    case 'MISSION_CANCELLED':
      this.applyMissionStatusEvent(event);

      if (isSelectedMission) {
        this.reloadSelectedMissionLight(eventMissionId);
      }
      break;

    case 'MISSION_BIN_COLLECTED':
    case 'MISSION_BIN_SKIPPED':
      this.applyMissionStatusEvent(event);

      if (isSelectedMission) {
        this.loadMissionBins(eventMissionId);
        this.loadMissionAlerts(eventMissionId);
      }
      break;

    case 'MISSION_ALERT_CREATED':
    case 'MISSION_ALERT_RESOLVED':
      if (isSelectedMission) {
        this.loadMissionAlerts(eventMissionId);
      }
      break;

    case 'MISSION_URGENT_BIN_INSERTED':
    case 'MISSION_REPLANNED':
    case 'MISSION_PARTIALLY_REASSIGNED':
      this.loadMissions();

      if (isSelectedMission) {
        this.reloadSelectedMissionFull(eventMissionId);
      }
      break;

    default:
      if (isSelectedMission) {
        this.reloadSelectedMissionLight(eventMissionId);
      }
      break;
  }
}

private applyMissionStatusEvent(event: MissionRealtimeEvent): void {
  const missionId = Number(event.missionId ?? event.oldMissionId ?? 0);
  if (!missionId) return;

  this.missions.update(list =>
    this.sortMissionList(
      list.map(m => {
        if (m.id !== missionId) return m;

        return {
          ...m,
          status: event.status ?? m.status,
          missionStatusDetail: event.missionStatusDetail ?? m.missionStatusDetail,
          mission_status_detail: event.missionStatusDetail ?? m.mission_status_detail
        };
      })
    )
  );

  const selected = this.selectedMission();

  if (selected?.id === missionId) {
    this.selectedMission.set({
      ...selected,
      status: event.status ?? selected.status,
      missionStatusDetail: event.missionStatusDetail ?? selected.missionStatusDetail,
      mission_status_detail: event.missionStatusDetail ?? selected.mission_status_detail
    });
  }
}

private reloadSelectedMissionLight(missionId: number): void {
  this.missionService.getMissionById(missionId).subscribe({
    next: mission => {
      this.selectedMission.set(mission);

      this.missions.update(list =>
        this.sortMissionList(
          list.map(item => item.id === mission.id ? mission : item)
        )
      );
    },
    error: () => {}
  });
}

private reloadSelectedMissionFull(missionId: number): void {
  this.missionService.getMissionById(missionId).subscribe({
    next: mission => {
      this.selectedMission.set(mission);

      this.missions.update(list =>
        this.sortMissionList(
          list.map(item => item.id === mission.id ? mission : item)
        )
      );

      this.loadMissionBins(missionId);
      this.loadMissionRoute(missionId);
      this.loadMissionAlerts(missionId);
      this.loadMissionReassignments(missionId);
    },
    error: () => {
      this.loadMissions();
    }
  });
}

  private sortMissionList(list: MissionResponse[]): MissionResponse[] {
    const statusOrder: Record<string, number> = {
      IN_PROGRESS: 0,
      CREATED: 1,
      COMPLETED: 2,
      CANCELLED: 3
    };

    return [...list].sort((a, b) => {
      const aStatus = statusOrder[a.status || ''] ?? 99;
      const bStatus = statusOrder[b.status || ''] ?? 99;

      if (aStatus !== bStatus) {
        return aStatus - bStatus;
      }

      return b.id - a.id;
    });
  }

filteredMissions = computed(() => {
  const today = this.todayIsoDate();

  let data = [...this.missions()].filter(m => {
    const planned = this.normalizeDateOnly(m.plannedDate);

    if (this.showHistory()) {
      return !!planned && planned < today;
    }

    return planned === today;
  });

  if (this.showOnlyLatestAi()) {
    const latestIds = this.latestAiMissionIds();
    data = data.filter(m => latestIds.includes(m.id));
  }

  const status = this.selectedStatus();
  const term = this.searchTerm().trim();

  if (status !== 'ALL') {
    data = data.filter(m => m.status === status);
  }

  if (term) {
    const q = term.toLowerCase();
    data = data.filter(m =>
      (m.missionCode || '').toLowerCase().includes(q) ||
      (m.driverName || '').toLowerCase().includes(q) ||
      (m.zoneName || '').toLowerCase().includes(q) ||
      (m.status || '').toLowerCase().includes(q) ||
      (m.plannedDate || '').toLowerCase().includes(q)
    );
  }

  return this.sortMissionList(data);
});

  groupedMissions = computed(() => {
    const data = this.filteredMissions();

    return {
      inProgress: data.filter(m => m.status === 'IN_PROGRESS'),
      created: data.filter(m => m.status === 'CREATED'),
      completed: data.filter(m => m.status === 'COMPLETED'),
      cancelled: data.filter(m => m.status === 'CANCELLED')
    };
  });
  todayMissionsCount = computed(() => {
  const today = this.todayIsoDate();
  return this.missions().filter(m => this.normalizeDateOnly(m.plannedDate) === today).length;
});

historyMissionsCount = computed(() => {
  const today = this.todayIsoDate();
  return this.missions().filter(m => {
    const planned = this.normalizeDateOnly(m.plannedDate);
    return !!planned && planned < today;
  }).length;
});

private todayIsoDate(): string {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

private normalizeDateOnly(value: string | null | undefined): string | null {
  if (!value) return null;
  return String(value).slice(0, 10);
}

toggleHistory(): void {
  this.showHistory.set(!this.showHistory());
  this.selectedStatus.set('ALL');
  this.searchTerm.set('');

  const first = this.filteredMissions()[0] || null;
  if (first) {
    this.selectMission(first);
  } else {
    this.selectedMission.set(null);
    this.missionBins.set([]);
    this.missionRouteCoordinates.set([]);
    this.collectionRouteCoordinates.set([]);
    this.transferRouteCoordinates.set([]);
    this.missionRouteStops.set([]);
    this.snappedWaypoints.set([]);
    this.missionAlerts.set([]);
    this.missionReassignments.set([]);
  }
}
  newestMissionId = computed(() => {
    const all = [...this.missions()].sort((a, b) => b.id - a.id);
    return all.length ? all[0].id : null;
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

  remainingBins = computed(() =>
    this.missionBins().filter(b => !b.collected && b.assignmentStatus !== 'REASSIGNED').length
  );
  reassignedBins = computed(() =>
    this.missionBins().filter(b => b.assignmentStatus === 'REASSIGNED').length
  );

  progressPercent = computed(() => {
    const total = this.totalBins();
    if (!total) return 0;
    return Math.round((this.collectedBins() / total) * 100);
  });
  missionWasteTypes = computed(() => {
    const types = this.missionBins()
      .map(bin => (bin.wasteType || '').trim().toUpperCase())
      .filter(type => !!type);

    return [...new Set(types)];
  });

  missionJournal = computed<MissionJournalItem[]>(() => {
  const mission = this.selectedMission();

  if (!mission) {
    return [];
  }

  const items: MissionJournalItem[] = [];

  const detail = this.getMissionStatusDetail(mission);
  const bins = this.missionBins();
  const reassignments = this.missionReassignments();

  const collected = bins.filter(b => b.collected).length;
  const reassigned = bins.filter(b => b.assignmentStatus === 'REASSIGNED').length;

  const targetTruckIds = [
    ...new Set(
      bins
        .filter(b => b.assignmentStatus === 'REASSIGNED' && b.reassignedToTruckId != null)
        .map(b => b.reassignedToTruckId as number)
    )
  ];

  const sourceTruckIds = [
    ...new Set(
      bins
        .filter(b => b.assignmentStatus === 'REASSIGNED' && b.reassignedFromTruckId != null)
        .map(b => b.reassignedFromTruckId as number)
    )
  ];

  const replacementMission = this.missions().find(m =>
    m.id !== mission.id &&
    (m.notes || '').toLowerCase().includes(`mission ${mission.id}`)
  );

  items.push({
    kind: 'created',
    time: mission.createdAt,
    title: 'Mission générée par optimisation IA',
    message: 'Création automatique de la tournée avec OR-Tools, OSRM et contraintes métier.',
    actor: 'Automatique'
  });

  if (mission.startedAt) {
    items.push({
      kind: 'started',
      time: mission.startedAt,
      title: 'Mission démarrée',
      message: 'Le chauffeur a commencé la tournée de collecte depuis l’application mobile.',
      actor: 'Chauffeur'
    });
  }

  if (collected > 0) {
    items.push({
      kind: 'collection',
      time: null,
      title: `${collected} bac(s) collecté(s)`,
      message: `La progression de collecte est mise à jour en temps réel. Bacs restants non réaffectés: ${this.remainingBins()}.`,
      actor: 'Chauffeur'
    });
  }

  if (this.isMissionReplanned(mission)) {
    const sourceText = sourceTruckIds.length
      ? ` depuis camion #${sourceTruckIds.join(', #')}`
      : '';

    const targetText = targetTruckIds.length
      ? ` vers camion #${targetTruckIds.join(', #')}`
      : '';

    items.push({
      kind: 'replan',
      time: reassignments[0]?.reassignedAt || null,
      title: 'Replanification dynamique effectuée',
      message:
        reassigned > 0
          ? `${reassigned} bac(s) restant(s) ont été réaffectés automatiquement${sourceText}${targetText}.`
          : this.getReplanJournalMessage(mission),
      actor: 'Système IA'
    });
  }

  if (replacementMission) {
    items.push({
      kind: 'replacement',
      time: replacementMission.createdAt,
      title: `Mission de remplacement créée: ${replacementMission.missionCode}`,
      message: `Nouvelle mission prioritaire générée pour reprendre les bacs non collectés. Statut: ${this.getStatusLabel(replacementMission.status)}.`,
      actor: 'Automatique'
    });
  }

  if ((mission.notes || '').toLowerCase().includes('replanifiee a partir')) {
    items.push({
      kind: 'replacement',
      time: mission.createdAt,
      title: 'Mission issue d’une replanification',
      message: mission.notes || 'Cette mission a été créée automatiquement suite à un incident camion.',
      actor: 'Automatique'
    });
  }

  if (this.missionAlerts().length > 0) {
    items.push({
      kind: 'alert',
      time: this.missionAlerts()[0]?.createdAt || null,
      title: `${this.missionAlerts().length} alerte(s) ouverte(s)`,
      message: 'Des alertes opérationnelles sont encore liées à cette mission.',
      actor: 'Monitoring'
    });
  }

  if (mission.completedAt || detail === 'COMPLETED') {
    items.push({
      kind: 'completed',
      time: mission.completedAt,
      title: 'Mission terminée',
      message: 'La collecte associée à cette mission est clôturée.',
      actor: 'Système'
    });
  }

  return items;
});

missionMapBins = computed<FleetMapMissionBinItem[]>(() => {
  const routeStops = this.missionRouteStops();

  return this.missionBins()
    .filter(bin => bin.lat != null && bin.lng != null)
    .map(bin => {
      const stop = routeStops.find(s =>
        s.stopType === 'BIN_PICKUP' &&
        s.binId === bin.binId &&
        s.lat != null &&
        s.lng != null
      );

      return {
        id: bin.id,
        binId: bin.binId,
        binCode: bin.binCode,

        // المهم: marker الأزرق يمشي لنفس point متاع route stop
        lat: stop ? Number(stop.lat) : Number(bin.lat),
        lng: stop ? Number(stop.lng) : Number(bin.lng),

        visitOrder: bin.visitOrder,
        collected: bin.collected,
        targetFillThreshold: bin.targetFillThreshold,
        wasteType: (bin as any).wasteType ?? null,

        fillLevel: (bin as any).fillLevel ?? null,
        batteryLevel: (bin as any).batteryLevel ?? null,
        status: (bin as any).status ?? null,
        zoneName: (bin as any).zoneName ?? null,
        clusterId: (bin as any).clusterId ?? null,

        priorityScore: (bin as any).priorityScore ?? null,
        predictedFillLevelNext: (bin as any).predictedFillLevelNext ?? null,
        hoursToFull: (bin as any).hoursToFull ?? null,
        alertStatus: (bin as any).alertStatus ?? null,
        shouldCollect: (bin as any).shouldCollect ?? null,

        decisionReason: (bin as any).decisionReason ?? null,
        scoreExplanation: (bin as any).scoreExplanation ?? null,
        urgencyExplanation: (bin as any).urgencyExplanation ?? null,
        feedbackExplanation: (bin as any).feedbackExplanation ?? null,
        postponementExplanation: (bin as any).postponementExplanation ?? null,
        classificationExplanation: (bin as any).classificationExplanation ?? null
      };
    });
});

  missionMapRouteStops = computed<FleetMapRouteStop[]>(() =>
    this.missionRouteStops()
      .filter(stop => stop.lat != null && stop.lng != null)
      .map(stop => ({
        stopOrder: stop.stopOrder,
        stopType: stop.stopType,
        binId: stop.binId,

        fuelStationId: stop.fuelStationId ?? null,
        fuelStationName: stop.fuelStationName ?? null,

        disposalSiteId: stop.disposalSiteId ?? null,
        disposalSiteName: stop.disposalSiteName ?? null,

        lat: stop.lat,
        lng: stop.lng
      }))
  );

  matrixSourceLabel = computed(() => {
    const source = (this.routeMatrixSource() || '').toUpperCase();

    switch (source) {
      case 'TOMTOM':
        return 'TomTom Traffic réel';
      case 'OSRM':
        return 'OSRM standard';
      case 'FALLBACK':
        return 'Approximation Haversine';
      case 'NONE':
        return 'Aucune matrice';
      default:
        return source || '—';
    }
  });

  matrixSourceClass = computed(() => {
    const source = (this.routeMatrixSource() || '').toUpperCase();

    switch (source) {
      case 'TOMTOM':
        return 'badge success';
      case 'OSRM':
        return 'badge primary';
      case 'FALLBACK':
        return 'badge warning';
      case 'NONE':
        return 'badge danger';
      default:
        return 'badge neutral';
    }
  });

  geometrySourceLabel = computed(() => {
    const source = (this.routeGeometrySource() || '').toUpperCase();

    switch (source) {
      case 'OSRM':
        return 'OSRM';
      default:
        return source || '—';
    }
  });

  geometrySourceClass = computed(() => {
    const source = (this.routeGeometrySource() || '').toUpperCase();

    switch (source) {
      case 'OSRM':
        return 'badge primary';
      default:
        return 'badge neutral';
    }
  });

  setActiveTab(tab: MissionTab): void {
    this.activeTab.set(tab);
  }

  loadMissions(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.missionService.getAllMissions().subscribe({
      next: (data) => {
        const sorted = this.sortMissionList(data || []);
        this.missions.set(sorted);
        this.loading.set(false);

        const currentSelected = this.selectedMission();

        if (!sorted.length) {
          this.selectedMission.set(null);
          this.missionBins.set([]);
          this.missionRouteCoordinates.set([]);
          this.collectionRouteCoordinates.set([]);
          this.transferRouteCoordinates.set([]);
          this.missionRouteStops.set([]);
          this.snappedWaypoints.set([]);
          this.routeMatrixSource.set(null);
          this.routeGeometrySource.set('OSRM');
          this.routeTotalDistanceKm.set(0);
          this.routeDistances.set([]);
          this.debugMode.set(false);
          return;
        }

        if (currentSelected) {
          const updatedSelected = sorted.find(m => m.id === currentSelected.id);
          if (updatedSelected) {
            this.selectedMission.set(updatedSelected);
            return;
          }
        }

        this.selectMission(sorted[0]);
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
    this.errorMessage.set(null);
    this.selectedMission.set(mission);
    this.activeTab.set('overview');

    this.missionBins.set([]);
    this.missionRouteCoordinates.set([]);
    this.collectionRouteCoordinates.set([]);
    this.transferRouteCoordinates.set([]);
    this.missionRouteStops.set([]);
    this.missionReassignments.set([]);
    this.snappedWaypoints.set([]);
    this.routeMatrixSource.set(null);
    this.routeGeometrySource.set('OSRM');
    this.routeTotalDistanceKm.set(0);
    this.routeDistances.set([]);
    this.debugMode.set(false);

    this.loadMissionBins(mission.id);
    this.loadMissionRoute(mission.id);
    this.loadMissionAlerts(mission.id);
    

    this.loadMissionReassignments(mission.id);
  }

  loadMissionBins(missionId: number): void {
    this.loadingBins.set(true);

    this.missionService.getMissionBins(missionId).subscribe({
      next: (data) => {
        this.missionBins.set(data || []);
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
        this.collectionRouteCoordinates.set(data?.collectionRouteCoordinates ?? []);
        this.transferRouteCoordinates.set(data?.transferRouteCoordinates ?? []);
        this.missionRouteStops.set(data?.routeStops ?? []);
        this.snappedWaypoints.set(data?.snappedWaypoints ?? []);
        this.routeMatrixSource.set(data?.matrixSource ?? null);
        this.routeGeometrySource.set(data?.geometrySource ?? 'OSRM');
        this.routeTotalDistanceKm.set(data?.totalDistanceKm ?? 0);
        this.routeDistances.set(data?.stopLegDistancesKm ?? []);
        this.loadingRoute.set(false);

        this.trafficEnabled.set(!!data?.trafficEnabled);
        this.trafficSource.set(data?.trafficSource ?? data?.matrixSource ?? null);
        this.trafficDelayMin.set(Number(data?.trafficDelayMin ?? 0));
        this.trafficLevel.set(data?.trafficLevel ?? 'FLUID');
        this.roadClosed.set(!!data?.roadClosed);
        this.estimatedBaseDurationMin.set(data?.estimatedBaseDurationMin ?? null);
        this.estimatedRealDurationMin.set(data?.estimatedDurationMin ?? null);
      },
      error: () => {
        this.missionRouteCoordinates.set([]);
        this.collectionRouteCoordinates.set([]);
        this.transferRouteCoordinates.set([]);
        this.missionRouteStops.set([]);
        this.snappedWaypoints.set([]);
        this.routeMatrixSource.set(null);
        this.routeGeometrySource.set('OSRM');
        this.routeTotalDistanceKm.set(0);
        this.routeDistances.set([]);
        this.loadingRoute.set(false);

        this.trafficEnabled.set(false);
        this.trafficSource.set(null);
        this.trafficDelayMin.set(0);
        this.trafficLevel.set('FLUID');
        this.roadClosed.set(false);
        this.estimatedBaseDurationMin.set(null);
        this.estimatedRealDurationMin.set(null);
      }
    });
  }

  refreshSelectedMission(): void {
    const mission = this.selectedMission();
    if (!mission) return;

    this.actionLoading.set(true);
    this.errorMessage.set(null);

    this.missionService.getMissionById(mission.id).subscribe({
      next: (data) => {
        this.selectedMission.set(data);
        this.loadMissionBins(data.id);
        this.loadMissionRoute(data.id);
        this.loadMissionAlerts(data.id);
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
    this.errorMessage.set(null);

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
    this.errorMessage.set(null);

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
    if (!mission || bin.collected || bin.assignmentStatus === 'REASSIGNED') return;

    this.actionLoading.set(true);
    this.errorMessage.set(null);

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

  generateAiMissions(): void {
    if (this.generatingAi()) return;

    this.errorMessage.set(null);
    this.aiMessage.set(null);
    this.generatingAi.set(true);
    this.showOnlyLatestAi.set(false);

    this.missionService.planAndSaveMissions().subscribe({
      next: (createdMissions) => {
        const newMissions = createdMissions || [];
        const newIds = newMissions.map(m => m.id);

        this.latestAiMissionIds.set(newIds);
        this.latestAiBatchLabel.set(
          `IA-${new Date().toISOString().slice(0, 19).replace(/[-:T]/g, '')}`
        );
        this.showOnlyLatestAi.set(newIds.length > 0);

        this.generatingAi.set(false);
        this.aiMessage.set(
          newIds.length
            ? `${newIds.length} mission(s) générée(s) avec succès par l’optimisation.`
            : 'Optimisation terminée, mais aucune mission n’a été créée.'
        );

        this.loadMissions();

        if (newMissions.length) {
          this.selectMission(newMissions[0]);
        }
      },
      error: (err) => {
        this.generatingAi.set(false);

        const backendMessage =
          err?.error?.message ||
          err?.error?.error ||
          (typeof err?.error === 'string' ? err.error : null);

        this.errorMessage.set(
          backendMessage || 'Impossible de générer un nouveau plan de missions.'
        );
      }
    });
  }

  toggleLatestAiFilter(): void {
    this.showOnlyLatestAi.set(!this.showOnlyLatestAi());
  }

  clearLatestAiBatch(): void {
    this.latestAiBatchLabel.set(null);
    this.latestAiMissionIds.set([]);
    this.showOnlyLatestAi.set(false);
  }

  isInLatestAiBatch(missionId: number): boolean {
    return this.latestAiMissionIds().includes(missionId);
  }

  isNewestMission(missionId: number): boolean {
    return this.newestMissionId() === missionId;
  }

  clearAiMessage(): void {
    this.aiMessage.set(null);
  }

  getMissionStatusDetail(mission: MissionResponse | null | undefined): string | null {
    if (!mission) return null;

    const detail =
      (mission as any).missionStatusDetail ||
      (mission as any).mission_status_detail ||
      null;

    if (detail) return detail;

    switch (mission.status) {
      case 'CREATED':
        return 'PLANNED';
      case 'IN_PROGRESS':
        return 'IN_PROGRESS';
      case 'COMPLETED':
        return 'COMPLETED';
      case 'CANCELLED':
        return 'CANCELLED';
      default:
        return null;
    }
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

  getStatusDetailLabel(detail: string | null | undefined): string {
    switch (detail) {
      case 'PLANNED':
        return 'Planifiée';
      case 'IN_PROGRESS':
        return 'En exécution';
      case 'COMPLETED':
        return 'Clôturée';
      case 'REPLANNED':
        return 'Replanifiée';
      case 'PARTIALLY_REASSIGNED':
        return 'Partiellement réaffectée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return '—';
    }
  }

  statusDetailClass(detail: string | null | undefined): string {
    switch (detail) {
      case 'PARTIALLY_REASSIGNED':
        return 'mission-detail-badge reassign';
      case 'REPLANNED':
        return 'mission-detail-badge replanned';
      case 'COMPLETED':
        return 'mission-detail-badge completed';
      case 'IN_PROGRESS':
        return 'mission-detail-badge running';
      case 'PLANNED':
        return 'mission-detail-badge planned';
      case 'CANCELLED':
        return 'mission-detail-badge cancelled';
      default:
        return 'mission-detail-badge neutral';
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
    if (bin.assignmentStatus === 'REASSIGNED') return 'badge warning';
    if (bin.collected) return 'badge success';
    return 'badge neutral';
  }

  openDebugMode(): void {
    this.debugMode.set(true);
  }

  closeDebugMode(): void {
    this.debugMode.set(false);
  }

  getTotalDistance(): number {
    if (this.routeTotalDistanceKm() > 0) {
      return this.routeTotalDistanceKm();
    }

    return this.routeDistances().reduce((sum, d) => sum + d, 0);
  }


  loadMissionReassignments(missionId: number): void {
  this.loadingReassignments.set(true);

  this.missionService.getMissionReassignments(missionId).subscribe({
    next: (data) => {
      this.missionReassignments.set(data || []);
      this.loadingReassignments.set(false);
    },
    error: (err) => {
      console.error('Mission reassignments error:', err);
      this.missionReassignments.set([]);
      this.loadingReassignments.set(false);
    }
  });
}
  loadMissionAlerts(missionId: number): void {
    this.loadingMissionAlerts.set(true);

    this.alertService.getAlertsByMission(missionId).subscribe({
      next: (alerts: AlertDto[]) => {
        this.missionAlerts.set((alerts || []).filter(a => !a.resolved));
        this.loadingMissionAlerts.set(false);
      },
      error: (err: any) => {
        console.error('Mission alerts error:', err);
        this.missionAlerts.set([]);
        this.loadingMissionAlerts.set(false);
      }
    });
  }

  resolveMissionAlert(alert: AlertDto): void {
    this.alertService.resolveAlert(alert.id).subscribe({
      next: () => {
        this.missionAlerts.update(list => list.filter(a => a.id !== alert.id));
      },
      error: (err: any) => {
        console.error('Resolve mission alert error:', err);
      }
    });
  }

  getAlertSeverityLabel(severity?: string): string {
    switch ((severity || '').toUpperCase()) {
      case 'CRITICAL':
        return 'Critique';
      case 'HIGH':
        return 'Élevée';
      case 'MEDIUM':
        return 'Moyenne';
      case 'LOW':
        return 'Faible';
      default:
        return severity || '—';
    }
  }

  alertSeverityClass(severity?: string): string {
    switch ((severity || '').toUpperCase()) {
      case 'CRITICAL':
        return 'mission-alert critical';
      case 'HIGH':
        return 'mission-alert high';
      case 'MEDIUM':
        return 'mission-alert medium';
      case 'LOW':
        return 'mission-alert low';
      default:
        return 'mission-alert medium';
    }
  }

  trafficBadgeClass(): string {
    if (this.roadClosed()) return 'traffic-danger';
    if (this.trafficDelayMin() >= 30) return 'traffic-danger';
    if (this.trafficDelayMin() >= 15) return 'traffic-warning-strong';
    if (this.trafficDelayMin() >= 5) return 'traffic-warning';
    if (this.trafficEnabled()) return 'traffic-success';
    return 'traffic-neutral';
  }

  trafficLabel(): string {
    const delay = this.trafficDelayMin();

    if (this.roadClosed()) return 'Route potentiellement fermée';
    if (delay >= 30) return `Route congestionnée · +${delay} min trafic`;
    if (delay >= 15) return `Trafic important · +${delay} min`;
    if (delay >= 5) return `Trafic modéré · +${delay} min`;
    if (this.trafficEnabled()) return 'Trafic réel activé';

    return 'OSRM standard';
  }



  getWasteTypeLabel(type: string | null | undefined): string {
    switch ((type || '').toUpperCase()) {
      case 'GREEN':
        return 'Déchets verts';
      case 'YELLOW':
        return 'Emballages';
      case 'GRAY':
      case 'GREY':
        return 'Ordures ménagères';
      case 'WHITE':
        return 'Papier';
      default:
        return type || '—';
    }
  }

  wasteTypeClass(type: string | null | undefined): string {
    switch ((type || '').toUpperCase()) {
      case 'GREEN':
        return 'waste-pill waste-green';
      case 'YELLOW':
        return 'waste-pill waste-yellow';
      case 'GRAY':
      case 'GREY':
        return 'waste-pill waste-gray';
      case 'WHITE':
        return 'waste-pill waste-white';
      default:
        return 'waste-pill waste-default';
    }
  }






isMissionReplanned(mission: MissionResponse | null | undefined): boolean {
  if (!mission) return false;

  const detail = this.getMissionStatusDetail(mission);

  return (
    detail === 'REPLANNED' ||
    detail === 'PARTIALLY_REASSIGNED' ||
    this.reassignedBins() > 0 ||
    this.missionReassignments().length > 0 ||
    (mission.notes || '').toLowerCase().includes('replanifiee')
  );
}

getReplanJournalTitle(mission: MissionResponse | null | undefined): string {
  const detail = this.getMissionStatusDetail(mission);

  if (detail === 'PARTIALLY_REASSIGNED') {
    return 'Replanification dynamique avec réaffectation partielle';
  }

  if (detail === 'REPLANNED') {
    return 'Mission replanifiée automatiquement';
  }

  return 'Replanification automatique effectuée';
}

getReplanJournalMessage(mission: MissionResponse | null | undefined): string {
  const reassigned = this.reassignedBins();

  if (reassigned > 0) {
    return `${reassigned} bac(s) restant(s) ont été réaffectés vers un camion disponible après incident.`;
  }

  if (mission?.notes) {
    return mission.notes;
  }

  return 'Le système a détecté un incident opérationnel et recalculé la tournée automatiquement.';
}

journalItemClass(item: MissionJournalItem): string {
  return `smart-journal-item smart-journal-item--${item.kind}`;
}

formatJournalTime(item: MissionJournalItem): string {
  if (!item.time) {
    return item.kind === 'replan' ? 'Temps réel' : '—';
  }

  return this.formatDate(item.time);
}
}