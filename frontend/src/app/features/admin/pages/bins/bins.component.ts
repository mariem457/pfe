import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { BinService } from '../../../../services/bin.service';
import { MapFocusService } from '../../../../services/map-focus.service';

type BinStatus = 'Plein' | 'Partiel' | 'Vide' | 'Maintenance';
type ActivityFilter = 'All' | 'Actif' | 'Inactif';
type BatteryFilter = 'All' | 'Low' | 'Normal';
type SortOption =
  | 'fill_desc'
  | 'fill_asc'
  | 'battery_desc'
  | 'battery_asc'
  | 'id_asc';

interface BinRow {
  id: string;
  location: string;
  zone: string;
  wasteType: string;
  type: string;
  fill: number;
  status: BinStatus;
  battery: number;
  lastTelemetry: string;
  isActive: boolean;
  lat: number | null;
  lng: number | null;
}

interface BinFormModel {
  id: string;
  location: string;
  zone: string;
  fill: number | null;
  battery: number | null;
  status: BinStatus;
  isActive: boolean;
  type: string;
  wasteType: string;
  lat: number | null;
  lng: number | null;
}

type ModalMode = 'create' | 'edit' | 'view';

@Component({
  selector: 'app-bins',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bins.component.html',
  styleUrls: ['./bins.component.css'],
})
export class BinsComponent implements OnInit {
  constructor(
    private binService: BinService,
    private router: Router,
    private route: ActivatedRoute,
    private mapFocusService: MapFocusService
  ) {}

  stats = {
    total: 0,
    full: 0,
    partial: 0,
    empty: 0,
    maintenance: 0,
    active: 0,
    inactive: 0
  };

  loading = true;
  error = '';
  lastUpdatedLabel = '—';

  query = '';
  statusFilter: 'All' | BinStatus = 'All';
  activityFilter: ActivityFilter = 'All';
  batteryFilter: BatteryFilter = 'All';
  sortBy: SortOption = 'fill_desc';

  rows: BinRow[] = [];

  showFormModal = false;
  modalMode: ModalMode = 'create';
  selectedBinId: string | null = null;
  formError = '';
  form: BinFormModel = this.createEmptyForm();

  ngOnInit(): void {
    this.reloadBins();
    this.consumePickedLocationFromQuery();
  }

  reloadBins(): void {
    this.loading = true;
    this.error = '';

    this.binService.getBins().subscribe({
      next: (bins: any[]) => {
        this.rows = (bins || []).map((b: any) => {
          const fill = this.clampPercent(b.fillLevel ?? 0);
          const battery = this.clampPercent(b.batteryLevel ?? 0);
          const mappedStatus = this.mapStatus(b.status, fill);

          const rawLat = b.lat ?? b.latitude ?? null;
          const rawLng = b.lng ?? b.longitude ?? null;

          const lat = rawLat !== null ? Number(rawLat) : null;
          const lng = rawLng !== null ? Number(rawLng) : null;

          return {
            id: b.binCode ?? b.bin_code ?? String(b.binId ?? b.id ?? '—'),
            location: b.address ?? b.location ?? `(${lat ?? '-'}, ${lng ?? '-'})`,
            zone: b.zoneName ?? b.zone?.name ?? 'Zone non définie',
            wasteType: b.wasteType ?? 'GRAY',
            type: b.type ?? 'SIM',
            fill,
            status: mappedStatus,
            battery,
            lastTelemetry: b.timestamp ? this.formatDate(b.timestamp) : '-',
            isActive: b.active ?? b.isActive ?? true,
            lat: lat !== null && !Number.isNaN(lat) ? lat : null,
            lng: lng !== null && !Number.isNaN(lng) ? lng : null
          };
        });

        this.recomputeStats();
        this.loading = false;
        this.updateLastRefresh();
      },
      error: (err) => {
        console.error('GET /api/bins failed', err);
        this.error = 'Impossible de charger les bacs.';
        this.loading = false;
      }
    });
  }

  openCreateModal(): void {
    this.modalMode = 'create';
    this.selectedBinId = null;
    this.formError = '';
    this.form = this.createEmptyForm();
    this.showFormModal = true;
  }

  openEditModal(row: BinRow): void {
    this.modalMode = 'edit';
    this.selectedBinId = row.id;
    this.formError = '';
    this.form = {
      id: row.id,
      location: row.location,
      zone: row.zone,
      fill: row.fill,
      battery: row.battery,
      status: row.status,
      isActive: row.isActive,
      type: row.type || 'SIM',
      wasteType: row.wasteType || 'GRAY',
      lat: row.lat,
      lng: row.lng
    };
    this.showFormModal = true;
  }

  openViewModal(row: BinRow): void {
    this.modalMode = 'view';
    this.selectedBinId = row.id;
    this.formError = '';
    this.form = {
      id: row.id,
      location: row.location,
      zone: row.zone,
      fill: row.fill,
      battery: row.battery,
      status: row.status,
      isActive: row.isActive,
      type: row.type || 'SIM',
      wasteType: row.wasteType || 'GRAY',
      lat: row.lat,
      lng: row.lng
    };
    this.showFormModal = true;
  }

