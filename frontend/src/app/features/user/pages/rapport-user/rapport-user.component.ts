import {
  AfterViewInit,
  Component,
  OnDestroy
} from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import * as L from 'leaflet';
import { PublicReportService } from '../../../../services/public-report.service';

type ReportType =
  | 'BIN_FULL'
  | 'OVERFLOW'
  | 'ILLEGAL_DUMP'
  | 'BIN_DAMAGED'
  | 'MISSED_COLLECTION'
  | 'OTHER';

@Component({
  selector: 'app-rapport-user',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './rapport-user.component.html',
  styleUrls: ['./rapport-user.component.css']
})
export class RapportUserComponent implements AfterViewInit, OnDestroy {
  dragging = false;
  locating = false;
  submitting = false;

  photoFile: File | null = null;
  photoPreview: string | null = null;

  reportType: ReportType = 'OVERFLOW';
  address = '';
  description = '';

  coords: { lat: number; lng: number } | null = null;

  private map?: L.Map;
  private marker?: L.Marker;
  private mahdiaPolygon?: L.LatLng[];

  constructor(
    private publicReportService: PublicReportService,
    private location: Location
  ) {}

  ngAfterViewInit(): void {
    this.initMap();

    this.loadMahdiaBoundary()
      .then(() => {
        this.fitToMahdia();
        this.lockMapToMahdia();
        this.enableMapClick();
      })
      .catch((err) => {
        console.error('GeoJSON error:', err);
        this.enableMapClick();
      });
  }

  ngOnDestroy(): void {
    try {
      this.map?.remove();
    } catch {}
  }

