import {
  AfterViewInit,
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import * as L from 'leaflet';
import 'leaflet.heat';
import { Subscription } from 'rxjs';
import { RealtimeService, TruckLocationMsg } from '../../../../../services/realtime.service';
import { BinService } from '../../../../../services/bin.service';
import { MapFocusService } from '../../../../../services/map-focus.service';

declare module 'leaflet' {
  function heatLayer(
    latlngs: Array<[number, number, number?]>,
    options?: any
  ): any;
}

type LatLng = { lat: number; lng: number };
type MapDisplayMode = 'markers' | 'heatmap' | 'both';
type HeatMetricMode = 'density' | 'fillLevel';

export interface FleetMapReportItem {
  id: number;
  code: string;
  status: 'Pending' | 'Validated' | 'Assigned';
  priority: 'High' | 'Medium' | 'Low';
  description: string;
  location: string;
  lat: number;
  lng: number;
  assignedTo?: string;
}

export interface FleetMapMissionBinItem {
  id: number;
  binId: number;
  binCode?: string | null;
  lat: number;
  lng: number;
  visitOrder: number;
  collected: boolean;
  targetFillThreshold?: number | null;
  wasteType?: string | null;
  decisionReason?: string | null;
  scoreExplanation?: string | null;
  urgencyExplanation?: string | null;
  feedbackExplanation?: string | null;
  postponementExplanation?: string | null;
  classificationExplanation?: string | null;
  fillLevel?: number | null;
  batteryLevel?: number | null;
  status?: string | null;
  zoneName?: string | null;
  clusterId?: number | null;
}

export interface FleetMapRouteCoordinate {
  lat: number;
  lng: number;
}

export interface FleetMapRouteStop {
  stopOrder: number;
  stopType: string | null;
  binId: number | null;
  fuelStationId?: number | null;
  fuelStationName?: string | null;
  lat: number;
  lng: number;
}

export interface FleetMapPlanningStop {
  binId: number;
  orderIndex: number;
}

export interface FleetMapPlanningMission {
  truckId: number;
  totalDistanceKm: number;
  totalDurationMinutes: number;
  stops: FleetMapPlanningStop[];
  routeCoordinates: FleetMapRouteCoordinate[];
}

export interface FleetMapDroppedBin {
  id: number;
  lat: number;
  lng: number;
  binCode?: string | null;
  wasteType?: string | null;
  clusterId?: number | null;
}

export interface FleetMapSelectedBinExplainability {
  decisionReason?: string | null;
  scoreExplanation?: string | null;
  urgencyExplanation?: string | null;
  feedbackExplanation?: string | null;
  postponementExplanation?: string | null;
  classificationExplanation?: string | null;
}

export interface FleetMapSelectedBinDetails extends FleetMapSelectedBinExplainability {
  id?: number | string | null;
  binId?: number | null;
  binCode?: string | null;
  lat: number;
  lng: number;
  wasteType?: string | null;
  clusterId?: number | null;
  fillLevel?: number | null;
  batteryLevel?: number | null;
  status?: string | null;
  zoneName?: string | null;
  collected?: boolean | null;
  targetFillThreshold?: number | null;
  visitOrder?: number | null;
  source?: 'bin' | 'mission' | 'planning' | 'dropped' | 'focus';
}
export interface FleetMapInitialTruck {
  id: string;
  truckCode: string;
  lat: number;
  lng: number;
  label?: string;
  progress?: number;
  fuelLevel?: number;
  etaMinutes?: number;
  status?: string;
  speedKmh?: number;
  headingDeg?: number;
}
export interface FleetMapTruckRoute {
  truckId: string;
  truckCode?: string;
  missionId: number;
  routeCoordinates: FleetMapRouteCoordinate[];
  collectionRouteCoordinates?: FleetMapRouteCoordinate[];
  transferRouteCoordinates?: FleetMapRouteCoordinate[];
}

@Component({
  selector: 'app-fleet-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fleet-map.component.html',
  styleUrls: ['./fleet-map.component.css']
  
})
export class FleetMapComponent implements AfterViewInit, OnDestroy, OnChanges {
  @Input() showTrucks = true;
  @Input() showBins = true;
  @Input() showReports = false;
  @Input() allowAddBins = false;
  @Input() showHeader = true;
  @Input() minimalMode = false;
  @Input() title = 'Carte de la flotte en direct';
  @Input() subtitle = 'Localisation et itinéraires des camions en temps réel';
  @Input() reports: FleetMapReportItem[] = [];
 
  @Input() missionBins: FleetMapMissionBinItem[] = [];
  @Input() missionRouteCoordinates: FleetMapRouteCoordinate[] = [];
  @Input() collectionRouteCoordinates: FleetMapRouteCoordinate[] = [];
  @Input() transferRouteCoordinates: FleetMapRouteCoordinate[] = [];
  @Input() missionRouteStops: FleetMapRouteStop[] = [];
  @Input() snappedWaypoints: FleetMapRouteCoordinate[] = [];
  @Input() matrixSource: string | null = null;
  @Input() geometrySource: string | null = 'OSRM';

  @Input() planningMode = false;
  @Input() planningMissions: FleetMapPlanningMission[] = [];
  @Input() droppedBins: FleetMapDroppedBin[] = [];
 
  @Input() initialTrucks: FleetMapInitialTruck[] = [];
  @Input() truckRoutes: FleetMapTruckRoute[] = [];
  private map?: L.Map;
  private trucksSub?: Subscription;
  private binsSub?: Subscription;

  private parisPolygon?: L.LatLng[];
  private pendingAddMarker?: L.Marker;
  private temporaryFocusMarker?: L.Marker;

  private truckMarkers = new Map<string, L.Marker>();
  private truckTrails = new Map<string, L.Polyline>();
  private truckTrailPoints = new Map<string, L.LatLngExpression[]>();
  private truckLastPositions = new Map<string, LatLng>();
  private truckAnimationFrames = new Map<string, number>();
  private followedTruckId: string | null = null;
  private autoFollowEnabled = true;
  private truckRoutePolylines = new Map<string, L.Polyline>();
  private binMarkers = new Map<number | string, L.Layer>();
  private reportMarkers = new Map<number, L.Marker>();

  private missionMarkers = new Map<number, L.Layer>();
  private truckCompletedRoutePolylines = new Map<string, L.Polyline>();
  private truckRemainingRoutePolylines = new Map<string, L.Polyline>();
  private truckRouteCoords = new Map<string, FleetMapRouteCoordinate[]>();
  private missionPolyline?: L.Polyline;
  private missionRealRoutePolyline?: L.Polyline;
  private missionCollectionPolyline?: L.Polyline;
  private missionTransferPolyline?: L.Polyline;
  private missionRouteStopMarkers = new Map<number, L.Layer>();
  private snappedWaypointMarkers = new Map<number, L.Layer>();
  private snappedConnectorLines = new Map<number, L.Polyline>();

  private planningMissionPolylines = new Map<number, L.Polyline>();
  private planningMissionMarkers = new Map<string, L.Layer>();
  private droppedBinMarkers = new Map<number, L.Layer>();

  private heatLayer?: any;
  private binsData: any[] = [];

  addBinMode = false;

  showAddBinModal = false;
  creatingBin = false;
  addBinError = '';
  addBinSuccess = '';

  selectedBin: FleetMapSelectedBinDetails | null = null;
  displayMode: MapDisplayMode = 'both';
  heatMetric: HeatMetricMode = 'fillLevel';

  newBin = {
    binCode: '',
    type: 'SIM',
    wasteType: 'GRAY',
    lat: null as number | null,
    lng: null as number | null,
    notes: '',
    installationDate: '',
    isActive: true
  };

  private readonly truckIconUrl = 'icons/truck.png';

  constructor(
    private realtime: RealtimeService,
    private binService: BinService,
    private mapFocusService: MapFocusService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initMap();

      this.loadParisBoundary()
        .then(() => {
          this.fitToDataArea();
          this.lockMapToParis();
        })
        .catch((e) => console.error('GeoJSON error:', e))
        .finally(() => {
          if (this.showBins) {
            this.loadBins();
          }

          if (this.showReports) {
            this.loadReportsOnMap();
          }

          if (this.showTrucks) {
            this.renderInitialTrucks();
            this.startRealtime();
          }

          if (this.allowAddBins) {
            this.enableAddBinClick();
            this.activateAddModeFromQuery();
          }

          this.renderMissionBins();
          this.renderTruckRoutes();

          setTimeout(() => {
            this.applyPendingFocus();
            this.map?.invalidateSize();
            this.fitToDataArea();
          }, 300);
        });

      setTimeout(() => {
        this.map?.invalidateSize();
      }, 300);
    }, 0);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialTrucks'] && this.map) {
  this.renderInitialTrucks();
    }
    if (changes['truckRoutes'] && this.map) {
  this.renderTruckRoutes();
}
    if (changes['reports'] && this.map && this.showReports) {
      this.loadReportsOnMap();

      setTimeout(() => {
        this.applyPendingFocus();
        this.fitToDataArea();
      }, 100);
    }

    if (changes['showBins'] && this.map) {
      this.refreshBinVisualisation();
      this.fitToDataArea();
    }

    if (
      (
        changes['missionBins'] ||
        changes['missionRouteCoordinates'] ||
        changes['collectionRouteCoordinates'] ||
        changes['transferRouteCoordinates'] ||
        changes['missionRouteStops'] ||
        changes['snappedWaypoints'] ||
        changes['planningMissions'] ||
        changes['droppedBins'] ||
        changes['planningMode']
      ) && this.map
    ) {
      this.renderMissionBins();
      this.fitToDataArea();
    }
  }

  toggleAddBinMode(): void {
    if (!this.allowAddBins) return;

    this.addBinMode = !this.addBinMode;

    if (!this.addBinMode) {
      this.closeAddBinModal();
    }
  }

  setDisplayMode(mode: MapDisplayMode): void {
    this.displayMode = mode;
    this.refreshBinVisualisation();
    this.fitToDataArea();
  }

  setHeatMetric(metric: HeatMetricMode): void {
    this.heatMetric = metric;
    this.renderHeatLayer();
    this.fitToDataArea();
  }

  closeAddBinModal(): void {
    this.showAddBinModal = false;
    this.creatingBin = false;
    this.addBinError = '';
    this.addBinSuccess = '';

    this.newBin = {
      binCode: '',
      type: 'SIM',
      wasteType: 'GRAY',
      lat: null,
      lng: null,
      notes: '',
      installationDate: '',
      isActive: true
    };

    if (this.pendingAddMarker && this.map) {
      this.pendingAddMarker.removeFrom(this.map);
      this.pendingAddMarker = undefined;
    }
  }

  submitAddBin(): void {
    this.addBinError = '';
    this.addBinSuccess = '';

    if (this.newBin.lat == null || this.newBin.lng == null) {
      this.addBinError = 'Veuillez choisir un emplacement sur la carte.';
      return;
    }

    if (!this.newBin.wasteType) {
      this.addBinError = 'Veuillez choisir un type de déchet.';
      return;
    }

    const payload: any = {
      type: this.newBin.type,
      wasteType: this.newBin.wasteType,
      lat: this.newBin.lat,
      lng: this.newBin.lng,
      notes: this.newBin.notes?.trim() || null,
      isActive: this.newBin.isActive
    };

    if (this.newBin.binCode?.trim()) {
      payload.binCode = this.newBin.binCode.trim();
    }

    if (this.newBin.installationDate) {
      payload.installationDate = this.newBin.installationDate;
    }

    this.creatingBin = true;

    this.binService.createBin(payload).subscribe({
      next: (created: any) => {
        this.creatingBin = false;
        this.addBinSuccess = `Poubelle ajoutée avec succès : ${created?.binCode || 'BIN'}${created?.zoneName ? ' | Zone : ' + created.zoneName : ''}`;

        this.loadBins();

        setTimeout(() => {
          this.closeAddBinModal();
          this.addBinMode = false;
        }, 1200);
      },
      error: (err) => {
        console.error(err);
        this.creatingBin = false;
        this.addBinError =
          err?.error?.message ||
          err?.error?.error ||
          'Erreur lors de la création de la poubelle.';
      }
    });
  }

  private activateAddModeFromQuery(): void {
    const pickBinLocation = this.route.snapshot.queryParamMap.get('pickBinLocation');

    if (pickBinLocation === '1' && this.allowAddBins) {
      this.addBinMode = true;

      setTimeout(() => {
        alert('Cliquez sur la carte pour choisir l’emplacement de la poubelle.');
      }, 300);

      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { pickBinLocation: null },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    }
  }

  private initMap(): void {
    const container = document.getElementById('fleetMap');

    if (!container) {
      console.error('Map container #fleetMap not found');
      return;
    }

    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }

    this.map = L.map('fleetMap', {
      center: [48.840, 2.300],
      zoom: 13,
      minZoom: 12,
      maxZoom: 19
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);
  }

  private async loadParisBoundary(): Promise<void> {
    const res = await fetch('geo/paris-15.geojson');
    if (!res.ok) {
      throw new Error(`GeoJSON fetch failed ${res.status}`);
    }

    const geojson = await res.json();

    if (!this.map) return;

    L.geoJSON(geojson, {
      style: () => ({
        color: '#0a8f3c',
        weight: 3,
        fillOpacity: 0.04
      })
    }).addTo(this.map);

    this.parisPolygon = this.extractPolygonLatLngs(geojson);
  }

  private extractPolygonLatLngs(geojson: any): L.LatLng[] | undefined {
    const geom =
      geojson?.type === 'FeatureCollection'
        ? geojson?.features?.[0]?.geometry
        : geojson?.geometry;

    if (!geom) return;

    if (geom.type === 'Polygon') {
      const outer = geom.coordinates?.[0];
      return outer?.map((p: number[]) => L.latLng(p[1], p[0]));
    }

    if (geom.type === 'MultiPolygon') {
      const outer = geom.coordinates?.[0]?.[0];
      return outer?.map((p: number[]) => L.latLng(p[1], p[0]));
    }

    return;
  }

  private fitToParis(): void {
    if (!this.map || !this.parisPolygon?.length) return;
    const bounds = L.latLngBounds(this.parisPolygon);
    this.map.fitBounds(bounds, { padding: [20, 20] });
  }

  private fitToDataArea(): void {
    if (!this.map) return;

    const boundsPoints: L.LatLngExpression[] = [];

    if (this.parisPolygon?.length) {
      this.parisPolygon.forEach((p) => boundsPoints.push([p.lat, p.lng]));
    }

    (this.binsData || []).forEach((bin) => {
      if (bin?.lat != null && bin?.lng != null && this.isInsideParis(bin.lat, bin.lng)) {
        boundsPoints.push([bin.lat, bin.lng]);
      }
    });

    (this.missionBins || []).forEach((bin) => {
      if (bin?.lat != null && bin?.lng != null && this.isInsideParis(bin.lat, bin.lng)) {
        boundsPoints.push([bin.lat, bin.lng]);
      }
    });

    (this.reports || []).forEach((report) => {
      if (report?.lat != null && report?.lng != null && this.isInsideParis(report.lat, report.lng)) {
        boundsPoints.push([report.lat, report.lng]);
      }
    });

    (this.planningMissions || []).forEach((mission) => {
      (mission.routeCoordinates || []).forEach((p) => {
        if (p?.lat != null && p?.lng != null && this.isInsideParis(p.lat, p.lng)) {
          boundsPoints.push([p.lat, p.lng]);
        }
      });

      (mission.stops || []).forEach((stop) => {
        const bin = this.findMissionBinByBinId(stop.binId);
        if (bin?.lat != null && bin?.lng != null && this.isInsideParis(bin.lat, bin.lng)) {
          boundsPoints.push([bin.lat, bin.lng]);
        }
      });
    });

    (this.droppedBins || []).forEach((bin) => {
      if (bin?.lat != null && bin?.lng != null && this.isInsideParis(bin.lat, bin.lng)) {
        boundsPoints.push([bin.lat, bin.lng]);
      }
    });

    (this.missionRouteCoordinates || []).forEach((p) => {
      if (p?.lat != null && p?.lng != null) {
        boundsPoints.push([p.lat, p.lng]);
      }
    });

    (this.collectionRouteCoordinates || []).forEach((p) => {
      if (p?.lat != null && p?.lng != null) {
        boundsPoints.push([p.lat, p.lng]);
      }
    });

    (this.transferRouteCoordinates || []).forEach((p) => {
      if (p?.lat != null && p?.lng != null) {
        boundsPoints.push([p.lat, p.lng]);
      }
    });

    (this.missionRouteStops || []).forEach((s) => {
      if (s?.lat != null && s?.lng != null) {
        boundsPoints.push([s.lat, s.lng]);
      }
    });

    (this.snappedWaypoints || []).forEach((p) => {
      if (p?.lat != null && p?.lng != null) {
        boundsPoints.push([p.lat, p.lng]);
      }
    });

    if (!boundsPoints.length) {
      this.fitToParis();
      return;
    }

    const bounds = L.latLngBounds(boundsPoints);
    this.map.fitBounds(bounds.pad(0.08), { padding: [20, 20] });
  }

  private lockMapToParis(): void {
    if (!this.map || !this.parisPolygon?.length) return;

    const bounds = L.latLngBounds(this.parisPolygon);
    this.map.setMaxBounds(bounds.pad(0.02));

    const fittedZoom = this.map.getZoom();
    this.map.setMinZoom(fittedZoom);

    this.map.on('drag', () => {
      this.map?.panInsideBounds(bounds, { animate: false });
    });
  }

  private enableAddBinClick(): void {
    if (!this.map) return;

    this.map.on('click', (e: any) => {
      if (!this.addBinMode) return;

      const lat = e.latlng.lat;
      const lng = e.latlng.lng;

      if (!this.isInsideParis(lat, lng)) {
        alert('Choisis un point à l’intérieur de Paris');
        return;
      }

      this.openAddBinModal(lat, lng);
    });
  }

  private openAddBinModal(lat: number, lng: number): void {
    this.addBinError = '';
    this.addBinSuccess = '';

    if (this.map) {
      if (this.pendingAddMarker) {
        this.pendingAddMarker.removeFrom(this.map);
      }

      this.pendingAddMarker = L.marker([lat, lng], {
        zIndexOffset: 3000
      }).addTo(this.map);

      this.pendingAddMarker.bindPopup(`
        <div style="font-weight:600;">Nouvelle poubelle</div>
        <div>Lat: ${lat.toFixed(6)}</div>
        <div>Lng: ${lng.toFixed(6)}</div>
      `).openPopup();
    }

    const returnToBins = this.route.snapshot.queryParamMap.get('returnToBins');

    if (returnToBins === '1') {
      this.router.navigate(['/municipality/bins'], {
        queryParams: {
          pickedLat: lat,
          pickedLng: lng,
          returnToBins: 1
        }
      });
      return;
    }

    this.showAddBinModal = true;

    this.newBin = {
      binCode: '',
      type: 'SIM',
      wasteType: 'GRAY',
      lat,
      lng,
      notes: '',
      installationDate: '',
      isActive: true
    };
  }

  private loadBins(): void {
    this.binsSub?.unsubscribe();

    this.binsSub = this.binService.getBins().subscribe({
      next: (bins: any[]) => {
        if (!this.map) return;

        this.binsData = (bins || []).filter(bin =>
          bin?.lat != null &&
          bin?.lng != null &&
          this.isInsideParis(bin.lat, bin.lng)
        );

        this.refreshBinVisualisation();
        this.renderMissionBins();
        this.fitToDataArea();

        setTimeout(() => {
          this.applyPendingFocus();
        }, 150);
      },
      error: (err) => {
        console.error('Error bins', err);
      }
    });
  }

  private refreshBinVisualisation(): void {
    if (!this.map) return;

    this.clearBinMarkers();
    this.clearHeatLayer();

    if (!this.showBins) {
      return;
    }

    if (this.displayMode === 'markers' || this.displayMode === 'both') {
      this.renderBinMarkersFromData();
    }

    if (this.displayMode === 'heatmap' || this.displayMode === 'both') {
      this.renderHeatLayer();
    }
  }

  private renderBinMarkersFromData(): void {
    if (!this.map) return;

    const map = this.map;
    this.clearBinMarkers();

    (this.binsData || []).forEach((bin) => {
      if (bin.lat == null || bin.lng == null) return;
      if (!this.isInsideParis(bin.lat, bin.lng)) return;

      const typeColor = this.getBinColor(bin);
      const visual = this.getFillVisual(bin);

      const marker = L.circleMarker([bin.lat, bin.lng], {
        radius: visual.radius,
        color: visual.borderColor,
        fillColor: typeColor,
        fillOpacity: visual.fillOpacity,
        weight: visual.borderWeight,
        className: visual.className
      });

      marker.addTo(map).bindPopup(this.buildBinPopup(bin));

      marker.on('click', () => {
        this.selectBinDetails({
          id: bin.binId ?? bin.id ?? null,
          binId: bin.binId ?? bin.id ?? null,
          binCode: bin.binCode ?? null,
          lat: bin.lat,
          lng: bin.lng,
          wasteType: bin.wasteType ?? null,
          clusterId: bin.clusterId ?? null,
          fillLevel: bin.fillLevel ?? null,
          batteryLevel: bin.batteryLevel ?? null,
          status: bin.status ?? null,
          zoneName: bin.zoneName ?? null,
          decisionReason: bin.decisionReason ?? null,
          scoreExplanation: bin.scoreExplanation ?? null,
          urgencyExplanation: bin.urgencyExplanation ?? null,
          feedbackExplanation: bin.feedbackExplanation ?? null,
          postponementExplanation: bin.postponementExplanation ?? null,
          classificationExplanation: bin.classificationExplanation ?? null,
          source: 'bin'
        });
      });

      this.binMarkers.set(bin.binId ?? bin.id ?? bin.binCode, marker);
    });
  }

  private buildHeatPoints(): [number, number, number][] {
    const bins = (this.binsData || [])
      .filter(bin =>
        bin?.lat != null &&
        bin?.lng != null &&
        this.isInsideParis(bin.lat, bin.lng)
      );

    if (this.heatMetric === 'fillLevel') {
      return bins.map(bin => {
        const fill = typeof bin.fillLevel === 'number' ? bin.fillLevel : 0;
        const intensity = Math.max(0.05, Math.min(1, fill / 100));
        return [bin.lat, bin.lng, intensity];
      });
    }

    return bins.map(bin => [bin.lat, bin.lng, 0.6]);
  }

  private renderHeatLayer(): void {
    if (!this.map) return;

    if (this.heatLayer) {
      this.heatLayer.removeFrom(this.map);
      this.heatLayer = undefined;
    }

    if (!this.showBins) return;
    if (this.displayMode !== 'heatmap' && this.displayMode !== 'both') return;
    if (!this.binsData.length) return;

    const points = this.buildHeatPoints();
    if (!points.length) return;

    this.heatLayer = (L as any).heatLayer(points, {
      radius: 28,
      blur: 22,
      maxZoom: 18,
      minOpacity: 0.3,
      gradient: {
        0.15: '#3b82f6',
        0.35: '#22c55e',
        0.55: '#facc15',
        0.75: '#f97316',
        1.0: '#dc2626'
      }
    });

    this.heatLayer.addTo(this.map);
  }

  private clearHeatLayer(): void {
    if (!this.map || !this.heatLayer) return;
    this.heatLayer.removeFrom(this.map);
    this.heatLayer = undefined;
  }

  private renderMissionBins(): void {
    if (!this.map) return;

    if (this.planningMode) {
      this.renderPlanningRoutes();
      return;
    }

    this.clearMissionMarkers();
    this.selectedBin = null;

    const bins = (this.missionBins || [])
      .filter(b => b.lat != null && b.lng != null)
      .sort((a, b) => a.visitOrder - b.visitOrder);

    const routeCoords = (this.missionRouteCoordinates || [])
      .filter(p => p.lat != null && p.lng != null);

    const collectionCoords = (this.collectionRouteCoordinates || [])
      .filter(p => p.lat != null && p.lng != null);

    const transferCoords = (this.transferRouteCoordinates || [])
      .filter(p => p.lat != null && p.lng != null);

    const routeStops = (this.missionRouteStops || [])
      .filter(s => s.lat != null && s.lng != null);

    const snapped = (this.snappedWaypoints || [])
      .filter(p => p.lat != null && p.lng != null);

    if (!bins.length && !routeCoords.length && !routeStops.length && !snapped.length && !collectionCoords.length && !transferCoords.length) {
      this.fitToDataArea();
      return;
    }

    const boundsPoints: L.LatLngExpression[] = [];

    bins.forEach((bin) => {
      const color = this.getWasteTypeColor(bin.wasteType);
      const borderColor = bin.collected ? '#16a34a' : color;

      const marker = L.circleMarker([bin.lat, bin.lng], {
        radius: 10,
        color: borderColor,
        fillColor: color,
        fillOpacity: 0.95,
        weight: 3
      });

      marker.addTo(this.map!).bindPopup(this.buildMissionBinPopup(bin));

      marker.on('click', () => {
        this.selectBinDetails({
          id: bin.id,
          binId: bin.binId,
          binCode: bin.binCode ?? null,
          lat: bin.lat,
          lng: bin.lng,
          wasteType: bin.wasteType ?? null,
          clusterId: bin.clusterId ?? null,
          fillLevel: bin.fillLevel ?? null,
          batteryLevel: bin.batteryLevel ?? null,
          collected: bin.collected,
          targetFillThreshold: bin.targetFillThreshold ?? null,
          visitOrder: bin.visitOrder,
          status: bin.status ?? (bin.collected ? 'COLLECTED' : 'TO_COLLECT'),
          zoneName: bin.zoneName ?? null,
          decisionReason: bin.decisionReason ?? null,
          scoreExplanation: bin.scoreExplanation ?? null,
          urgencyExplanation: bin.urgencyExplanation ?? null,
          feedbackExplanation: bin.feedbackExplanation ?? null,
          postponementExplanation: bin.postponementExplanation ?? null,
          classificationExplanation: bin.classificationExplanation ?? null,
          source: 'mission'
        });
      });

      const orderLabel = L.marker([bin.lat, bin.lng], {
        icon: L.divIcon({
          className: 'mission-order-icon',
          html: `<div class="mission-order-badge">${bin.visitOrder}</div>`,
          iconSize: [24, 24],
          iconAnchor: [12, 12]
        }),
        zIndexOffset: 2500
      }).addTo(this.map!);

      this.missionMarkers.set(bin.id, marker);
      this.missionMarkers.set(bin.id * 100000, orderLabel);

      boundsPoints.push([bin.lat, bin.lng]);
    });

    routeStops.forEach((stop, index) => {
      let color = '#111827';
      let fillColor = '#ffffff';
      let radius = 6;
      let popupTitle = 'Route stop';
      let extraHtml = '';

      if (stop.stopType === 'DEPOT_START') {
        color = '#16a34a';
        fillColor = '#16a34a';
        radius = 8;
        popupTitle = 'Dépôt de départ';
      } else if (stop.stopType === 'DEPOT_RETURN') {
        color = '#dc2626';
        fillColor = '#dc2626';
        radius = 8;
        popupTitle = 'Dépôt de retour';
      } else if (stop.stopType === 'BIN_PICKUP') {
        color = '#111827';
        fillColor = '#ffffff';
        radius = 5;
        popupTitle = 'Bac à collecter';
      } else if (stop.stopType === 'FUEL_STATION') {
        color = '#f59e0b';
        fillColor = '#f59e0b';
        radius = 7;
        popupTitle = 'Station-service';
        extraHtml = `<div><b>Station:</b> ${stop.fuelStationName ?? '—'}</div>`;
      }

      const marker = L.circleMarker([stop.lat, stop.lng], {
        radius,
        color,
        fillColor,
        fillOpacity: 1,
        weight: 2
      });

      marker.addTo(this.map!).bindPopup(`
        <div style="min-width:200px">
          <div style="font-weight:700; margin-bottom:6px;">${popupTitle}</div>
          <div><b>Order:</b> ${stop.stopOrder}</div>
          <div><b>Type:</b> ${stop.stopType ?? '—'}</div>
          <div><b>Bin ID:</b> ${stop.binId ?? '—'}</div>
          ${extraHtml}
        </div>
      `);

      this.missionRouteStopMarkers.set(index, marker);
      boundsPoints.push([stop.lat, stop.lng]);
    });

    snapped.forEach((point, index) => {
      const marker = L.circleMarker([point.lat, point.lng], {
        radius: 6,
        color: '#2563eb',
        fillColor: '#ffffff',
        fillOpacity: 1,
        weight: 3
      });

      marker.addTo(this.map!).bindPopup(`
        <div style="min-width:180px">
          <div style="font-weight:700; margin-bottom:6px;">Snapped waypoint</div>
          <div><b>Index:</b> ${index + 1}</div>
          <div><b>Lat:</b> ${point.lat.toFixed(6)}</div>
          <div><b>Lng:</b> ${point.lng.toFixed(6)}</div>
        </div>
      `);

      this.snappedWaypointMarkers.set(index, marker);
      boundsPoints.push([point.lat, point.lng]);
    });

    const pickupStops = routeStops.filter(s => s.stopType === 'BIN_PICKUP');
    const snappedPickupPoints = snapped.slice(1, snapped.length - 1);

    pickupStops.forEach((stop, index) => {
      const snappedPoint = snappedPickupPoints[index];
      if (!snappedPoint) return;

      const connector = L.polyline(
        [
          [stop.lat, stop.lng],
          [snappedPoint.lat, snappedPoint.lng]
        ],
        {
          color: '#dc2626',
          weight: 2,
          opacity: 0.9,
          dashArray: '5 5'
        }
      ).addTo(this.map!);

      this.snappedConnectorLines.set(index, connector);
      boundsPoints.push([snappedPoint.lat, snappedPoint.lng]);
    });

    if (collectionCoords.length >= 2 || transferCoords.length >= 2) {
      if (transferCoords.length >= 2) {
        const transferLatLngs: L.LatLngExpression[] = transferCoords.map(p => [p.lat, p.lng]);

        this.missionTransferPolyline = L.polyline(transferLatLngs, {
          color: '#94a3b8',
          weight: 4,
          opacity: 0.9,
          dashArray: '10 8'
        }).addTo(this.map!);

        boundsPoints.push(...transferLatLngs);
      }

      if (collectionCoords.length >= 2) {
        const collectionLatLngs: L.LatLngExpression[] = collectionCoords.map(p => [p.lat, p.lng]);

        this.missionCollectionPolyline = L.polyline(collectionLatLngs, {
          color: '#2563eb',
          weight: 6,
          opacity: 0.95
        }).addTo(this.map!);

        boundsPoints.push(...collectionLatLngs);
      }
    } else if (routeCoords.length >= 2) {
      const realLatLngs: L.LatLngExpression[] = routeCoords.map(p => [p.lat, p.lng]);

      this.missionRealRoutePolyline = L.polyline(realLatLngs, {
        color: '#2563eb',
        weight: 5,
        opacity: 0.9
      }).addTo(this.map!);

      boundsPoints.push(...realLatLngs);
    } else if (bins.length >= 2) {
      const fallbackLatLngs: L.LatLngExpression[] = bins.map(bin => [bin.lat, bin.lng]);

      this.missionPolyline = L.polyline(fallbackLatLngs, {
        color: '#0f766e',
        weight: 4,
        opacity: 0.85,
        dashArray: '8 6'
      }).addTo(this.map!);

      boundsPoints.push(...fallbackLatLngs);
    }

    if (boundsPoints.length) {
      const bounds = L.latLngBounds(boundsPoints);
      this.map!.fitBounds(bounds.pad(0.08), { padding: [20, 20] });
    }
  }

  private renderPlanningRoutes(): void {
    if (!this.map) return;

    this.clearMissionMarkers();

    const boundsPoints: L.LatLngExpression[] = [];
    const colors = ['#2563eb', '#dc2626', '#16a34a', '#7c3aed', '#ea580c'];

    (this.planningMissions || []).forEach((mission, missionIndex) => {
      const color = colors[missionIndex % colors.length];

      const coords = (mission.routeCoordinates || [])
        .filter(p => p.lat != null && p.lng != null);

      if (coords.length >= 2) {
        const latLngs: L.LatLngExpression[] = coords.map(p => [p.lat, p.lng]);

        const polyline = L.polyline(latLngs, {
          color,
          weight: 5,
          opacity: 0.9
        }).addTo(this.map!);

        polyline.bindPopup(`
          <div style="min-width:220px">
            <div style="font-weight:700; margin-bottom:6px;">Truck ${mission.truckId}</div>
            <div><b>Distance:</b> ${mission.totalDistanceKm} km</div>
            <div><b>Duration:</b> ${mission.totalDurationMinutes} min</div>
            <div><b>Stops:</b> ${mission.stops?.length || 0}</div>
          </div>
        `);

        this.planningMissionPolylines.set(mission.truckId, polyline);
        boundsPoints.push(...latLngs);
      }

      (mission.stops || []).forEach((stop) => {
        const bin = this.findMissionBinByBinId(stop.binId);
        if (!bin) return;

        const fillColor = this.getWasteTypeColor(bin.wasteType);

        const marker = L.circleMarker([bin.lat, bin.lng], {
          radius: 10,
          color,
          fillColor,
          fillOpacity: 0.9,
          weight: 3
        }).addTo(this.map!);

        marker.bindPopup(`
          <div style="min-width:220px">
            <div style="font-weight:700; margin-bottom:6px;">Bin ${stop.binId}</div>
            <div><b>Truck:</b> ${mission.truckId}</div>
            <div><b>Order:</b> ${stop.orderIndex}</div>
            <div><b>Code:</b> ${bin.binCode || '—'}</div>
            <div><b>Waste Type:</b> ${bin.wasteType || '—'}</div>
          </div>
        `);

        marker.on('click', () => {
          this.selectBinDetails({
            id: bin.id,
            binId: bin.binId,
            binCode: bin.binCode ?? null,
            lat: bin.lat,
            lng: bin.lng,
            wasteType: bin.wasteType ?? null,
            clusterId: bin.clusterId ?? null,
            fillLevel: bin.fillLevel ?? null,
            batteryLevel: bin.batteryLevel ?? null,
            visitOrder: stop.orderIndex,
            status: `Truck ${mission.truckId}`,
            zoneName: bin.zoneName ?? null,
            decisionReason: bin.decisionReason ?? null,
            scoreExplanation: bin.scoreExplanation ?? null,
            urgencyExplanation: bin.urgencyExplanation ?? null,
            feedbackExplanation: bin.feedbackExplanation ?? null,
            postponementExplanation: bin.postponementExplanation ?? null,
            classificationExplanation: bin.classificationExplanation ?? null,
            source: 'planning'
          });
        });

        const label = L.marker([bin.lat, bin.lng], {
          icon: L.divIcon({
            className: 'mission-order-icon',
            html: `<div class="mission-order-badge" style="background:${color};">${stop.orderIndex}</div>`,
            iconSize: [24, 24],
            iconAnchor: [12, 12]
          }),
          zIndexOffset: 2500
        }).addTo(this.map!);

        this.planningMissionMarkers.set(`${mission.truckId}-${stop.binId}`, marker);
        this.planningMissionMarkers.set(`${mission.truckId}-${stop.binId}-label`, label);

        boundsPoints.push([bin.lat, bin.lng]);
      });
    });

    (this.droppedBins || []).forEach((bin) => {
      const marker = L.circleMarker([bin.lat, bin.lng], {
        radius: 8,
        color: '#dc2626',
        fillColor: '#dc2626',
        fillOpacity: 0.45,
        weight: 2,
        dashArray: '4 4'
      }).addTo(this.map!);

      marker.bindPopup(`
        <div style="min-width:220px">
          <div style="font-weight:700; margin-bottom:6px;">Dropped Bin ${bin.id}</div>
          <div><b>Code:</b> ${bin.binCode || '—'}</div>
          <div><b>Waste Type:</b> ${bin.wasteType || '—'}</div>
          <div><b>Cluster:</b> ${bin.clusterId ?? '—'}</div>
          <div style="color:#dc2626; margin-top:6px;"><b>Not assigned</b></div>
        </div>
      `);

      marker.on('click', () => {
        this.selectBinDetails({
          id: bin.id,
          binCode: bin.binCode ?? null,
          lat: bin.lat,
          lng: bin.lng,
          wasteType: bin.wasteType ?? null,
          clusterId: bin.clusterId ?? null,
          status: 'NOT_ASSIGNED',
          source: 'dropped'
        });
      });

      this.droppedBinMarkers.set(bin.id, marker);
      boundsPoints.push([bin.lat, bin.lng]);
    });

    if (boundsPoints.length) {
      const bounds = L.latLngBounds(boundsPoints);
      this.map.fitBounds(bounds.pad(0.08), { padding: [20, 20] });
    }
  }

  private findMissionBinByBinId(binId: number | null): FleetMapMissionBinItem | null {
    if (binId == null) return null;
    const found = (this.missionBins || []).find(b => b.binId === binId);
    return found || null;
  }

  private loadReportsOnMap(): void {
    if (!this.map) return;

    const map = this.map;
    this.clearReportMarkers();

    (this.reports || []).forEach((report) => {
      if (report.lat == null || report.lng == null) return;
      if (!this.isInsideParis(report.lat, report.lng)) return;

      const marker = L.marker([report.lat, report.lng], {
        icon: this.makeReportIcon(report),
        zIndexOffset: 2000
      });

      marker.addTo(map).bindPopup(`
        <div style="min-width:240px">
          <div style="font-weight:700; margin-bottom:8px;">${report.code}</div>
          <div><b>Statut:</b> ${this.reportStatusLabel(report.status)}</div>
          <div><b>Priorité:</b> ${this.reportPriorityLabel(report.priority)}</div>
          <div><b>Adresse:</b> ${report.location}</div>
          <div style="margin-top:8px;">${report.description}</div>
          ${report.assignedTo ? `<div style="margin-top:8px;"><b>Affecté à:</b> ${report.assignedTo}</div>` : ''}
        </div>
      `);

      this.reportMarkers.set(report.id, marker);
    });
  }

  private applyPendingFocus(): void {
    if (!this.map) return;

    const target = this.mapFocusService.getTarget();
    if (!target) return;

    if (target.type === 'report') {
      const marker = this.reportMarkers.get(target.id as number);

      if (marker) {
        this.map.setView([target.lat, target.lng], 17, { animate: true });
        setTimeout(() => {
          marker.openPopup();
        }, 250);
        this.mapFocusService.clearTarget();
        return;
      }

      if (this.isInsideParis(target.lat, target.lng)) {
        this.map.setView([target.lat, target.lng], 17, { animate: true });
        this.mapFocusService.clearTarget();
      }

      return;
    }

    if (target.type === 'bin') {
      this.map.setView([target.lat, target.lng], 17, { animate: true });

      let matchedMarker: any = null;

      for (const layer of this.binMarkers.values()) {
        const marker: any = layer;

        if (typeof marker.getLatLng === 'function') {
          const pos = marker.getLatLng();

          const samePoint =
            Math.abs(pos.lat - target.lat) < 0.0001 &&
            Math.abs(pos.lng - target.lng) < 0.0001;

          if (samePoint) {
            matchedMarker = marker;
            break;
          }
        }
      }

      this.selectBinDetails({
        id: target.id ?? null,
        binCode: target.code ?? null,
        lat: target.lat,
        lng: target.lng,
        zoneName: target.zone ?? null,
        source: 'focus'
      });

      if (matchedMarker) {
        setTimeout(() => {
          if (typeof matchedMarker.openPopup === 'function') {
            matchedMarker.openPopup();
          }
        }, 250);

        this.mapFocusService.clearTarget();
        return;
      }

      if (this.temporaryFocusMarker) {
        this.temporaryFocusMarker.removeFrom(this.map);
        this.temporaryFocusMarker = undefined;
      }

      this.temporaryFocusMarker = L.marker([target.lat, target.lng], {
        zIndexOffset: 4000
      }).addTo(this.map);

      this.temporaryFocusMarker.bindPopup(`
        <div style="min-width:220px">
          <div style="font-weight:700; margin-bottom:6px;">${target.code ?? 'Poubelle sélectionnée'}</div>
          <div><b>Zone:</b> ${target.zone ?? '—'}</div>
          <div><b>Latitude:</b> ${target.lat.toFixed(6)}</div>
          <div><b>Longitude:</b> ${target.lng.toFixed(6)}</div>
        </div>
      `);

      setTimeout(() => {
        this.temporaryFocusMarker?.openPopup();
      }, 250);

      this.mapFocusService.clearTarget();
    }
  }

  private clearBinMarkers(): void {
    if (!this.map) return;

    for (const marker of this.binMarkers.values()) {
      marker.removeFrom(this.map);
    }

    this.binMarkers.clear();
  }

  private clearReportMarkers(): void {
    if (!this.map) return;

    for (const marker of this.reportMarkers.values()) {
      marker.removeFrom(this.map);
    }

    this.reportMarkers.clear();
  }

  private clearMissionMarkers(): void {
    if (!this.map) return;

    for (const marker of this.missionMarkers.values()) {
      marker.removeFrom(this.map);
    }
    this.missionMarkers.clear();

    for (const marker of this.missionRouteStopMarkers.values()) {
      marker.removeFrom(this.map);
    }
    this.missionRouteStopMarkers.clear();

    for (const marker of this.snappedWaypointMarkers.values()) {
      marker.removeFrom(this.map);
    }
    this.snappedWaypointMarkers.clear();

    for (const line of this.snappedConnectorLines.values()) {
      line.removeFrom(this.map);
    }
    this.snappedConnectorLines.clear();

    for (const line of this.planningMissionPolylines.values()) {
      line.removeFrom(this.map);
    }
    this.planningMissionPolylines.clear();

    for (const marker of this.planningMissionMarkers.values()) {
      marker.removeFrom(this.map);
    }
    this.planningMissionMarkers.clear();

    for (const marker of this.droppedBinMarkers.values()) {
      marker.removeFrom(this.map);
    }
    this.droppedBinMarkers.clear();

    if (this.missionPolyline) {
      this.missionPolyline.removeFrom(this.map);
      this.missionPolyline = undefined;
    }

    if (this.missionRealRoutePolyline) {
      this.missionRealRoutePolyline.removeFrom(this.map);
      this.missionRealRoutePolyline = undefined;
    }

    if (this.missionCollectionPolyline) {
      this.missionCollectionPolyline.removeFrom(this.map);
      this.missionCollectionPolyline = undefined;
    }

    if (this.missionTransferPolyline) {
      this.missionTransferPolyline.removeFrom(this.map);
      this.missionTransferPolyline = undefined;
    }
  }

  private getWasteTypeColor(wasteType?: string | null): string {
    switch ((wasteType || '').toUpperCase()) {
      case 'GRAY':
        return '#475569';
      case 'GREEN':
        return '#16a34a';
      case 'YELLOW':
        return '#facc15';
      case 'WHITE':
        return '#2563eb';
      default:
        return '#64748b';
    }
  }

  private getBinColor(bin: any): string {
    return this.getWasteTypeColor(bin?.wasteType);
  }

  private getFillVisual(bin: any): {
    radius: number;
    borderColor: string;
    borderWeight: number;
    fillOpacity: number;
    className: string;
  } {
    const fill = Number(bin?.fillLevel ?? 0);

    if (fill >= 95) {
      return {
        radius: 11,
        borderColor: '#dc2626',
        borderWeight: 4,
        fillOpacity: 1,
        className: 'bin-marker-critical'
      };
    }

    if (fill >= 80) {
      return {
        radius: 10,
        borderColor: '#ef4444',
        borderWeight: 3,
        fillOpacity: 0.96,
        className: 'bin-marker-alert'
      };
    }

    if (fill >= 50) {
      return {
        radius: 9,
        borderColor: '#f59e0b',
        borderWeight: 3,
        fillOpacity: 0.93,
        className: 'bin-marker-warning'
      };
    }

    return {
      radius: 8,
      borderColor: '#ffffff',
      borderWeight: 2,
      fillOpacity: 0.9,
      className: 'bin-marker-normal'
    };
  }

  private escapeHtml(value: any): string {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  private buildBinPopup(bin: any): string {
    const code = this.escapeHtml(bin.binCode ?? `BIN-${bin.binId ?? bin.id ?? '?'}`);
    const wasteType = this.escapeHtml(bin.wasteType ?? '—');
    const fill = bin.fillLevel ?? 0;
    const battery = bin.batteryLevel ?? 0;
    const status = this.escapeHtml(bin.status ?? 'OK');
    const zone = this.escapeHtml(bin.zoneName ?? '—');
    const cluster = bin.clusterId ?? '—';

    let fillLabel = 'Faible';
    if (fill >= 95) fillLabel = 'Critique';
    else if (fill >= 80) fillLabel = 'Très élevé';
    else if (fill >= 50) fillLabel = 'Élevé';
    else if (fill >= 25) fillLabel = 'Modéré';

    return `
      <div class="bin-popup-card">
        <div class="bin-popup-title">${code}</div>
        <div><b>Zone:</b> ${zone}</div>
        <div><b>Cluster:</b> ${cluster}</div>
        <div><b>Type:</b> ${wasteType}</div>
        <div><b>Fill:</b> ${fill}% (${fillLabel})</div>
        <div><b>Battery:</b> ${battery}%</div>
        <div><b>Status:</b> ${status}</div>
      </div>
    `;
  }

  private buildMissionBinPopup(bin: FleetMapMissionBinItem): string {
    const code = this.escapeHtml(bin.binCode || `BIN-${bin.binId}`);
    const wasteType = this.escapeHtml(bin.wasteType ?? '—');
    const cluster = bin.clusterId ?? '—';

    return `
      <div class="bin-popup-card">
        <div class="bin-popup-title">${code}</div>
        <div><b>Cluster:</b> ${cluster}</div>
        <div><b>Ordre:</b> ${bin.visitOrder}</div>
        <div><b>Statut:</b> ${bin.collected ? 'Collecté' : 'À collecter'}</div>
        <div><b>Seuil:</b> ${bin.targetFillThreshold ?? '—'}%</div>
        <div><b>Type:</b> ${wasteType}</div>
      </div>
    `;
  }

  private selectBinDetails(details: FleetMapSelectedBinDetails): void {
    this.selectedBin = details;
  }

  hasExplainability(bin: FleetMapSelectedBinDetails | null): boolean {
    return !!(
      bin?.urgencyExplanation ||
      bin?.classificationExplanation ||
      bin?.scoreExplanation ||
      bin?.decisionReason ||
      bin?.feedbackExplanation ||
      bin?.postponementExplanation
    );
  }

  private makeReportIcon(report: FleetMapReportItem): L.DivIcon {
    let color = '#f59e0b';

    if (report.status === 'Assigned') {
      color = '#2563eb';
    } else if (report.status === 'Validated') {
      color = '#22c55e';
    } else if (report.priority === 'High') {
      color = '#ef4444';
    } else if (report.priority === 'Medium') {
      color = '#f59e0b';
    } else {
      color = '#fb923c';
    }

    return L.divIcon({
      className: 'report-marker-wrapper',
      iconSize: [20, 20],
      iconAnchor: [10, 10],
      html: `
        <div style="
          width: 18px;
          height: 18px;
          border-radius: 50%;
          background: ${color};
          border: 3px solid #ffffff;
          box-shadow: 0 2px 8px rgba(0,0,0,0.25);
        "></div>
      `
    });
  }

  private reportStatusLabel(status: 'Pending' | 'Validated' | 'Assigned'): string {
    if (status === 'Pending') return 'En attente';
    if (status === 'Validated') return 'Validé';
    return 'Affecté';
  }

  private reportPriorityLabel(priority: 'High' | 'Medium' | 'Low'): string {
    if (priority === 'High') return 'Élevée';
    if (priority === 'Medium') return 'Moyenne';
    return 'Faible';
  }
  private renderInitialTrucks(): void {
  if (!this.map || !this.showTrucks) return;

  const boundsPoints: L.LatLngExpression[] = [];

  (this.initialTrucks || []).forEach((truck) => {
    if (truck.lat == null || truck.lng == null) return;

    this.upsertTruckMarker(
      truck.id,
      {
        lat: Number(truck.lat),
        lng: Number(truck.lng),
      },
      truck
    );

    boundsPoints.push([Number(truck.lat), Number(truck.lng)]);
  });

  if (boundsPoints.length) {
    const bounds = L.latLngBounds(boundsPoints);
    this.map.fitBounds(bounds.pad(0.15), { padding: [30, 30] });
  }
}
private startRealtime(): void {
  this.realtime.connect({
    wsUrl: 'ws://localhost:8081/ws',
    topic: '/topic/truck-locations'
  });

  this.trucksSub = this.realtime.trucks$.subscribe((trucks) => {
    if (!this.map || !this.showTrucks) return;

    for (const [id, payload] of trucks.entries()) {
      const pos = this.extractLatLng(payload);
      if (!pos) continue;

      const existingInitial = (this.initialTrucks || []).find(t => t.id === id);

      // Auto-follow أول camion يوصل live
      if (!this.followedTruckId) {
        this.followedTruckId = id;
        this.autoFollowEnabled = true;
      }

      this.upsertTruckMarker(id, pos, {
        id,
        truckCode: existingInitial?.truckCode || payload.truckCode || `TRUCK-${id}`,
        label: existingInitial?.label,
        lat: pos.lat,
        lng: pos.lng,
        progress: existingInitial?.progress,
        fuelLevel: existingInitial?.fuelLevel,
        etaMinutes: existingInitial?.etaMinutes,
        status: existingInitial?.status || 'ON_MISSION',
        speedKmh: Number(payload.speedKmh ?? payload.speed ?? 0),
        headingDeg: Number(payload.headingDeg ?? payload.heading ?? 0),
      });
    }
  });
}
  private extractLatLng(payload: TruckLocationMsg): LatLng | null {
    const lat = (payload as any).lat ?? (payload as any).latitude;
    const lng = (payload as any).lng ?? (payload as any).longitude;

    if (typeof lat !== 'number' || typeof lng !== 'number') return null;
    return { lat, lng };
  }

  private isInsideParis(lat: number, lng: number): boolean {
    const poly = this.parisPolygon;
    if (!poly || poly.length < 3) return true;

    let inside = false;

    for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
      const xi = poly[i].lng;
      const yi = poly[i].lat;
      const xj = poly[j].lng;
      const yj = poly[j].lat;

      const intersect =
        (yi > lat) !== (yj > lat) &&
        lng < ((xj - xi) * (lat - yi)) / ((yj - yi) || 1e-12) + xi;

      if (intersect) inside = !inside;
    }

    return inside;
  }
