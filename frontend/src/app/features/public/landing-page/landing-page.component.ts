
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import * as L from 'leaflet';

import { FooterComponent } from '../footer/footer.component';
import { PublicHeaderComponent } from '../header/header.component';
import { ThemeService } from '../../../services/theme.service';
import { PublicReportService } from '../../../services/public-report.service';

type BinItem = {
  code: string;
  zone: string;
  fillLevel: number;
  distance?: string;
  lat?: number;
  lng?: number;
};

type ChatMessage = {
  type: 'bot' | 'user';
  text: string;
  bins?: BinItem[];
  showAllBins?: boolean;
  showLocationActions?: boolean;
};

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PublicHeaderComponent,
    FooterComponent,
    RouterModule,
    MatIconModule
  ],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.css'
})
export class LandingPageComponent implements OnDestroy, OnInit {

  chatOpen = false;
  message = '';
  isSending = false;
  mapPickerOpen = false;
  binMapOpen = false;

  pendingAddress = '';
  pendingLocationLabel = '';
  showWasteChoice = false;
  selectedWasteTypes: string[] = [];
  selectedUserCoords: { lat: number; lng: number } | null = null;
  private pendingRouteBin?: BinItem;

  private pickerMap?: L.Map;
  private pickerMarker?: L.Marker;
  private binRouteMap?: L.Map;
  private parisPolygon?: L.LatLng[];

  wasteTypes = [
    { label: 'Déchets ménagers', value: 'GRAY' },
    { label: 'Verre', value: 'GREEN' },
    { label: 'Plastique', value: 'YELLOW' },
    { label: 'Papier', value: 'WHITE' }
  ];

  messages: ChatMessage[] = [
    {
      type: 'bot',
      text: 'Bonjour 👋 Vous pouvez écrire votre localisation ou utiliser votre position actuelle pour trouver la poubelle disponible la plus proche.',
      bins: [],
      showLocationActions: true
    }
  ];

  constructor(
    private themeService: ThemeService,
    private publicReportService: PublicReportService
  ) {}

  ngOnInit(): void {
    this.themeService.initTheme();
  }

  ngOnDestroy(): void {
    this.destroyPickerMap();
    this.destroyBinRouteMap();
  }

  toggleChat(): void {
    this.chatOpen = !this.chatOpen;
  }

  send(): void {

    const text = this.message.trim();

    if (!text || this.isSending) return;

    if (this.showWasteChoice) {

      this.messages.push({
        type: 'bot',
        text: 'Veuillez choisir un type de poubelle dans la liste ci-dessous 👇',
        bins: []
      });

      this.message = '';
      return;
    }

    this.messages.push({
      type: 'user',
      text,
      bins: []
    });

    if (!this.looksLikeLocation(text)) {
      this.messages.push({
        type: 'bot',
        text: 'Bonjour 👋 Vous pouvez écrire votre localisation ou utiliser votre position actuelle pour trouver la poubelle disponible la plus proche.',
        bins: [],
        showLocationActions: true
      });

      this.message = '';
      return;
    }

    this.pendingAddress = text;
    this.pendingLocationLabel = text;
    this.selectedUserCoords = null;

    this.message = '';

    this.selectedWasteTypes = [];

    this.showWasteChoice = true;

    this.messages.push({
      type: 'bot',
      text: 'Quel type de poubelle cherchez-vous ?',
      bins: []
    });
  }

  useCurrentLocation(): void {

    if (!navigator.geolocation) {

      this.messages.push({
        type: 'bot',
        text: 'La géolocalisation n’est pas supportée par votre navigateur.',
        bins: []
      });

      return;
    }

    this.messages.push({
      type: 'user',
      text: '📍 Utiliser ma position actuelle',
      bins: []
    });

    navigator.geolocation.getCurrentPosition(

      (position) => {

        const lat = position.coords.latitude;
        const lng = position.coords.longitude;

        this.pendingAddress = `${lat},${lng}`;
        this.pendingLocationLabel = '';
        this.selectedUserCoords = {
          lat: Number(lat.toFixed(6)),
          lng: Number(lng.toFixed(6))
        };

        if (this.pendingRouteBin) {
          const bin = this.pendingRouteBin;
          this.pendingRouteBin = undefined;
          this.showWasteChoice = false;
          this.showBinOnMap(bin);
          return;
        }

        this.selectedWasteTypes = [];

        this.showWasteChoice = true;

        this.messages.push({
          type: 'bot',
          text: 'Position actuelle reçue ✅ Quel type de poubelle cherchez-vous ?',
          bins: []
        });

      },

      () => {

        this.messages.push({
          type: 'bot',
          text: 'Impossible d’accéder à votre position. Veuillez écrire votre localisation manuellement.',
          bins: []
        });

      }

    );
  }