  goBack(): void {
    this.location.back();
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging = false;

    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.setPhoto(file);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.setPhoto(file);
    }
  }

  removePhoto(): void {
    this.photoFile = null;
    this.photoPreview = null;
  }

  private setPhoto(file: File): void {
    const maxSize = 10 * 1024 * 1024;

    if (file.size > maxSize) {
      alert('La photo dépasse 10 Mo.');
      return;
    }

    this.photoFile = file;
    this.photoPreview = URL.createObjectURL(file);
  }

  detectLocation(): void {
    if (!navigator.geolocation) {
      alert('La géolocalisation n’est pas supportée par votre navigateur.');
      return;
    }

    this.locating = true;

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;

        this.locating = false;

        if (!this.isInsideMahdia(lat, lng)) {
          alert('Votre position est en dehors de Mahdia.');
          return;
        }

        this.setSelectedPoint(lat, lng, true);
      },
      (error) => {
        console.error(error);
        this.locating = false;
        alert('Impossible de détecter votre position.');
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    );
  }

  submitReport(): void {
    if (!this.reportType) {
      alert('Veuillez choisir un type de signalement.');
      return;
    }

    if (!this.coords && !this.address.trim()) {
      alert('Veuillez choisir un emplacement sur la carte, détecter votre position ou saisir une adresse.');
      return;
    }

    this.submitting = true;

    if (this.coords) {
      if (!this.isInsideMahdia(this.coords.lat, this.coords.lng)) {
        this.submitting = false;
        alert('La position choisie est en dehors de Mahdia.');
        return;
      }

      this.sendReport(this.coords.lat, this.coords.lng, this.address.trim());
      return;
    }

    const fullAddress = this.buildMahdiaAddress(this.address.trim());

    this.publicReportService.geocodeAddress(fullAddress).subscribe({
      next: (results) => {
        if (!results || results.length === 0) {
          this.submitting = false;
          alert('Adresse introuvable.');
          return;
        }

        const first = results[0];
        const lat = Number(first.lat);
        const lng = Number(first.lon);

        if (Number.isNaN(lat) || Number.isNaN(lng)) {
          this.submitting = false;
          alert('Coordonnées invalides pour cette adresse.');
          return;
        }

        if (!this.isInsideMahdia(lat, lng)) {
          this.submitting = false;
          alert('Cette adresse est en dehors de Mahdia.');
          return;
        }

        this.setSelectedPoint(lat, lng, false, first.display_name || fullAddress);
        this.sendReport(lat, lng, this.address.trim());
      },
      error: (err) => {
        console.error('Geocoding failed', err);
        this.submitting = false;
        alert("Impossible de localiser l'adresse.");
      }
    });
  }

  private sendReport(lat: number, lng: number, address: string): void {
    const data = {
      reportType: this.reportType,
      description: this.description?.trim() || '',
      address: address || 'Mahdia',
      latitude: lat,
      longitude: lng
    };

    console.log('REPORT DATA =>', data);

    this.publicReportService.createReport(data, this.photoFile || undefined).subscribe({
      next: () => {
        this.submitting = false;
        alert('Signalement envoyé avec succès.');
        this.resetForm();
      },
      error: (err) => {
        console.error('Create report failed', err);
        this.submitting = false;
        alert("Erreur lors de l'envoi.");
      }
    });
  }

  private resetForm(): void {
    this.dragging = false;
    this.locating = false;
    this.submitting = false;

    this.photoFile = null;
    this.photoPreview = null;

    this.reportType = 'OVERFLOW';
    this.address = '';
    this.description = '';
    this.coords = null;

    if (this.marker) {
      this.marker.remove();
      this.marker = undefined;
    }

    this.fitToMahdia();
  }

  private buildMahdiaAddress(address: string): string {
    const lower = address.toLowerCase();

    if (lower.includes('mahdia')) {
      return address;
    }

    return `${address}, Mahdia, Tunisia`;
  }

  private initMap(): void {
    this.map = L.map('reportUserMap', {
      center: [35.5047, 11.0622],
      zoom: 14,
      minZoom: 12,
      maxZoom: 19
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);
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
        color: '#10b981',
        weight: 3,
        fillOpacity: 0.05
      })
    }).addTo(this.map);

    this.mahdiaPolygon = this.extractPolygonLatLngs(geojson);
  }

  private extractPolygonLatLngs(geojson: any): L.LatLng[] | undefined {
    const geom =
      geojson?.type === 'FeatureCollection'
        ? geojson?.features?.[0]?.geometry
        : geojson?.geometry;

    if (!geom) return undefined;

    if (geom.type === 'Polygon') {
      const outer = geom.coordinates?.[0];
      return outer?.map((p: number[]) => L.latLng(p[1], p[0]));
    }

    if (geom.type === 'MultiPolygon') {
      const outer = geom.coordinates?.[0]?.[0];
      return outer?.map((p: number[]) => L.latLng(p[1], p[0]));
    }

    return undefined;
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

  private enableMapClick(): void {
    if (!this.map) return;

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      const lat = e.latlng.lat;
      const lng = e.latlng.lng;

      if (!this.isInsideMahdia(lat, lng)) {
        alert('Choisissez un point dans Mahdia.');
        return;
      }

      this.setSelectedPoint(lat, lng, true);
    });
  }

  private setSelectedPoint(
    lat: number,
    lng: number,
    updateAddressFromReverseGeocode = true,
    forcedAddress?: string
  ): void {
    this.coords = {
      lat: Number(lat.toFixed(6)),
      lng: Number(lng.toFixed(6))
    };

    if (!this.map) return;

    if (!this.marker) {
      this.marker = L.marker([lat, lng], {
        icon: this.makeSelectedLocationIcon()
      }).addTo(this.map);
    } else {
      this.marker.setLatLng([lat, lng]);
    }

    this.map.setView([lat, lng], Math.max(this.map.getZoom(), 16));

    if (forcedAddress) {
      this.address = forcedAddress;
      this.marker.bindPopup(`
        <div style="min-width:220px">
          <div style="font-weight:700; margin-bottom:6px;">Point sélectionné</div>
          <div><b>Lat:</b> ${this.coords.lat}</div>
          <div><b>Lng:</b> ${this.coords.lng}</div>
          <div style="margin-top:6px;">${forcedAddress}</div>
        </div>
      `).openPopup();
      return;
    }

    if (!updateAddressFromReverseGeocode) {
      return;
    }

    this.publicReportService.reverseGeocode(lat, lng).subscribe({
      next: (res) => {
        const displayName = res?.display_name || `Lat: ${this.coords?.lat}, Lng: ${this.coords?.lng}`;
        this.address = displayName;

        this.marker?.bindPopup(`
          <div style="min-width:220px">
            <div style="font-weight:700; margin-bottom:6px;">Point sélectionné</div>
            <div><b>Lat:</b> ${this.coords?.lat}</div>
            <div><b>Lng:</b> ${this.coords?.lng}</div>
            <div style="margin-top:6px;">${displayName}</div>
          </div>
        `).openPopup();
      },
      error: (err) => {
        console.error('Reverse geocoding failed', err);

        this.marker?.bindPopup(`
          <div style="min-width:220px">
            <div style="font-weight:700; margin-bottom:6px;">Point sélectionné</div>
            <div><b>Lat:</b> ${this.coords?.lat}</div>
            <div><b>Lng:</b> ${this.coords?.lng}</div>
          </div>
        `).openPopup();
      }
    });
  }

  private makeSelectedLocationIcon(): L.DivIcon {
    return L.divIcon({
      className: 'selected-location-marker',
      iconSize: [22, 22],
      iconAnchor: [11, 11],
      html: `
        <div style="
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: #ef4444;
          border: 4px solid white;
          box-shadow: 0 2px 8px rgba(0,0,0,0.25);
        "></div>
      `
    });
  }

  private isInsideMahdia(lat: number, lng: number): boolean {
    const poly = this.mahdiaPolygon;
    if (!poly || poly.length < 3) {
      const minLat = 35.477;
      const maxLat = 35.548;
      const minLng = 11.017;
      const maxLng = 11.108;
      return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
    }

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
}