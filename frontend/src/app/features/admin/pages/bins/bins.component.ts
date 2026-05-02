import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

import { BinService } from '../../../../services/bin.service';
import { MapFocusService } from '../../../../services/map-focus.service';
import { AlertService, AlertDto } from '../../../../services/alert.service';

type BinStatus = 'Plein' | 'Partiel' | 'Vide' | 'Maintenance';
type ActivityFilter = 'All' | 'Actif' | 'Inactif';
type BatteryFilter = 'All' | 'Low' | 'Normal';
type AlertFilter = 'All' | 'WithAlerts' | 'WithoutAlerts' | 'Critical';

type SortOption =
  | 'fill_desc'
  | 'fill_asc'
  | 'battery_desc'
  | 'battery_asc'
  | 'alerts_desc'
  | 'id_asc';

interface BinRow {
  backendId: number | null;
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

  alertsCount: number;
  topAlertSeverity: string | null;
  topAlertType: string | null;
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
    private alertService: AlertService,
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
    inactive: 0,
    withAlerts: 0,
    criticalAlerts: 0
  };

  loading = true;
  loadingAlerts = false;
  error = '';
  alertsError = '';
  lastUpdatedLabel = '—';

  query = '';
  statusFilter: 'All' | BinStatus = 'All';
  activityFilter: ActivityFilter = 'All';
  batteryFilter: BatteryFilter = 'All';
  alertFilter: AlertFilter = 'All';
  sortBy: SortOption = 'fill_desc';

  rows: BinRow[] = [];

  binAlerts: AlertDto[] = [];
  alertsByBinCode = new Map<string, AlertDto[]>();
  alertsByBinId = new Map<number, AlertDto[]>();

  showFormModal = false;
  modalMode: ModalMode = 'create';
  selectedBinId: string | null = null;
  formError = '';
  form: BinFormModel = this.createEmptyForm();

  showAlertsDrawer = false;
  selectedAlertBin: BinRow | null = null;
  resolvingAlertId: number | null = null;

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

          const backendId =
            b.id !== undefined && b.id !== null && !Number.isNaN(Number(b.id))
              ? Number(b.id)
              : null;

          return {
            backendId,
            id: b.binCode ?? b.bin_code ?? String(b.binId ?? b.id ?? '—'),
            location: b.address ?? b.location ?? `(${lat ?? '-'}, ${lng ?? '-'})`,
            zone: b.zoneName ?? b.zone?.name ?? 'Zone non définie',
            wasteType: b.wasteType ?? 'GRAY',
            type: b.type ?? 'SIM',
            fill,
            status: mappedStatus,
            battery,
            lastTelemetry: b.timestamp
              ? this.formatDate(b.timestamp)
              : b.lastTelemetryAt
                ? this.formatDate(b.lastTelemetryAt)
                : '-',
            isActive: b.active ?? b.isActive ?? true,
            lat: lat !== null && !Number.isNaN(lat) ? lat : null,
            lng: lng !== null && !Number.isNaN(lng) ? lng : null,

            alertsCount: 0,
            topAlertSeverity: null,
            topAlertType: null
          };
        });

        this.recomputeStats();
        this.loading = false;
        this.updateLastRefresh();

        this.loadBinAlerts();
      },
      error: (err) => {
        console.error('GET /api/bins failed', err);
        this.error = 'Impossible de charger les bacs.';
        this.loading = false;
      }
    });
  }

  loadBinAlerts(): void {
    this.loadingAlerts = true;
    this.alertsError = '';

    this.alertService.searchAlerts({
      resolved: false,
      entityType: 'BIN'
    }).subscribe({
      next: (alerts: AlertDto[]) => {
        this.binAlerts = alerts || [];
        this.rebuildAlertIndexes();
        this.attachAlertsToRows();
        this.recomputeStats();

        this.loadingAlerts = false;
        this.updateLastRefresh();
      },
      error: (err) => {
        console.error('GET /api/alerts?entityType=BIN failed', err);
        this.alertsError = 'Impossible de charger les alertes des bacs.';
        this.loadingAlerts = false;
      }
    });
  }

  refreshAll(): void {
    this.reloadBins();
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

    if (existing.backendId) {
      this.binService.updateBin(existing.backendId, payload).subscribe({
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
    if (row.backendId) {
      this.binService.updateBin(row.backendId, {
        isActive: !row.isActive
      }).subscribe({
        next: () => this.reloadBins(),
        error: (err) => console.error('TOGGLE ACTIVE FAILED', err)
      });
      return;
    }

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

  openAlertsDrawer(row: BinRow): void {
    this.selectedAlertBin = row;
    this.showAlertsDrawer = true;
  }

  closeAlertsDrawer(): void {
    this.showAlertsDrawer = false;
    this.selectedAlertBin = null;
    this.resolvingAlertId = null;
  }

  alertsForRow(row: BinRow | null): AlertDto[] {
    if (!row) return [];

    const byId =
      row.backendId !== null
        ? this.alertsByBinId.get(row.backendId) || []
        : [];

    const byCode = this.alertsByBinCode.get(row.id) || [];

    const merged = [...byId, ...byCode];
    const unique = new Map<number, AlertDto>();

    for (const a of merged) {
      unique.set(a.id, a);
    }

    return Array.from(unique.values()).sort((a, b) => {
      const da = new Date(a.createdAt || '').getTime();
      const db = new Date(b.createdAt || '').getTime();
      return db - da;
    });
  }

  resolveBinAlert(alert: AlertDto): void {
    if (!alert?.id || this.resolvingAlertId) return;

    this.resolvingAlertId = alert.id;

    this.alertService.resolveAlert(alert.id).subscribe({
      next: () => {
        this.resolvingAlertId = null;
        this.loadBinAlerts();
      },
      error: (err) => {
        console.error('RESOLVE BIN ALERT FAILED', err);
        this.resolvingAlertId = null;
        this.alertsError = "Impossible de résoudre l'alerte.";
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

      const matchAlerts =
        this.alertFilter === 'All'
          ? true
          : this.alertFilter === 'WithAlerts'
            ? r.alertsCount > 0
            : this.alertFilter === 'WithoutAlerts'
              ? r.alertsCount === 0
              : this.isCriticalAlertSeverity(r.topAlertSeverity);

      return matchQuery && matchStatus && matchActivity && matchBattery && matchAlerts;
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
        case 'alerts_desc':
          return b.alertsCount - a.alertsCount;
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
    this.alertFilter = 'All';
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

  alertTypeLabel(type?: string | null): string {
    switch ((type || '').toUpperCase()) {
      case 'BIN_FULL': return 'Bac plein';
      case 'BIN_ALMOST_FULL': return 'Bac presque plein';
      case 'BIN_FAST_FILLING': return 'Remplissage rapide';
      case 'BIN_SUDDEN_FILL': return 'Remplissage soudain';
      case 'BIN_SENSOR_STUCK': return 'Capteur bloqué';
      case 'BIN_BATTERY_LOW': return 'Batterie faible';
      case 'BIN_SENSOR_OR_OVERFLOW': return 'Capteur / débordement';
      case 'NEED_EXTRA_BIN_NEARBY': return 'Besoin bac supplémentaire';
      default: return type || 'Alerte';
    }
  }

  severityLabel(severity?: string | null): string {
    switch ((severity || '').toUpperCase()) {
      case 'CRITICAL': return 'Critique';
      case 'HIGH': return 'Élevée';
      case 'MEDIUM': return 'Moyenne';
      case 'LOW': return 'Faible';
      default: return severity || '—';
    }
  }

  alertBadgeLabel(row: BinRow): string {
    if (!row.alertsCount) return '—';

    if (this.isCriticalAlertSeverity(row.topAlertSeverity)) {
      return row.alertsCount > 1 ? `${row.alertsCount} critiques` : 'Critique';
    }

    if ((row.topAlertType || '').includes('BATTERY') || (row.topAlertType || '').includes('SENSOR')) {
      return row.alertsCount > 1 ? `${row.alertsCount} maintenance` : 'Maintenance';
    }

    return row.alertsCount > 1 ? `${row.alertsCount} alertes` : 'Alerte';
  }

  alertBadgeClass(row: BinRow): string {
    if (!row.alertsCount) return 'is-none';

    if (this.isCriticalAlertSeverity(row.topAlertSeverity)) {
      return 'is-critical';
    }

    if ((row.topAlertType || '').includes('BATTERY') || (row.topAlertType || '').includes('SENSOR')) {
      return 'is-maintenance';
    }

    return 'is-warning';
  }

  timeAgo(iso?: string | null): string {
    if (!iso) return '—';

    const d = new Date(iso).getTime();
    if (Number.isNaN(d)) return '—';

    const diff = Date.now() - d;
    const min = Math.floor(diff / 60000);

    if (min < 1) return "à l'instant";
    if (min < 60) return `il y a ${min} min`;

    const h = Math.floor(min / 60);
    if (h < 24) return `il y a ${h} h`;

    const days = Math.floor(h / 24);
    return `il y a ${days} j`;
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
      'Alertes ouvertes',
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
        String(r.alertsCount),
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

  trackByAlert(_: number, alert: AlertDto): number {
    return alert.id;
  }

  private rebuildAlertIndexes(): void {
    this.alertsByBinCode.clear();
    this.alertsByBinId.clear();

    for (const alert of this.binAlerts) {
      if (alert.binCode) {
        const list = this.alertsByBinCode.get(alert.binCode) || [];
        list.push(alert);
        this.alertsByBinCode.set(alert.binCode, list);
      }

      if (alert.binId !== null && alert.binId !== undefined) {
        const list = this.alertsByBinId.get(alert.binId) || [];
        list.push(alert);
        this.alertsByBinId.set(alert.binId, list);
      }
    }
  }

  private attachAlertsToRows(): void {
    this.rows = this.rows.map(row => {
      const alerts = this.alertsForRow(row);
      const top = this.pickTopAlert(alerts);

      return {
        ...row,
        alertsCount: alerts.length,
        topAlertSeverity: top?.severity || null,
        topAlertType: top?.alertType || null
      };
    });
  }

  private pickTopAlert(alerts: AlertDto[]): AlertDto | null {
    if (!alerts.length) return null;

    return [...alerts].sort((a, b) => {
      const sa = this.severityWeight(a.severity);
      const sb = this.severityWeight(b.severity);

      if (sb !== sa) return sb - sa;

      const da = new Date(a.createdAt || '').getTime();
      const db = new Date(b.createdAt || '').getTime();

      return db - da;
    })[0];
  }

  private severityWeight(severity?: string | null): number {
    switch ((severity || '').toUpperCase()) {
      case 'CRITICAL': return 4;
      case 'HIGH': return 3;
      case 'MEDIUM': return 2;
      case 'LOW': return 1;
      default: return 0;
    }
  }

  private isCriticalAlertSeverity(severity?: string | null): boolean {
    const s = (severity || '').toUpperCase();
    return s === 'CRITICAL' || s === 'HIGH';
  }

  private recomputeStats(): void {
    this.stats.total = this.rows.length;
    this.stats.full = this.rows.filter(r => r.status === 'Plein').length;
    this.stats.partial = this.rows.filter(r => r.status === 'Partiel').length;
    this.stats.empty = this.rows.filter(r => r.status === 'Vide').length;
    this.stats.maintenance = this.rows.filter(r => r.status === 'Maintenance').length;
    this.stats.active = this.rows.filter(r => r.isActive).length;
    this.stats.inactive = this.rows.filter(r => !r.isActive).length;
    this.stats.withAlerts = this.rows.filter(r => r.alertsCount > 0).length;
    this.stats.criticalAlerts = this.rows.filter(r => this.isCriticalAlertSeverity(r.topAlertSeverity)).length;
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