  openMapPicker(): void {
    this.mapPickerOpen = true;
    this.chatOpen = true;

    setTimeout(() => {
      this.destroyPickerMap();
      this.initPickerMap();
      this.loadParis15Boundary()
        .then(() => {
          this.fitPickerToParis();
          this.lockPickerToParis();
          this.enablePickerClick();
        })
        .catch(() => {
          this.messages.push({
            type: 'bot',
            text: 'Impossible de charger la carte Paris 15 pour le moment.',
            bins: []
          });
        });
    });
  }

  closeMapPicker(): void {
    this.mapPickerOpen = false;
    this.destroyPickerMap();
  }

  closeBinMap(): void {
    this.binMapOpen = false;
    this.destroyBinRouteMap();
  }

  getVisibleBins(msg: ChatMessage): BinItem[] {
    const bins = msg.bins ?? [];
    return msg.showAllBins ? bins : bins.slice(0, 5);
  }

  canShowMoreBins(msg: ChatMessage): boolean {
    return !!msg.bins && msg.bins.length > 5 && !msg.showAllBins;
  }

  showMoreBins(msg: ChatMessage): void {
    msg.showAllBins = true;
  }

  showBinOnMap(bin: BinItem): void {
    if (!this.selectedUserCoords) {
      this.pendingRouteBin = bin;
      this.messages.push({
        type: 'bot',
        text: 'Veuillez sélectionner votre position.',
        bins: [],
        showLocationActions: true
      });
      return;
    }

    const binLat = this.toNumberOrUndefined(bin.lat);
    const binLng = this.toNumberOrUndefined(bin.lng);

    if (typeof binLat !== 'number' || typeof binLng !== 'number') {
      this.messages.push({
        type: 'bot',
        text: 'La position de cette poubelle est indisponible.',
        bins: []
      });
      return;
    }

    this.binMapOpen = true;

    setTimeout(async () => {
      this.destroyBinRouteMap();

      this.binRouteMap = L.map('chatbotBinMap', {
        center: [this.selectedUserCoords!.lat, this.selectedUserCoords!.lng],
        zoom: 16,
        minZoom: 12,
        maxZoom: 19
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(this.binRouteMap);

      const userLatLng: L.LatLngExpression = [
        this.selectedUserCoords!.lat,
        this.selectedUserCoords!.lng
      ];
      const binLatLng: L.LatLngExpression = [binLat, binLng];

      L.marker(userLatLng, {
        icon: this.makeMapIcon('my_location', '#2563eb')
      }).addTo(this.binRouteMap).bindPopup('Votre position');

      L.marker(binLatLng, {
        icon: this.makeMapIcon('delete', '#10b981')
      }).addTo(this.binRouteMap).bindPopup(bin.zone || bin.code);

      await this.drawRouteToBin(userLatLng, binLatLng);

    });
  }

  private async drawRouteToBin(
    userLatLng: L.LatLngExpression,
    binLatLng: L.LatLngExpression
  ): Promise<void> {
    if (!this.binRouteMap) return;

    const userPoint = userLatLng as [number, number];
    const binPoint = binLatLng as [number, number];

    try {
      const route = await this.fetchRoute(userPoint, binPoint);

      L.polyline(route, {
        color: '#10b981',
        weight: 5,
        opacity: 0.95
      }).addTo(this.binRouteMap);

      this.binRouteMap.fitBounds(L.latLngBounds(route), {
        padding: [35, 35],
        maxZoom: 17
      });
    } catch {
      L.polyline([userLatLng, binLatLng], {
        color: '#10b981',
        weight: 5,
        opacity: 0.9,
        dashArray: '8 8'
      }).addTo(this.binRouteMap);
    }
  }

  private async fetchRoute(
    userPoint: [number, number],
    binPoint: [number, number]
  ): Promise<L.LatLngExpression[]> {
    const profiles = ['foot', 'driving'];

    for (const profile of profiles) {
      const url =
        `https://router.project-osrm.org/route/v1/${profile}/` +
        `${userPoint[1]},${userPoint[0]};${binPoint[1]},${binPoint[0]}` +
        '?overview=full&geometries=geojson';

      const res = await fetch(url);
      if (!res.ok) continue;

      const data = await res.json();
      const coordinates = data?.routes?.[0]?.geometry?.coordinates;

      if (Array.isArray(coordinates) && coordinates.length > 1) {
        return coordinates.map((point: number[]) => [point[1], point[0]]);
      }
    }

    throw new Error('No route found');
  }

  private looksLikeLocation(text: string): boolean {
    const value = text.trim().toLowerCase();

    if (/\d+/.test(value)) {
      return true;
    }

    const locationWords = [
      'rue',
      'avenue',
      'av ',
      'boulevard',
      'bd ',
      'place',
      'quai',
      'impasse',
      'passage',
      'paris',
      'javel',
      'saint-lambert',
      'saint lambert',
      'saintlambert',
      'necker',
      'commerce',
      'convention',
      'vaugirard',
      'grenelle',
      'pasteur',
      'cambronne',
      'volontaires',
      'balard'
    ];

    return locationWords.some(word => value.includes(word));
  }

  toggleWasteType(value: string): void {

    if (this.selectedWasteTypes.includes(value)) {

      this.selectedWasteTypes =
        this.selectedWasteTypes.filter(v => v !== value);

    } else {

      this.selectedWasteTypes.push(value);

    }
  }

  async searchBinsByType(): Promise<void> {

    if (
      !this.pendingAddress ||
      this.selectedWasteTypes.length === 0 ||
      this.isSending
    ) return;

    this.isSending = true;

    this.showWasteChoice = false;

    const loadingIndex = this.messages.length;

    this.messages.push({
      type: 'bot',
      text: '🔎 Recherche des poubelles disponibles...',
      bins: []
    });

    try {

      const res = await fetch(
        'http://localhost:8081/api/chatbot',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            message: this.pendingAddress,
            locationLabel: this.pendingLocationLabel,
            wasteTypes: this.selectedWasteTypes
          })
        }
      );

      if (!res.ok) {
        throw new Error(`HTTP error ${res.status}`);
      }

      const data = await res.json();

      this.messages[loadingIndex] = {
        type: 'bot',
        text: data.reply || 'Aucune réponse.',
        bins: Array.isArray(data.bins)
          ? data.bins.map((bin: any) => ({
            ...bin,
            lat: this.toNumberOrUndefined(bin.lat ?? bin.latitude),
            lng: this.toNumberOrUndefined(bin.lng ?? bin.longitude)
          }))
          : []
      };

    } catch {

      this.messages[loadingIndex] = {
        type: 'bot',
        text: 'Erreur serveur ❌ Réessayez plus tard.',
        bins: []
      };

    } finally {

      this.isSending = false;

      this.pendingAddress = '';
      this.pendingLocationLabel = '';

      this.selectedWasteTypes = [];

    }
  }

  private initPickerMap(): void {
    this.pickerMap = L.map('chatbotParisMap', {
      center: [48.8412, 2.3003],
      zoom: 13,
      minZoom: 12,
      maxZoom: 19
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.pickerMap);
  }

  private async loadParis15Boundary(): Promise<void> {
    const res = await fetch('geo/paris-15.geojson');
    if (!res.ok) {
      throw new Error(`GeoJSON fetch failed ${res.status}`);
    }

    const geojson = await res.json();
    if (!this.pickerMap) return;

    L.geoJSON(geojson, {
      style: () => ({
        color: '#10b981',
        weight: 3,
        fillOpacity: 0.06
      })
    }).addTo(this.pickerMap);

    this.parisPolygon = this.extractPolygonLatLngs(geojson);
  }

  private extractPolygonLatLngs(geojson: any): L.LatLng[] | undefined {
    const geom =
      geojson?.type === 'FeatureCollection'
        ? geojson?.features?.[0]?.geometry
        : geojson?.geometry;

    if (!geom) return undefined;

    if (geom.type === 'Polygon') {
      return geom.coordinates?.[0]?.map((p: number[]) => L.latLng(p[1], p[0]));
    }

    if (geom.type === 'MultiPolygon') {
      return geom.coordinates?.[0]?.[0]?.map((p: number[]) => L.latLng(p[1], p[0]));
    }

    return undefined;
  }

  private fitPickerToParis(): void {
    if (!this.pickerMap || !this.parisPolygon?.length) return;
    this.pickerMap.fitBounds(L.latLngBounds(this.parisPolygon), { padding: [20, 20] });
  }

  private lockPickerToParis(): void {
    if (!this.pickerMap || !this.parisPolygon?.length) return;

    const bounds = L.latLngBounds(this.parisPolygon);
    this.pickerMap.setMaxBounds(bounds.pad(0.02));
    this.pickerMap.setMinZoom(this.pickerMap.getZoom());
    this.pickerMap.on('drag', () => {
      this.pickerMap?.panInsideBounds(bounds, { animate: false });
    });
  }

  private enablePickerClick(): void {
    if (!this.pickerMap) return;

    this.pickerMap.on('click', (e: L.LeafletMouseEvent) => {
      const lat = e.latlng.lat;
      const lng = e.latlng.lng;

      if (!this.isInsideParis15(lat, lng)) {
        this.messages.push({
          type: 'bot',
          text: 'Choisissez un point dans Paris 15 uniquement.',
          bins: []
        });
        return;
      }

      this.selectPointFromMap(lat, lng);
    });
  }

  private selectPointFromMap(lat: number, lng: number): void {
    if (!this.pickerMap) return;

    const selectedLat = Number(lat.toFixed(6));
    const selectedLng = Number(lng.toFixed(6));

    if (!this.pickerMarker) {
      this.pickerMarker = L.marker([lat, lng]).addTo(this.pickerMap);
    } else {
      this.pickerMarker.setLatLng([lat, lng]);
    }

    this.pendingAddress = `${selectedLat},${selectedLng}`;
    this.pendingLocationLabel = '';
    this.selectedUserCoords = {
      lat: selectedLat,
      lng: selectedLng
    };
    const routeBin = this.pendingRouteBin;
    this.selectedWasteTypes = [];
    this.showWasteChoice = !routeBin;
    this.closeMapPicker();

    if (routeBin) {
      this.pendingRouteBin = undefined;
      this.showBinOnMap(routeBin);
      return;
    }

    this.publicReportService.reverseGeocode(selectedLat, selectedLng).subscribe({
      next: (res) => {
        const address = res?.display_name || 'Adresse sélectionnée dans Paris 15';
        this.pendingLocationLabel = address;

        this.messages.push({
          type: 'user',
          text: `Adresse sélectionnée : ${address}`,
          bins: []
        });

        this.messages.push({
          type: 'bot',
          text: 'Adresse Paris 15 reçue ✅ Quel type de poubelle cherchez-vous ?',
          bins: []
        });
      },
      error: () => {
        this.messages.push({
          type: 'user',
          text: 'Adresse sélectionnée dans Paris 15',
          bins: []
        });

        this.messages.push({
          type: 'bot',
          text: 'Adresse Paris 15 reçue ✅ Quel type de poubelle cherchez-vous ?',
          bins: []
        });
      }
    });

    return;
    this.mapPickerOpen = false;

    this.messages.push({
      type: 'user',
      text: `Point sélectionné sur la carte (${selectedLat}, ${selectedLng})`,
      bins: []
    });

    this.messages.push({
      type: 'bot',
      text: 'Point Paris 15 reçu ✅ Quel type de poubelle cherchez-vous ?',
      bins: []
    });
  }

  private destroyPickerMap(): void {
    this.pickerMap?.remove();
    this.pickerMap = undefined;
    this.pickerMarker = undefined;
    this.parisPolygon = undefined;
  }

  private destroyBinRouteMap(): void {
    this.binRouteMap?.remove();
    this.binRouteMap = undefined;
  }

  private toNumberOrUndefined(value: unknown): number | undefined {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }

    if (typeof value === 'string') {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : undefined;
    }

    return undefined;
  }

  private makeMapIcon(iconName: string, color: string): L.DivIcon {
    return L.divIcon({
      className: 'chatbot-map-marker',
      iconSize: [34, 34],
      iconAnchor: [17, 17],
      html: `
        <div style="
          width: 34px;
          height: 34px;
          border-radius: 50%;
          background: ${color};
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 3px solid white;
          box-shadow: 0 6px 16px rgba(15,23,42,.28);
        ">
          <span class="material-icons" style="font-size:18px;">${iconName}</span>
        </div>
      `
    });
  }

  private isInsideParis15(lat: number, lng: number): boolean {
    const poly = this.parisPolygon;
    if (!poly || poly.length < 3) {
      return true;
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
