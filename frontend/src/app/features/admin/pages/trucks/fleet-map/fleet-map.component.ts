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
import { Subscription } from 'rxjs';
import { RealtimeService, TruckLocationMsg } from '../../../../../services/realtime.service';
import { BinService } from '../../../../../services/bin.service';
import { MapFocusService } from '../../../../../services/map-focus.service';

type LatLng = { lat: number; lng: number };

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
}

export interface FleetMapRouteCoordinate {
  lat: number;
  lng: number;
}

export interface FleetMapRouteStop {
  stopOrder: number;
  stopType: string | null;
  binId: number | null;
  lat: number;
  lng: number;
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
  @Input() title = 'Carte de la flotte en direct';
  @Input() subtitle = 'Localisation et itinéraires des camions en temps réel';
  @Input() reports: FleetMapReportItem[] = [];
  @Input() missionBins: FleetMapMissionBinItem[] = [];
  @Input() missionRouteCoordinates: FleetMapRouteCoordinate[] = [];
  @Input() missionRouteStops: FleetMapRouteStop[] = [];
  @Input() snappedWaypoints: FleetMapRouteCoordinate[] = [];
  @Input() matrixSource: string | null = null;
  @Input() geometrySource: string | null = 'OSRM';

  private map?: L.Map;
  private trucksSub?: Subscription;
  private binsSub?: Subscription;

  private parisPolygon?: L.LatLng[];
  private pendingAddMarker?: L.Marker;
  private temporaryFocusMarker?: L.Marker;

  private truckMarkers = new Map<string, L.Marker>();
  private binMarkers = new Map<number | string, L.Layer>();
  private reportMarkers = new Map<number, L.Marker>();

  private missionMarkers = new Map<number, L.Layer>();
  private missionPolyline?: L.Polyline;
  private missionRealRoutePolyline?: L.Polyline;
  private missionRouteStopMarkers = new Map<number, L.Layer>();
  private snappedWaypointMarkers = new Map<number, L.Layer>();
  private snappedConnectorLines = new Map<number, L.Polyline>();

  addBinMode = false;

  showAddBinModal = false;
  creatingBin = false;
  addBinError = '';
  addBinSuccess = '';

  newBin = {
    binCode: '',
    type: 'SIM',
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
          this.fitToParis();
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
            this.startRealtime();
          }

          if (this.allowAddBins) {
            this.enableAddBinClick();
            this.activateAddModeFromQuery();
          }

          this.renderMissionBins();