  closeFormModal(): void {
    this.showFormModal = false;
    this.formError = '';
    this.selectedBinId = null;
  }

  saveBin(): void {
    if (this.modalMode === 'view') return;

    this.formError = '';

    const id = this.form.id?.trim();
    const location = this.form.location?.trim();

    if (!location) {
      this.formError = 'La localisation est obligatoire.';
      return;
    }

    if (this.form.lat == null || this.form.lng == null) {
      const match = location.match(/\(([^,]+),\s*([^)]+)\)/);

      if (!match) {
        this.formError = 'Coordonnées invalides.';
        return;
      }

      const lat = parseFloat(match[1]);
      const lng = parseFloat(match[2]);

      if (Number.isNaN(lat) || Number.isNaN(lng)) {
        this.formError = 'Coordonnées invalides.';
        return;
      }

      this.form.lat = lat;
      this.form.lng = lng;
    }

    if (!this.form.type) {
      this.formError = 'Le type du bac est obligatoire.';
      return;
    }

    if (!this.form.wasteType) {
      this.formError = 'Le type de déchet est obligatoire.';
      return;
    }

    const payload: any = {
      type: this.form.type,
      wasteType: this.form.wasteType,
      lat: this.form.lat,
      lng: this.form.lng,
      isActive: this.form.isActive,
      notes: null
    };

    if (id) {
      payload.binCode = id;
    }

    if (this.modalMode === 'create') {
      if (id) {
        const exists = this.rows.some(r => r.id.toLowerCase() === id.toLowerCase());
        if (exists) {
          this.formError = 'Un bac avec ce code existe déjà.';
          return;
        }
      }

      this.binService.createBin(payload).subscribe({
        next: () => {
          this.reloadBins();
          this.closeFormModal();
        },
        error: (err) => {
          console.error('CREATE BIN FAILED', err);
          this.formError =
            err?.error?.message ||
            err?.error?.error ||
            'Erreur lors de la création du bac.';
        }
      });

      return;
    }

    const existing = this.rows.find(r => r.id === this.selectedBinId);
    if (!existing) {
      this.formError = 'Bac introuvable pour modification.';
      return;
    }

