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

  showDeleteModal = false;
  deleteTarget: BinRow | null = null;

  ngOnInit(): void {
    this.reloadBins();
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
        console.error('GET /api/bins/status failed', err);
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
      isActive: row.isActive
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
      isActive: row.isActive
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

    const id = this.form.id.trim();
    const location = this.form.location.trim();
    const zone = this.form.zone.trim();

    if (!id) {
      this.formError = 'Le code du bac est obligatoire.';
      return;
    }

    if (!location) {
      this.formError = 'La localisation est obligatoire.';
      return;
    }

    if (!zone) {
      this.formError = 'La zone est obligatoire.';
      return;
    }

    const fill = this.clampPercent(this.form.fill ?? 0);
    const battery = this.clampPercent(this.form.battery ?? 0);

    const row: BinRow = {
      id,
      location,
      zone,
      fill,
      battery,
      status: this.form.status,
      isActive: this.form.isActive,
      lastTelemetry: this.formatDate(new Date()),
      lat: null,
      lng: null
    };

    if (this.modalMode === 'create') {
      const exists = this.rows.some(r => r.id.toLowerCase() === id.toLowerCase());

      if (exists) {
        this.formError = 'Un bac avec ce code existe déjà.';
        return;
      }

      this.rows = [row, ...this.rows];
    } else {
      this.rows = this.rows.map(r =>
        r.id === this.selectedBinId
          ? {
              ...r,
              ...row,
              lat: r.lat,
              lng: r.lng
            }
          : r
      );
    }

    this.recomputeStats();
    this.updateLastRefresh();
    this.closeFormModal();
  }

  confirmDelete(row: BinRow): void {
    this.deleteTarget = row;
    this.showDeleteModal = true;
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.deleteTarget = null;
  }

  deleteBin(): void {
    const target = this.deleteTarget;
    if (!target) return;

    this.rows = this.rows.filter(r => r.id !== target.id);
    this.recomputeStats();
    this.updateLastRefresh();
    this.closeDeleteModal();
  }

  toggleActivation(row: BinRow): void {
    this.rows = this.rows.map(r =>
      r.id === row.id
        ? {
            ...r,
            isActive: !r.isActive,
            lastTelemetry: this.formatDate(new Date())
          }
        : r
    );

    this.recomputeStats();
    this.updateLastRefresh();
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
      queryParams: { pickBinLocation: 1 }
    });
  }

  get filteredRows(): BinRow[] {
    const q = this.query.trim().toLowerCase();

    let data = this.rows.filter(r => {
      const matchQuery =
        !q ||
        r.id.toLowerCase().includes(q) ||
        r.zone.toLowerCase().includes(q);

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
      isActive: true
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
}