private getTruckColor(idOrCode: string): string {
  const colors = ['#2f80ed', '#10b981', '#8b5cf6', '#f97316', '#ef4444', '#4b5563'];
  let hash = 0;

  for (let i = 0; i < idOrCode.length; i++) {
    hash = idOrCode.charCodeAt(i) + ((hash << 5) - hash);
  }

  return colors[Math.abs(hash) % colors.length];
}

private makeTruckIcon(truck?: FleetMapInitialTruck, headingDeg = 0, speedKmh = 0): L.DivIcon {
  const code = truck?.truckCode || truck?.label || truck?.id || 'TRUCK';
  const status = truck?.status || 'ON_MISSION';
  const progress = truck?.progress ?? 0;
  const color = this.getTruckColor(code);

  return L.divIcon({
    className: 'truck-modern-marker',
    iconSize: [148, 58],
    iconAnchor: [26, 29],
    popupAnchor: [0, -26],
    html: `
      <div class="truck-modern-wrap">
        <div class="truck-modern-pin" style="background:${color};">
          <span class="material-icons truck-heading">
            local_shipping
          </span>
        </div>

        <div class="truck-modern-label">
          <strong>${code}</strong>
          <span>En mission</span>
          <small>${progress}% · ${Math.round(speedKmh)} km/h</small>
        </div>
      </div>
    `
  });
}
private upsertTruckMarker(id: string, pos: LatLng, truck?: FleetMapInitialTruck): void {
  if (!this.map || !this.showTrucks) return;

  const speedKmh = truck?.speedKmh ?? 0;
  const headingDeg = truck?.headingDeg ?? 0;

  const existing = this.truckMarkers.get(id);

  let finalPos = pos;

  const route = this.truckRouteCoords.get(id);
  if (route && route.length > 1) {
    finalPos = this.snapToRoute(route, pos);
  }

  this.updateTruckRouteProgress(id, finalPos);

  if (!existing) {
    const marker = L.marker([finalPos.lat, finalPos.lng], {
      icon: this.makeTruckIcon(truck, headingDeg, speedKmh),
      zIndexOffset: 5000
    }).addTo(this.map);

    marker.bindPopup(`
      <div style="min-width:170px">
        <b>${truck?.truckCode || truck?.label || id}</b><br>
        Status: ${truck?.status || 'ON_MISSION'}<br>
        Progress: ${truck?.progress ?? 0}%<br>
        Speed: ${Math.round(speedKmh)} km/h<br>
        Heading: ${Math.round(headingDeg)}°
      </div>
    `);

    marker.on('click', () => {
      this.followedTruckId = id;
      this.autoFollowEnabled = true;
      this.map?.setView([finalPos.lat, finalPos.lng], 17, { animate: true });
    });

    this.truckMarkers.set(id, marker);
    this.truckLastPositions.set(id, finalPos);

    if (this.autoFollowEnabled && this.followedTruckId === id) {
      this.map.setView([finalPos.lat, finalPos.lng], 17, { animate: true });
    }

    return;
  }

  existing.setIcon(this.makeTruckIcon(truck, headingDeg, speedKmh));
  this.animateTruckMarker(id, existing, finalPos);

  if (this.autoFollowEnabled && this.followedTruckId === id) {
    this.map.panTo([finalPos.lat, finalPos.lng], {
      animate: true,
      duration: 0.6
    });
  }
}
private animateTruckMarker(id: string, marker: L.Marker, next: LatLng): void {
  const previous = this.truckLastPositions.get(id);

  if (!previous) {
    marker.setLatLng([next.lat, next.lng]);
    this.truckLastPositions.set(id, next);
    return;
  }

  const oldFrame = this.truckAnimationFrames.get(id);
  if (oldFrame) {
    cancelAnimationFrame(oldFrame);
  }

  const duration = 900;
  const start = performance.now();

  const step = (now: number) => {
    const t = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - t, 3);

    let lat = previous.lat + (next.lat - previous.lat) * eased;
let lng = previous.lng + (next.lng - previous.lng) * eased;

const route = this.truckRouteCoords.get(id);
if (route && route.length > 1) {
  const snapped = this.snapToRoute(route, { lat, lng });
  lat = snapped.lat;
  lng = snapped.lng;
}

    marker.setLatLng([lat, lng]);

    if (t < 1) {
      const frame = requestAnimationFrame(step);
      this.truckAnimationFrames.set(id, frame);
    } else {
      this.truckLastPositions.set(id, next);
      this.truckAnimationFrames.delete(id);
    }
  };

  const frame = requestAnimationFrame(step);
  this.truckAnimationFrames.set(id, frame);
}

