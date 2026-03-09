import { AfterViewInit, Component, Input, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { Subscription } from 'rxjs';
import { RealtimeService, TruckLocationMsg } from '../../../../../services/realtime.service';
import { BinService } from '../../../../../services/bin.service';

type LatLng = { lat: number; lng: number };

@Component({
  selector: 'app-fleet-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fleet-map.component.html',
  styleUrls: ['./fleet-map.component.css'],
})
export class FleetMapComponent implements AfterViewInit, OnDestroy {
  @Input() showTrucks = true;
  @Input() allowAddBins = false;
  @Input() showHeader = true;
  @Input() title = 'Carte de la flotte en direct';
  @Input() subtitle = 'Localisation et itinéraires des camions en temps réel';

  private map?: L.Map;
  private trucksSub?: Subscription;
  private binsSub?: Subscription;

  private mahdiaPolygon?: L.LatLng[];

  private truckMarkers = new Map<string, L.Marker>();
  private binMarkers = new Map<number, L.Layer>();

  addBinMode = false;

  private readonly truckIconUrl = 'icons/truck.png';

  constructor(
    private realtime: RealtimeService,
    private binService: BinService
  ) {}

  ngAfterViewInit(): void {
    this.initMap();

    this.loadMahdiaBoundary()
      .then(() => {
        this.fitToMahdia();
        this.lockMapToMahdia();
      })
      .catch((e) => console.error('GeoJSON error:', e))
      .finally(() => {
        this.loadBins();

        if (this.showTrucks) {
          this.startRealtime();
        }

        if (this.allowAddBins) {
          this.enableAddBinClick();
        }
      });
  }

  toggleAddBinMode(): void {
    if (!this.allowAddBins) return;
    this.addBinMode = !this.addBinMode;
  }

  private initMap(): void {
    this.map = L.map('fleetMap', {
      center: [35.505, 11.062],
      zoom: 13,
      minZoom: 12,
      maxZoom: 19
    });

    L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      {
        attribution: '&copy; OpenStreetMap contributors'
      }
    ).addTo(this.map);
  }

  private async loadMahdiaBoundary(): Promise<void> {
    const res = await fetch('geo/mahdia.geojson');
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

    this.mahdiaPolygon = this.extractPolygonLatLngs(geojson);
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

  private fitToMahdia(): void {
    if (!this.map || !this.mahdiaPolygon?.length) return;
    const bounds = L.latLngBounds(this.mahdiaPolygon);
    this.map.fitBounds(bounds, { padding: [20, 20] });
  }

  private lockMapToMahdia(): void {
    if (!this.map || !this.mahdiaPolygon?.length) return;

    const bounds = L.latLngBounds(this.mahdiaPolygon);
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

      if (!this.isInsideMahdia(lat, lng)) {
        alert('Choisis un point داخل Mahdia');
        return;
      }

      const confirmed = confirm('Ajouter une poubelle ici ?');

      if (!confirmed) {
        return;
      }

      const payload = {
        type: 'SIM',
        lat,
        lng,
        notes: 'Added from overview map'
      };

      this.binService.createBin(payload).subscribe({
        next: (created: any) => {
          alert(`Poubelle ajoutée: ${created?.binCode ?? 'BIN'}`);
          this.addBinMode = false;
          this.loadBins();
        },
        error: (err) => {
          console.error(err);
          alert('Erreur création bin');
        }
      });
    });
  }

  private loadBins(): void {
    this.binsSub?.unsubscribe();

    this.binsSub = this.binService.getBins().subscribe({
      next: (bins: any[]) => {
        this.clearBinMarkers();

        bins.forEach(bin => {
          const marker = L.circleMarker([bin.lat, bin.lng], {
            radius: 8,
            color: this.getBinColor(bin),
            fillColor: this.getBinColor(bin),
            fillOpacity: 0.9,
            weight: 2
          });

          marker.addTo(this.map!)
            .bindPopup(`
              <b>${bin.binCode}</b><br/>
              Fill: ${bin.fillLevel ?? 0}%<br/>
              Battery: ${bin.batteryLevel ?? 0}%<br/>
              Status: ${bin.status ?? 'OK'}
            `);

          this.binMarkers.set(bin.binId, marker);
        });
      },
      error: (err) => {
        console.error('Error bins', err);
      }
    });
  }

  private clearBinMarkers(): void {
    if (!this.map) return;

    for (const marker of this.binMarkers.values()) {
      marker.removeFrom(this.map);
    }

    this.binMarkers.clear();
  }

  private getBinColor(bin: any): string {
    const fill = Number(bin.fillLevel ?? 0);
    const status = (bin.status ?? '').toUpperCase();

    if (status === 'ERROR') return '#111827';
    if (status === 'OVERFLOW' || status === 'FULL' || fill >= 90) return '#ef4444';
    if (status === 'WARNING' || fill >= 60) return '#f59e0b';
    return '#16a34a';
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

        if (!this.isInsideMahdia(pos.lat, pos.lng)) continue;

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

  private isInsideMahdia(lat: number, lng: number): boolean {
    const poly = this.mahdiaPolygon;
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
    try { this.map?.remove(); } catch {}

    this.truckMarkers.clear();
    this.binMarkers.clear();
  }
}