          setTimeout(() => {
            this.applyPendingFocus();
            this.map?.invalidateSize();
          }, 300);
        });

      setTimeout(() => {
        this.map?.invalidateSize();
      }, 300);
    }, 0);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['reports'] && this.map && this.showReports) {
      this.loadReportsOnMap();

      setTimeout(() => {
        this.applyPendingFocus();
      }, 100);
    }

    if (
      (
        changes['missionBins'] ||
        changes['missionRouteCoordinates'] ||
        changes['missionRouteStops'] ||
        changes['snappedWaypoints']
      ) && this.map
    ) {
      this.renderMissionBins();
    }
  }

  toggleAddBinMode(): void {
    if (!this.allowAddBins) return;

    this.addBinMode = !this.addBinMode;

    if (!this.addBinMode) {
      this.closeAddBinModal();
    }
  }

  closeAddBinModal(): void {
    this.showAddBinModal = false;
    this.creatingBin = false;
    this.addBinError = '';
    this.addBinSuccess = '';

    this.newBin = {
      binCode: '',
      type: 'SIM',
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

    const payload: any = {
      type: this.newBin.type,
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
    this.showAddBinModal = true;

    this.newBin = {
      binCode: '',
      type: 'SIM',
      lat,
      lng,
      notes: '',
      installationDate: '',
      isActive: true
    };

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
  }

  private loadBins(): void {
    this.binsSub?.unsubscribe();

    this.binsSub = this.binService.getBins().subscribe({
      next: (bins: any[]) => {
        if (!this.map) return;

        const map = this.map;
        this.clearBinMarkers();

        bins.forEach((bin) => {
          if (bin.lat == null || bin.lng == null) return;
          if (!this.isInsideParis(bin.lat, bin.lng)) return;

          const color = this.getBinColor(bin);

          const marker = L.circleMarker([bin.lat, bin.lng], {
            radius: 8,
            color,
            fillColor: color,
            fillOpacity: 0.9,
            weight: 2
          });

          marker.addTo(map).bindPopup(`
            <div style="min-width:220px">
              <div style="font-weight:700; margin-bottom:6px;">${bin.binCode ?? 'BIN'}</div>
              <div><b>Zone:</b> ${bin.zoneName ?? '—'}</div>
              <div><b>Fill:</b> ${bin.fillLevel ?? 0}%</div>
              <div><b>Battery:</b> ${bin.batteryLevel ?? 0}%</div>
              <div><b>Status:</b> ${bin.status ?? 'OK'}</div>
            </div>
          `);

          this.binMarkers.set(bin.binId ?? bin.id ?? bin.binCode, marker);
        });

        this.renderMissionBins();

        setTimeout(() => {
          this.applyPendingFocus();
        }, 150);
      },
      error: (err) => {
        console.error('Error bins', err);
      }
    });
  }

  private renderMissionBins(): void {
    if (!this.map) return;

    this.clearMissionMarkers();

    const bins = (this.missionBins || [])
      .filter(b => b.lat != null && b.lng != null)
      .sort((a, b) => a.visitOrder - b.visitOrder);

    const routeCoords = (this.missionRouteCoordinates || [])
      .filter(p => p.lat != null && p.lng != null);

    const routeStops = (this.missionRouteStops || [])
      .filter(s => s.lat != null && s.lng != null);

    const snapped = (this.snappedWaypoints || [])
      .filter(p => p.lat != null && p.lng != null);

    if (!bins.length && !routeCoords.length && !routeStops.length && !snapped.length) return;

    const boundsPoints: L.LatLngExpression[] = [];

    bins.forEach((bin) => {
      const color = bin.collected ? '#16a34a' : '#f59e0b';

      const marker = L.circleMarker([bin.lat, bin.lng], {
        radius: 10,
        color,
        fillColor: color,
        fillOpacity: 0.95,
        weight: 3
      });

      marker.addTo(this.map!).bindPopup(`
        <div style="min-width:220px">
          <div style="font-weight:700; margin-bottom:6px;">${bin.binCode || 'BIN-' + bin.binId}</div>
          <div><b>Ordre:</b> ${bin.visitOrder}</div>
          <div><b>Statut:</b> ${bin.collected ? 'Collecté' : 'À collecter'}</div>
          <div><b>Seuil:</b> ${bin.targetFillThreshold ?? '—'}%</div>
        </div>
      `);

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

      if (stop.stopType === 'DEPOT_START') {
        color = '#16a34a';
        fillColor = '#16a34a';
        radius = 7;
      } else if (stop.stopType === 'DEPOT_RETURN') {
        color = '#dc2626';
        fillColor = '#dc2626';
        radius = 7;
      } else if (stop.stopType === 'BIN_PICKUP') {
        color = '#111827';
        fillColor = '#ffffff';
        radius = 5;
      }

      const marker = L.circleMarker([stop.lat, stop.lng], {
        radius,
        color,
        fillColor,
        fillOpacity: 1,
        weight: 2
      });

      marker.addTo(this.map!).bindPopup(`
        <div style="min-width:180px">
          <div style="font-weight:700; margin-bottom:6px;">Route stop</div>
          <div><b>Order:</b> ${stop.stopOrder}</div>
          <div><b>Type:</b> ${stop.stopType ?? '—'}</div>
          <div><b>Bin ID:</b> ${stop.binId ?? '—'}</div>
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

    if (routeCoords.length >= 2) {
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
      const bounds = L.latLngBounds(boundsPoints as [number, number][]);
      this.map!.fitBounds(bounds, { padding: [30, 30] });
    }
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

    if (this.missionPolyline) {
      this.missionPolyline.removeFrom(this.map);
      this.missionPolyline = undefined;
    }

    if (this.missionRealRoutePolyline) {
      this.missionRealRoutePolyline.removeFrom(this.map);
      this.missionRealRoutePolyline = undefined;
    }
  }

  private getBinColor(bin: any): string {
    const fill = Number(bin.fillLevel ?? 0);
    const status = (bin.status ?? '').toUpperCase();

    if (status === 'ERROR') return '#111827';
    if (status === 'OVERFLOW' || status === 'FULL' || fill >= 90) return '#ef4444';
    if (status === 'WARNING' || fill >= 60) return '#f59e0b';
    return '#16a34a';
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

  private startRealtime(): void {
    this.realtime.connect({
      wsUrl: 'ws://localhost:8081/ws',
      topic: '/topic/truck-locations'
    });

    this.trucksSub = this.realtime.trucks$.subscribe((trucks) => {
      if (!this.map) return;

      const keep = new Set<string>();

      for (const [id, payload] of trucks.entries()) {
        const pos = this.extractLatLng(payload);
        if (!pos) continue;
        if (!this.isInsideParis(pos.lat, pos.lng)) continue;

        keep.add(id);
        this.upsertTruckMarker(id, pos);
      }

      this.removeOldTrucks(keep);
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

  private makeTruckIcon(): L.DivIcon {
    return L.divIcon({
      className: 'truck-icon',
      iconSize: [40, 40],
      iconAnchor: [20, 20],
      html: `
        <img
          src="${this.truckIconUrl}"
          style="width:40px;height:40px"
        />
      `
    });
  }

  private upsertTruckMarker(id: string, pos: LatLng): void {
    if (!this.map) return;

    const existing = this.truckMarkers.get(id);

    if (!existing) {
      const marker = L.marker([pos.lat, pos.lng], {
        icon: this.makeTruckIcon(),
        zIndexOffset: 1000
      }).addTo(this.map);

      marker.bindPopup(`Truck ${id}`);
      this.truckMarkers.set(id, marker);
    } else {
      existing.setLatLng([pos.lat, pos.lng]);
    }
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

    try { this.map?.remove(); } catch {}

    this.truckMarkers.clear();
    this.binMarkers.clear();
    this.reportMarkers.clear();
    this.missionMarkers.clear();
    this.missionRouteStopMarkers.clear();
    this.snappedWaypointMarkers.clear();
    this.snappedConnectorLines.clear();
  }
}