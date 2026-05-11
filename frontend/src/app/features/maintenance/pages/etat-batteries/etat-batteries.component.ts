import { Component, OnInit } from '@angular/core';
import { MaintenanceDashboardService } from '../../../../services/maintenance-dashboard.service';

@Component({
  selector: 'app-etat-batteries',
  templateUrl: './etat-batteries.component.html',
  styleUrl: './etat-batteries.component.css'
})
export class EtatBatteriesComponent implements OnInit {

  loading = true;
  bins: any[] = [];
  searchTerm = '';

  selectedBin: any = null;

  constructor(private service: MaintenanceDashboardService) {}

  ngOnInit(): void {
    this.service.getBins().subscribe({
      next: (data) => {
        this.bins = data || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement batteries:', err);
        this.bins = [];
        this.loading = false;
      }
    });
  }

  get total(): number {
    return this.filteredBins.length;
  }

  get normal(): number {
    return this.filteredBins.filter(b => this.getBatteryStatus(b) === 'NORMAL').length;
  }

  get faible(): number {
    return this.filteredBins.filter(b => this.getBatteryStatus(b) === 'FAIBLE').length;
  }

  get critique(): number {
    return this.filteredBins.filter(b => this.getBatteryStatus(b) === 'CRITIQUE').length;
  }

  get enPanne(): number {
    return this.filteredBins.filter(b => this.getBatteryStatus(b) === 'EN PANNE').length;
  }

  get filteredBins(): any[] {
    const query = this.searchTerm.trim().toLowerCase();

    if (!query) return this.bins;

    return this.bins.filter((bin) => {
      const values = [
        this.getCode(bin),
        this.getZone(bin),
        this.getWasteType(bin),
        this.getBatteryStatus(bin)
      ];

      return values.some((value) =>
        value.toString().toLowerCase().includes(query)
      );
    });
  }

  onSearch(event: Event): void {
    this.searchTerm = (event.target as HTMLInputElement).value;
  }

  getCode(bin: any): string {
    return bin.binCode || bin.bin_code || `BIN-${bin.id}`;
  }

  getZone(bin: any): string {
    return bin.zoneName || bin.zone || 'Non affectée';
  }

  getBatteryLevel(bin: any): number | null {
    const value = bin.batteryLevel ?? bin.battery_level ?? null;
    if (value === null || value === undefined || value === '') return null;

    const n = Number(value);
    return isNaN(n) ? null : n;
  }

  getBatteryStatus(bin: any): string {
    const battery = this.getBatteryLevel(bin);
    const raw = (bin.status || '').toString().toUpperCase();

    if (raw.includes('OFFLINE') || raw.includes('HORS_SERVICE') || raw.includes('ERROR')) {
      return 'EN PANNE';
    }

    if (battery === null) return 'SANS DONNÉES';
    if (battery <= 5) return 'EN PANNE';
    if (battery < 20) return 'CRITIQUE';
    if (battery <= 60) return 'FAIBLE';

    return 'NORMAL';
  }

  getBatteryClass(bin: any): string {
    const status = this.getBatteryStatus(bin);

    if (status === 'NORMAL') return 'normal';
    if (status === 'FAIBLE') return 'faible';
    if (status === 'CRITIQUE') return 'critique';
    if (status === 'EN PANNE') return 'panne';

    return 'sans';
  }

  getLastTelemetry(bin: any): string {
    const value = bin.lastTelemetryAt || bin.last_telemetry_at || null;
    if (!value) return 'Aucune donnée';

    const d = new Date(value);
    if (isNaN(d.getTime())) return 'Aucune donnée';

    return d.toLocaleString('fr-FR');
  }

  openDetails(bin: any): void {
    this.selectedBin = bin;
  }

  closeDetails(): void {
    this.selectedBin = null;
  }
  getWasteType(bin: any): string {
  const type = (bin.wasteType || '').toUpperCase();

  if (type === 'GREEN') return 'Organique ';
  if (type === 'YELLOW') return 'Plastique ';
  if (type === 'WHITE') return 'Verre ';
  if (type === 'GRAY') return 'Mixte ';

  return '-';
}

getWasteClass(bin: any): string {
  const type = (bin.wasteType || '').toUpperCase();

  if (type === 'GREEN') return 'type-organique';
  if (type === 'YELLOW') return 'type-plastique';
  if (type === 'WHITE') return 'type-verre';
  if (type === 'GRAY') return 'type-mixte';

  return 'type-mixte';
}
}