private updateTruckTrail(id: string, pos: LatLng): void {
  if (!this.map) return;

  const points = this.truckTrailPoints.get(id) || [];
  points.push([pos.lat, pos.lng]);

  const maxPoints = 30;
  const limitedPoints = points.slice(-maxPoints);

  this.truckTrailPoints.set(id, limitedPoints);

  let trail = this.truckTrails.get(id);

  if (!trail) {
    trail = L.polyline(limitedPoints, {
      color: '#2563eb',
      weight: 4,
      opacity: 0.55,
      dashArray: '6 8'
    }).addTo(this.map);

    this.truckTrails.set(id, trail);
    return;
  }

  trail.setLatLngs(limitedPoints);
}
 private renderTruckRoutes(): void {
  if (!this.map) return;

  this.clearTruckRoutes();

  const colors = ['#2563eb', '#059669', '#7c3aed', '#f97316', '#ef4444', '#0f766e'];

  (this.truckRoutes || []).forEach((route, index) => {
    const color = colors[index % colors.length];

    const coords =
      route.collectionRouteCoordinates && route.collectionRouteCoordinates.length >= 2
        ? route.collectionRouteCoordinates
        : route.routeCoordinates;

    const validCoords = (coords || []).filter((p) => p?.lat != null && p?.lng != null);

    if (validCoords.length < 2) return;

    this.truckRouteCoords.set(route.truckId, validCoords);

    const allLatLngs: L.LatLngExpression[] = validCoords.map((p) => [
      Number(p.lat),
      Number(p.lng),
    ]);

    const completedLine = L.polyline([], {
      color: '#22c55e',
      weight: 7,
      opacity: 1,
    }).addTo(this.map!);

    const remainingLine = L.polyline(allLatLngs, {
      color,
      weight: 6,
      opacity: 0.85,
    }).addTo(this.map!);

    remainingLine.bindPopup(`
      <div style="min-width:180px">
        <b>${route.truckCode || route.truckId}</b><br>
        Mission: ${route.missionId}
      </div>
    `);

    this.truckCompletedRoutePolylines.set(route.truckId, completedLine);
    this.truckRemainingRoutePolylines.set(route.truckId, remainingLine);
  });
}