    this.binService.getBins().subscribe({
      next: (bins: any[]) => {
        const backendBin = (bins || []).find((b: any) =>
          (b.binCode ?? b.bin_code ?? String(b.id)) === existing.id
        );

        if (!backendBin?.id) {
          this.formError = 'Impossible de retrouver le bac côté serveur.';
          return;
        }

        this.binService.updateBin(backendBin.id, payload).subscribe({
          next: () => {
            this.reloadBins();
            this.closeFormModal();
          },
          error: (err) => {
            console.error('UPDATE BIN FAILED', err);
            this.formError =
              err?.error?.message ||
              err?.error?.error ||
              'Erreur lors de la modification du bac.';
          }
        });
      },
      error: () => {
        this.formError = 'Impossible de charger les bacs pour la modification.';
      }
    });
  }

  toggleActivation(row: BinRow): void {
    this.binService.getBins().subscribe({
      next: (bins: any[]) => {
        const backendBin = (bins || []).find((b: any) =>
          (b.binCode ?? b.bin_code ?? String(b.id)) === row.id
        );

        if (!backendBin?.id) return;

        this.binService.updateBin(backendBin.id, {
          isActive: !row.isActive
        }).subscribe({
          next: () => this.reloadBins(),
          error: (err) => console.error('TOGGLE ACTIVE FAILED', err)
        });
      },
      error: (err) => {
        console.error('LOAD BINS FOR TOGGLE FAILED', err);
      }
    });
  }

  openMap(row: BinRow): void {
    if (
      row.lat === null ||
      row.lng === null ||
      Number.isNaN(row.lat) ||
      Number.isNaN(row.lng)
    ) {
      alert('Cette poubelle ne possède pas de coordonnées valides.');
      return;
    }

    this.mapFocusService.setTarget({
      type: 'bin',
      id: row.id,
      lat: row.lat,
      lng: row.lng,
      code: row.id,
      zone: row.zone
    });

    this.router.navigate(['../dashboard'], { relativeTo: this.route });
  }

  goPickLocationOnMap(): void {
    this.router.navigate(['../dashboard'], {
      relativeTo: this.route,
      queryParams: {
        pickBinLocation: 1,
        returnToBins: 1
      }
    });
  }

  get filteredRows(): BinRow[] {
    const q = this.query.trim().toLowerCase();

    let data = this.rows.filter(r => {
      const matchQuery =
        !q ||
        r.id.toLowerCase().includes(q) ||
        r.zone.toLowerCase().includes(q) ||
        r.wasteType.toLowerCase().includes(q);

      const matchStatus =
        this.statusFilter === 'All' ? true : r.status === this.statusFilter;

      const matchActivity =
        this.activityFilter === 'All'
          ? true
          : this.activityFilter === 'Actif'
            ? r.isActive
            : !r.isActive;

      const matchBattery =
        this.batteryFilter === 'All'
          ? true
          : this.batteryFilter === 'Low'
            ? r.battery < 20
            : r.battery >= 20;

      return matchQuery && matchStatus && matchActivity && matchBattery;
    });

    data = [...data].sort((a, b) => {
      switch (this.sortBy) {
        case 'fill_asc':
          return a.fill - b.fill;
        case 'fill_desc':
          return b.fill - a.fill;
        case 'battery_asc':
          return a.battery - b.battery;
        case 'battery_desc':
          return b.battery - a.battery;
        case 'id_asc':
          return a.id.localeCompare(b.id);
        default:
          return b.fill - a.fill;
      }
    });

    return data;
  }

  resetFilters(): void {
    this.query = '';
    this.statusFilter = 'All';
    this.activityFilter = 'All';
    this.batteryFilter = 'All';
    this.sortBy = 'fill_desc';
  }

  fillClass(fill: number, status: BinStatus): string {
    if (status === 'Maintenance') return 'is-gray';
    if (fill >= 85) return 'is-red';
    if (fill >= 50) return 'is-orange';
    return 'is-green';
  }

  batteryClass(battery: number): string {
    if (battery < 20) return 'is-low';
    if (battery < 50) return 'is-medium';
    return 'is-good';
  }

  activityLabel(isActive: boolean): string {
    return isActive ? 'Actif' : 'Inactif';
  }

  exportCsv(): void {
    const headers = [
      'Code du bac',
      'Zone',
      'Type de déchet',
      'Niveau de remplissage',
      'État opérationnel',
      'État administratif',
      'Batterie',
      'Dernière télémétrie'
    ];

    const lines = this.filteredRows.map(r =>
      [
        r.id,
        r.zone,
        r.wasteType,
        `${r.fill}%`,
        r.status,
        this.activityLabel(r.isActive),
        `${r.battery}%`,
        r.lastTelemetry
      ]
        .map(v => `"${String(v).replace(/"/g, '""')}"`)
        .join(',')
    );

    const csv = [headers.join(','), ...lines].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = 'gestion-bacs-intelligents.csv';
    a.click();

    URL.revokeObjectURL(url);
  }

  trackByBin(_: number, row: BinRow): string {
    return row.id;
  }

  private recomputeStats(): void {
    this.stats.total = this.rows.length;
    this.stats.full = this.rows.filter(r => r.status === 'Plein').length;
    this.stats.partial = this.rows.filter(r => r.status === 'Partiel').length;
    this.stats.empty = this.rows.filter(r => r.status === 'Vide').length;
    this.stats.maintenance = this.rows.filter(r => r.status === 'Maintenance').length;
    this.stats.active = this.rows.filter(r => r.isActive).length;
    this.stats.inactive = this.rows.filter(r => !r.isActive).length;
  }

  private createEmptyForm(): BinFormModel {
    return {
      id: '',
      location: '',
      zone: '',
      fill: 0,
      battery: 100,
      status: 'Vide',
      isActive: true,
      type: 'SIM',
      wasteType: 'GRAY',
      lat: null,
      lng: null
    };
  }

  private updateLastRefresh(): void {
    this.lastUpdatedLabel = new Date().toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  private formatDate(ts: any): string {
    try {
      return new Date(ts).toLocaleString('fr-FR');
    } catch {
      return String(ts);
    }
  }

  private clampPercent(value: any): number {
    const n = Number(value ?? 0);
    if (Number.isNaN(n)) return 0;
    return Math.max(0, Math.min(100, Math.round(n)));
  }

  private mapStatus(statusFromApi: string, fill: number): BinStatus {
    const s = (statusFromApi || '').toUpperCase();

    if (s === 'ERROR') return 'Maintenance';
    if (s === 'FULL' || s === 'OVERFLOW') return 'Plein';
    if (s === 'WARNING') return 'Partiel';

    if (fill >= 85) return 'Plein';
    if (fill >= 50) return 'Partiel';
    return 'Vide';
  }

  private consumePickedLocationFromQuery(): void {
    this.route.queryParamMap.subscribe(params => {
      const pickedLat = params.get('pickedLat');
      const pickedLng = params.get('pickedLng');
      const returnToBins = params.get('returnToBins');

      if (!returnToBins || pickedLat == null || pickedLng == null) {
        return;
      }

      const lat = Number(pickedLat);
      const lng = Number(pickedLng);

      if (Number.isNaN(lat) || Number.isNaN(lng)) {
        return;
      }

      this.openCreateModal();
      this.form.lat = lat;
      this.form.lng = lng;
      this.form.location = `(${lat.toFixed(6)}, ${lng.toFixed(6)})`;

      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {
          pickedLat: null,
          pickedLng: null,
          returnToBins: null
        },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    });
  }
}