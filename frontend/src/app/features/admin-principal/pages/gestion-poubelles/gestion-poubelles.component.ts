import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BinService, BinStatusDto } from '../../../../services/bin.service';
import * as L from 'leaflet';

type FiltrePoubelle = 'Toutes' | 'Actives' | 'Pleines' | 'Maintenance';
type StatutPoubelle = 'Active' | 'Full' | 'Maintenance';

interface Poubelle {
  id?: number;
  nom: string;
  zone: string;
  code: string;
  statut: StatutPoubelle;
  niveauRemplissage: number;
  capacite: string;
  batterie: number;
  signal: number;
  temperature: string;
  derniereCollecte: string;
  prochaineCollecte: string;
  zoneId?: number | null;
  lat?: number | null;
  lng?: number | null;
  wasteType?: string | null;
  isActive?: boolean | null;
  notes?: string | null;
}

@Component({
  selector: 'app-gestion-poubelles',
  templateUrl: './gestion-poubelles.component.html',
  styleUrls: ['./gestion-poubelles.component.css']
})
export class GestionPoubellesComponent implements OnInit {
  termeRecherche = '';
  filtreSelectionne: FiltrePoubelle = 'Toutes';
  filtres: FiltrePoubelle[] = ['Toutes', 'Actives', 'Pleines', 'Maintenance'];

  poubelles: Poubelle[] = [];
  zones: any[] = [];
  zoneIds: number[] = [];

  loading = false;
  errorMessage = '';

  showQrModal = false;
  qrPreviewUrl = '';
  selectedBin: Poubelle | null = null;
  qrLoading = false;

  showAddModal = false;
  addLoading = false;

  showEditModal = false;
  editLoading = false;
  editingBin: Poubelle | null = null;

  menuOpenId: number | null = null;

  addMap: L.Map | null = null;
  addMarker: L.Marker | null = null;

  editMap: L.Map | null = null;
  editMarker: L.Marker | null = null;

  existingBinMarkers: L.Marker[] = [];

  newBin = {
    binCode: '',
    type: '',
    zoneId: null as number | null,
    lat: null as number | null,
    lng: null as number | null,
    installationDate: '',
    isActive: true,
    notes: ''
  };

  editForm = {
    type: '',
    zoneId: null as number | null,
    lat: null as number | null,
    lng: null as number | null,
    isActive: true,
    notes: ''
  };

  editOriginal = {
    zoneId: null as number | null,
    lat: null as number | null,
    lng: null as number | null,
    wasteType: '',
    isActive: true,
    notes: ''
  };

  private readonly API = 'http://localhost:8081/api/bins';
  private readonly ZONES_API = 'http://localhost:8081/api/zones';

  constructor(
    private binService: BinService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.chargerPoubelles();
    this.loadZones();
  }

  loadZones(): void {
    this.http.get<any[]>(this.ZONES_API).subscribe({
      next: (data) => {
        this.zones = data || [];
        this.zoneIds = this.zones.map(z => Number(z.id));
      },
      error: (error) => console.error('Erreur chargement zones', error)
    });
  }

  chargerPoubelles(): void {
    this.loading = true;
    this.errorMessage = '';

    this.binService.getBins().subscribe({
      next: (data: BinStatusDto[]) => {
        this.poubelles = (data || []).map(item => this.mapBinToPoubelle(item));
        this.loading = false;
      },
      error: (error) => {
        console.error(error);
        this.errorMessage = 'Erreur chargement';
        this.loading = false;
      }
    });
  }

  openAddModal(): void {
    this.showAddModal = true;
    const today = new Date().toISOString().split('T')[0];

    this.newBin = {
      binCode: '',
      type: '',
      zoneId: null,
      lat: null,
      lng: null,
      installationDate: today,
      isActive: true,
      notes: ''
    };

    setTimeout(() => this.initAddMap(), 150);
  }

  closeAddModal(): void {
    this.showAddModal = false;
    this.destroyAddMap();
  }