private clearTruckRoutes(): void {
  if (!this.map) return;

  for (const line of this.truckCompletedRoutePolylines.values()) {
    line.removeFrom(this.map);
  }

  for (const line of this.truckRemainingRoutePolylines.values()) {
    line.removeFrom(this.map);
  }

  this.truckCompletedRoutePolylines.clear();
  this.truckRemainingRoutePolylines.clear();
  this.truckRouteCoords.clear();
}
private updateTruckRouteProgress(truckId: string, pos: LatLng): void {
  const route = this.truckRouteCoords.get(truckId);
  if (!route || route.length < 2) return;

  const completedLine = this.truckCompletedRoutePolylines.get(truckId);
  const remainingLine = this.truckRemainingRoutePolylines.get(truckId);

  if (!completedLine || !remainingLine) return;

  const closestIndex = this.findClosestIndexOnRoute(route, pos);

  const completed = route.slice(0, closestIndex + 1);
  const remaining = route.slice(closestIndex);

  completedLine.setLatLngs(completed.map((p) => [Number(p.lat), Number(p.lng)]));
  remainingLine.setLatLngs(remaining.map((p) => [Number(p.lat), Number(p.lng)]));
}

private findClosestIndexOnRoute(route: FleetMapRouteCoordinate[], pos: LatLng): number {
  let minDist = Infinity;
  let closestIndex = 0;

  route.forEach((p, index) => {
    const d =
      Math.pow(Number(p.lat) - pos.lat, 2) +
      Math.pow(Number(p.lng) - pos.lng, 2);

    if (d < minDist) {
      minDist = d;
      closestIndex = index;
    }
  });

  return closestIndex;
}