  initAddMap(): void {
    this.destroyAddMap();

    this.addMap = L.map('addMap').setView([48.845, 2.29], 14);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: ''
    }).addTo(this.addMap);

    this.showExistingBinsOnMap(this.addMap);

    this.addMap.on('click', (e: L.LeafletMouseEvent) => {
      const lat = Number(e.latlng.lat.toFixed(6));
      const lng = Number(e.latlng.lng.toFixed(6));
      this.applyMapClick(lat, lng, 'add');
    });

    setTimeout(() => this.addMap?.invalidateSize(), 200);
  }

  destroyAddMap(): void {
    if (this.addMap) {
      this.clearExistingBinMarkers(this.addMap);
      this.addMap.remove();
      this.addMap = null;
      this.addMarker = null;
    }
  }

  initEditMap(): void {
    this.destroyEditMap();

    const lat = this.editForm.lat ?? 48.845;
    const lng = this.editForm.lng ?? 2.29;

    this.editMap = L.map('editMap').setView([lat, lng], 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: ''
    }).addTo(this.editMap);

    this.showExistingBinsOnMap(this.editMap, this.editingBin?.id);

    if (this.editForm.lat !== null && this.editForm.lng !== null) {
      this.editMarker = L.marker([lat, lng]).addTo(this.editMap);
    }

    this.editMap.on('click', (e: L.LeafletMouseEvent) => {
      const newLat = Number(e.latlng.lat.toFixed(6));
      const newLng = Number(e.latlng.lng.toFixed(6));
      this.applyMapClick(newLat, newLng, 'edit');
    });

    setTimeout(() => this.editMap?.invalidateSize(), 200);
  }

  destroyEditMap(): void {
    if (this.editMap) {
      this.clearExistingBinMarkers(this.editMap);
      this.editMap.remove();
      this.editMap = null;
      this.editMarker = null;
    }
  }

  private clearExistingBinMarkers(map: L.Map): void {
    this.existingBinMarkers.forEach(marker => map.removeLayer(marker));
    this.existingBinMarkers = [];
  }

  private showExistingBinsOnMap(map: L.Map, excludeBinId?: number): void {
    this.clearExistingBinMarkers(map);

    this.poubelles.forEach(bin => {
      if (bin.lat == null || bin.lng == null) return;
      if (excludeBinId && bin.id === excludeBinId) return;

      const marker = L.marker([Number(bin.lat), Number(bin.lng)]).addTo(map);
      marker.bindPopup(`Poubelle: ${bin.code}`);
      this.existingBinMarkers.push(marker);
    });
  }

  private isPositionAlreadyUsed(lat: number, lng: number, excludeBinId?: number): boolean {
    return this.poubelles.some(bin => {
      if (bin.lat == null || bin.lng == null) return false;
      if (excludeBinId && bin.id === excludeBinId) return false;

      return Number(bin.lat).toFixed(6) === Number(lat).toFixed(6)
          && Number(bin.lng).toFixed(6) === Number(lng).toFixed(6);
    });
  }

  private applyMapClick(lat: number, lng: number, mode: 'add' | 'edit'): void {
    if (!this.isInsideParis15(lat, lng)) {
      alert('Vous n’avez pas le droit de sélectionner une position hors Paris 15.');
      return;
    }

    const excludeId = mode === 'edit' ? this.editingBin?.id : undefined;

    if (this.isPositionAlreadyUsed(lat, lng, excludeId)) {
      alert('Cette position contient déjà une poubelle.');
      return;
    }

    const zone = this.findNearestZone(lat, lng);

    if (!zone) {
      alert('Aucune zone valide trouvée pour cette position.');
      return;
    }

    const zoneId = Number(zone.id);

    if (mode === 'add') {
      this.newBin.lat = lat;
      this.newBin.lng = lng;
      this.newBin.zoneId = zoneId;

      if (this.addMarker) this.addMarker.setLatLng([lat, lng]);
      else this.addMarker = L.marker([lat, lng]).addTo(this.addMap!);
    }

    if (mode === 'edit') {
      this.editForm.lat = lat;
      this.editForm.lng = lng;
      this.editForm.zoneId = zoneId;

      if (this.editMarker) this.editMarker.setLatLng([lat, lng]);
      else this.editMarker = L.marker([lat, lng]).addTo(this.editMap!);
    }
  }

private isInsideParis15(lat: number, lng: number): boolean {
  return lat >= 48.82 && lat <= 48.87 && lng >= 2.25 && lng <= 2.33;
}

  private getZoneCenter(zone: any): { lat: number; lng: number } | null {
    const lat = zone.centerLat ?? zone.center_lat ?? zone.lat;
    const lng = zone.centerLng ?? zone.center_lng ?? zone.lng;

    if (lat == null || lng == null) return null;
    return { lat: Number(lat), lng: Number(lng) };
  }

  private distanceMeters(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;

    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) *
      Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  private findNearestZone(lat: number, lng: number): any | null {
    let nearestZone: any | null = null;
    let nearestDistance = Infinity;

    for (const zone of this.zones) {
      const center = this.getZoneCenter(zone);
      if (!center) continue;

      const distance = this.distanceMeters(lat, lng, center.lat, center.lng);

      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearestZone = zone;
      }
    }

    if (nearestDistance > 1200) return null;
    return nearestZone;
  }

  zoomToZone(zoneId: number | null, mode: 'add' | 'edit'): void {
    if (!zoneId) return;

    const zone = this.zones.find(z => Number(z.id) === Number(zoneId));
    if (!zone) return;

    const center = this.getZoneCenter(zone);
    if (!center) return;

    if (mode === 'add' && this.addMap) this.addMap.setView([center.lat, center.lng], 16);
    if (mode === 'edit' && this.editMap) this.editMap.setView([center.lat, center.lng], 16);
  }

  addBin(): void {
    const error = this.validateBin(this.newBin, true, true);
    if (error) {
      alert(error);
      return;
    }

    const wasteType = this.mapWasteType(this.newBin.type);

    const payload = {
      binCode: this.newBin.binCode?.trim() || null,
      type: 'REAL',
      wasteType,
      zoneId: Number(this.newBin.zoneId),
      lat: Number(this.newBin.lat),
      lng: Number(this.newBin.lng),
      accessLat: Number(this.newBin.lat),
      accessLng: Number(this.newBin.lng),
      installationDate: this.newBin.installationDate,
      isActive: this.newBin.isActive,
      notes: this.newBin.notes
    };

    this.addLoading = true;

    this.http.post(this.API, payload).subscribe({
      next: () => {
        this.addLoading = false;
        this.closeAddModal();
        this.chargerPoubelles();
        alert('Ajout réussi');
      },
      error: (e) => {
        console.error(e);
        this.addLoading = false;
        const msg = e?.error?.message || e?.error || '';

        if (msg.includes('bin_code already exists')) {
          alert('Ce code de poubelle existe déjà');
        } else {
          alert('Erreur lors de l’ajout');
        }
      }
    });
  }

  editBin(p: Poubelle): void {
    this.editingBin = p;
    this.menuOpenId = null;

    this.editOriginal = {
      zoneId: p.zoneId ?? null,
      lat: p.lat ?? null,
      lng: p.lng ?? null,
      wasteType: p.wasteType ?? '',
      isActive: p.isActive ?? true,
      notes: p.notes ?? ''
    };

    this.editForm = {
      type: this.mapWasteTypeToFr(p.wasteType ?? ''),
      zoneId: p.zoneId ?? null,
      lat: p.lat ?? null,
      lng: p.lng ?? null,
      isActive: p.isActive ?? true,
      notes: p.notes ?? ''
    };

    this.showEditModal = true;
    setTimeout(() => this.initEditMap(), 150);
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingBin = null;
    this.destroyEditMap();
  }

  hasEditChanges(): boolean {
    if (!this.editingBin) return false;

    const currentWasteType = this.editForm.type?.trim()
      ? this.mapWasteType(this.editForm.type)
      : this.editOriginal.wasteType;

    return (
      Number(this.editForm.zoneId) !== Number(this.editOriginal.zoneId) ||
      Number(this.editForm.lat) !== Number(this.editOriginal.lat) ||
      Number(this.editForm.lng) !== Number(this.editOriginal.lng) ||
      currentWasteType !== this.editOriginal.wasteType ||
      this.editForm.isActive !== this.editOriginal.isActive ||
      (this.editForm.notes ?? '') !== (this.editOriginal.notes ?? '')
    );
  }

  updateBin(): void {
    if (!this.editingBin?.id) return;

    if (!this.hasEditChanges()) {
      alert('Veuillez modifier au moins une information.');
      return;
    }

    const error = this.validateBin(this.editForm, false, false);
    if (error) {
      alert(error);
      return;
    }

    const finalWasteType = this.editForm.type?.trim()
      ? this.mapWasteType(this.editForm.type)
      : this.editOriginal.wasteType;

    const payload = {
      binCode: this.editingBin.code,
      type: 'REAL',
      wasteType: finalWasteType,
      zoneId: Number(this.editForm.zoneId),
      lat: Number(this.editForm.lat),
      lng: Number(this.editForm.lng),
      accessLat: Number(this.editForm.lat),
      accessLng: Number(this.editForm.lng),
      isActive: this.editForm.isActive,
      notes: this.editForm.notes
    };

    this.editLoading = true;

    this.http.put(`${this.API}/${this.editingBin.id}`, payload).subscribe({
      next: () => {
        this.editLoading = false;
        this.closeEditModal();
        this.chargerPoubelles();
        alert('Modification réussie');
      },
      error: (e) => {
        console.error(e);
        this.editLoading = false;
        const msg = e?.error?.message || e?.error || '';

        if (msg.includes('bin_code already exists')) {
          alert('Ce code de poubelle existe déjà');
        } else {
          alert('Erreur lors de la modification');
        }
      }
    });
  }

  deleteBin(id?: number): void {
    if (!id) return;

    if (!confirm('Voulez-vous vraiment supprimer cette poubelle ?')) return;

    this.http.delete(`${this.API}/${id}`).subscribe({
      next: () => {
        this.chargerPoubelles();
        this.menuOpenId = null;
        alert('Poubelle supprimée');
      },
      error: (e) => {
        console.error(e);
        alert(e?.error?.message || e?.error || 'Erreur suppression');
      }
    });
  }

  printQR(p: Poubelle): void {
    if (!p.id) return;

    this.http.get(`${this.API}/${p.id}/qrcode`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const reader = new FileReader();
        reader.onloadend = () => {
          const base64 = reader.result as string;
          this.printQrBase64(base64, p.code);
        };
        reader.readAsDataURL(blob);
      },
      error: (error) => {
        console.error('Erreur récupération QR', error);
        alert('Erreur lors de la récupération du QR code.');
      }
    });
  }

  regenerateQR(p: Poubelle): void {
    if (!p.id) return;

    this.qrLoading = true;
    this.selectedBin = p;

    this.http.post(`${this.API}/${p.id}/qrcode/regenerate`, {}, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        if (this.qrPreviewUrl) URL.revokeObjectURL(this.qrPreviewUrl);

        this.qrPreviewUrl = URL.createObjectURL(blob);
        this.selectedBin = p;
        this.showQrModal = true;
        this.qrLoading = false;
      },
      error: (error) => {
        console.error('Erreur régénération QR', error);
        this.qrLoading = false;
        alert('Erreur lors de la régénération du QR code.');
      }
    });
  }

  printQrBase64(base64: string, code: string): void {
    const printWindow = window.open('', '_blank', 'width=600,height=700');

    if (!printWindow) {
      alert('Veuillez autoriser les pop-ups pour imprimer le QR code.');
      return;
    }

    printWindow.document.open();
    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>QR Code - ${code}</title>
          <style>
            body {
              margin: 0;
              padding: 40px;
              font-family: Arial, sans-serif;
              display: flex;
              justify-content: center;
              align-items: center;
              min-height: 100vh;
              background: white;
            }
            .qr-card {
              border: 2px solid #111827;
              border-radius: 12px;
              padding: 24px;
              text-align: center;
            }
            h2 { margin: 0 0 16px; font-size: 24px; color: #111827; }
            img { width: 300px; height: 300px; }
            .code {
              margin-top: 16px;
              font-size: 18px;
              font-weight: bold;
              color: #111827;
            }
          </style>
        </head>
        <body>
          <div class="qr-card">
            <h2>Wise Trash</h2>
            <img src="${base64}" alt="QR Code ${code}" />
            <div class="code">${code}</div>
          </div>
          <script>
            window.onload = function() {
              window.focus();
              window.print();
            };
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  }

  downloadQR(): void {
    if (!this.qrPreviewUrl || !this.selectedBin) return;

    const a = document.createElement('a');
    a.href = this.qrPreviewUrl;
    a.download = `qr-${this.selectedBin.code}.png`;
    a.click();
  }

  printFromModal(): void {
    if (this.selectedBin) this.printQR(this.selectedBin);
  }

  closeQrModal(): void {
    this.showQrModal = false;

    if (this.qrPreviewUrl) {
      URL.revokeObjectURL(this.qrPreviewUrl);
      this.qrPreviewUrl = '';
    }

    this.selectedBin = null;
  }

  toggleMenu(id?: number): void {
    if (!id) return;
    this.menuOpenId = this.menuOpenId === id ? null : id;
  }

  closeMenu(): void {
    this.menuOpenId = null;
  }

  private validateBin(form: any, checkDate: boolean, requireType: boolean): string | null {
    const lat = Number(form.lat);
    const lng = Number(form.lng);
    const zoneId = Number(form.zoneId);
    const excludeId = this.showEditModal ? this.editingBin?.id : undefined;

    if (form.lat === null || form.lat === undefined || form.lat === '') return 'Latitude obligatoire';
    if (form.lng === null || form.lng === undefined || form.lng === '') return 'Longitude obligatoire';
    if (isNaN(lat) || isNaN(lng)) return 'Latitude et longitude doivent être des nombres';

    if (!this.isInsideParis15(lat, lng)) return 'La position doit être dans Paris 15';

    if (this.isPositionAlreadyUsed(lat, lng, excludeId)) {
      return 'Cette position contient déjà une poubelle';
    }

    if (form.zoneId === null || form.zoneId === undefined || form.zoneId === '') return 'Zone obligatoire';
    if (isNaN(zoneId) || zoneId <= 0) return 'Zone invalide';
    if (!this.zoneIds.includes(zoneId)) return 'Zone n’existe pas dans la base de données';

    if (requireType || form.type?.trim()) {
      const wasteType = this.mapWasteType(form.type);
      const validTypes = ['GRAY', 'GREEN', 'YELLOW', 'WHITE'];

      if (!validTypes.includes(wasteType)) {
        return 'Type déchet invalide. Utilisez: gris, vert, jaune ou blanc';
      }
    }

    if (checkDate && form.installationDate) {
      const d = new Date(form.installationDate);
      const today = new Date();
      today.setHours(23, 59, 59, 999);

      if (d > today) return 'Date installation ne peut pas être dans le futur';
    }

    return null;
  }

  private mapWasteType(value: string): string {
    const wasteTypeFr = value?.trim().toLowerCase();

    const map: Record<string, string> = {
      gris: 'GRAY',
      grise: 'GRAY',
      vert: 'GREEN',
      verte: 'GREEN',
      jaune: 'YELLOW',
      blanc: 'WHITE',
      blanche: 'WHITE'
    };

    return map[wasteTypeFr] || value?.trim().toUpperCase();
  }

  private mapWasteTypeToFr(value: string): string {
    const map: Record<string, string> = {
      GRAY: 'gris',
      GREEN: 'vert',
      YELLOW: 'jaune',
      WHITE: 'blanc'
    };

    return map[value] || value;
  }

  private mapBinToPoubelle(item: BinStatusDto): Poubelle {
    const raw: any = item;
    const niveau = item.fillLevel ?? 0;

    return {
      id: item.id,
      nom: item.zoneName || '',
      zone: item.zoneName || 'Zone inconnue',
      code: item.binCode || '',
      statut: this.mapStatut(item.status, niveau, item.isActive),
      niveauRemplissage: niveau,
      capacite: '1100L',
      batterie: item.batteryLevel ?? 100,
      signal: item.rssi ?? 0,
      temperature: '',
      derniereCollecte: item.lastTelemetryAt ? this.formatDate(item.lastTelemetryAt) : '--',
      prochaineCollecte: this.calculerProchaineCollecte(item.lastTelemetryAt),
      zoneId: raw.zoneId ?? null,
      lat: raw.lat ?? null,
      lng: raw.lng ?? null,
      wasteType: raw.wasteType ?? null,
      isActive: raw.isActive ?? true,
      notes: raw.notes ?? ''
    };
  }

  private mapStatut(status?: string, fillLevel?: number, isActive?: boolean): StatutPoubelle {
    if (isActive === false) return 'Maintenance';

    const s = (status ?? '').toUpperCase();

    if (s.includes('MAINTENANCE') || s.includes('OFFLINE') || s.includes('ERROR')) {
      return 'Maintenance';
    }

    if ((fillLevel ?? 0) >= 80) return 'Full';

    return 'Active';
  }

  private formatDate(dateValue: string): string {
    const date = new Date(dateValue);
    return isNaN(date.getTime()) ? '--' : date.toLocaleDateString('fr-FR');
  }

  private calculerProchaineCollecte(lastTelemetryAt?: string): string {
    if (!lastTelemetryAt) return '--';

    const date = new Date(lastTelemetryAt);
    if (isNaN(date.getTime())) return '--';

    date.setDate(date.getDate() + 3);
    return date.toLocaleDateString('fr-FR');
  }

  selectionnerFiltre(filtre: FiltrePoubelle): void {
    this.filtreSelectionne = filtre;
  }

  get nombreTotalPoubelles(): number {
    return this.poubelles.length;
  }

  get nombrePoubellesPleines(): number {
    return this.poubelles.filter(p => p.statut === 'Full').length;
  }

  get nombrePoubellesActives(): number {
    return this.poubelles.filter(p => p.statut === 'Active').length;
  }

  get nombrePoubellesMaintenance(): number {
    return this.poubelles.filter(p => p.statut === 'Maintenance').length;
  }

  get poubellesFiltrees(): Poubelle[] {
    let res = [...this.poubelles];

    if (this.filtreSelectionne === 'Actives') {
      res = res.filter(p => p.statut === 'Active');
    }

    if (this.filtreSelectionne === 'Pleines') {
      res = res.filter(p => p.statut === 'Full');
    }

    if (this.filtreSelectionne === 'Maintenance') {
      res = res.filter(p => p.statut === 'Maintenance');
    }

    const t = this.termeRecherche.trim().toLowerCase();

    if (t) {
      res = res.filter(p =>
        p.zone.toLowerCase().includes(t) ||
        p.code.toLowerCase().includes(t)
      );
    }

    return res;
  }

  getClasseStatut(statut: StatutPoubelle): string {
    if (statut === 'Active') return 'statut-actif';
    if (statut === 'Full') return 'statut-plein';
    return 'statut-maintenance';
  }

  getLibelleStatut(statut: StatutPoubelle): string {
    if (statut === 'Active') return 'Active';
    if (statut === 'Full') return 'Pleine';
    return 'Maintenance';
  }
// ===== DETAILS MODAL =====
showDetailsModal = false;
detailsBin: Poubelle | null = null;

showDetails(p: Poubelle): void {
  this.detailsBin = p;
  this.showDetailsModal = true;
  this.menuOpenId = null;
}

closeDetailsModal(): void {
  this.showDetailsModal = false;
  this.detailsBin = null;
}
  getClasseRemplissage(niveau: number): string {
    if (niveau >= 80) return 'fill-high';
    if (niveau >= 50) return 'fill-medium';
    return 'fill-low';
  }
  
}