private snapToRoute(
  route: FleetMapRouteCoordinate[],
  pos: LatLng
): LatLng {
  let minDist = Infinity;
  let closest: LatLng = pos;

  route.forEach((p) => {
    const d =
      Math.pow(Number(p.lat) - pos.lat, 2) +
      Math.pow(Number(p.lng) - pos.lng, 2);

    if (d < minDist) {
      minDist = d;
      closest = {
        lat: Number(p.lat),
        lng: Number(p.lng),
      };
    }
  });

  return closest;
}


  private removeOldTrucks(keep: Set<string>): void {
    if (!this.map) return;

    for (const [id, marker] of this.truckMarkers.entries()) {
      if (!keep.has(id)) {
        marker.removeFrom(this.map);
        this.truckMarkers.delete(id);
      }
    }
  }

 ngOnDestroy(): void {
  try { this.trucksSub?.unsubscribe(); } catch {}
  try { this.binsSub?.unsubscribe(); } catch {}

  try {
    if (this.showTrucks) {
      this.realtime.disconnect();
    }
  } catch {}

  for (const frame of this.truckAnimationFrames.values()) {
    try { cancelAnimationFrame(frame); } catch {}
  }

  for (const trail of this.truckTrails.values()) {
    try {
      if (this.map) {
        trail.removeFrom(this.map);
      }
    } catch {}
  }

  try {
    if (this.temporaryFocusMarker && this.map) {
      this.temporaryFocusMarker.removeFrom(this.map);
    }
  } catch {}

  try {
    if (this.pendingAddMarker && this.map) {
      this.pendingAddMarker.removeFrom(this.map);
    }
  } catch {}

  try {
    this.clearHeatLayer();
  } catch {}
  try {
  this.clearTruckRoutes();
} catch {}
  try { this.map?.remove(); } catch {}

  this.truckMarkers.clear();
  this.truckTrails.clear();
  this.truckCompletedRoutePolylines.clear();
  this.truckRemainingRoutePolylines.clear();
  this.truckRouteCoords.clear();
  this.truckTrailPoints.clear();
  this.truckLastPositions.clear();
  this.truckAnimationFrames.clear();
  this.followedTruckId = null;
  this.truckRoutePolylines.clear();

  this.binMarkers.clear();
  this.reportMarkers.clear();
  this.missionMarkers.clear();
  this.missionRouteStopMarkers.clear();
  this.snappedWaypointMarkers.clear();
  this.snappedConnectorLines.clear();
  this.planningMissionPolylines.clear();
  this.planningMissionMarkers.clear();
  this.droppedBinMarkers.clear();
}
}