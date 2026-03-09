import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BinService } from '../../../../services/bin.service';

type BinStatus = 'Plein' | 'Partiel' | 'Vide' | 'Maintenance';

interface BinRow {
  id: string;
  location: string;
  fill: number;        // 0..100
  status: BinStatus;
  battery: number;     // 0..100
  lastCollection: string; // (here: last telemetry time)
  temp: number;        // °C (not provided yet)
}

@Component({
  selector: 'app-bins',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bins.component.html',
  styleUrls: ['./bins.component.css'],
})
export class BinsComponent implements OnInit {

  constructor(private binService: BinService) {}

  // ===== Statistiques =====
  stats = {
    total: 0,
    full: 0,
    partial: 0,
    empty: 0,
  };

  loading = true;
  error = '';

  // ===== Filtres =====
  query = '';
  statusFilter: 'All' | BinStatus = 'All';
  sortBy: 'fill_desc' | 'fill_asc' = 'fill_desc';

  // ===== Données (vraies données API) =====
  rows: BinRow[] = [];

  ngOnInit(): void {
    this.binService.getBins().subscribe({
      next: (bins: any[]) => {
        this.rows = (bins || []).map((b: any) => {
          const fill = Number(b.fillLevel ?? 0);
          const battery = Number(b.batteryLevel ?? 0);
          const mappedStatus = this.mapStatus(b.status, fill);

          return {
            id: b.binCode ?? b.bin_code ?? String(b.binId ?? b.id),
            location: b.zoneName ?? b.zone?.name ?? `(${b.lat}, ${b.lng})`,
            fill,
            status: mappedStatus,
            battery,
            lastCollection: b.timestamp ? this.formatDate(b.timestamp) : '-',
            temp: 0 // مازال ما عناش temperature في telemetry
          };
        });

        this.recomputeStats();
        this.loading = false;
      },
      error: (err) => {
        console.error('GET /api/bins/status failed', err);
        this.error = 'Erreur: ma najamtech njib el bins (token/CORS/url).';
        this.loading = false;
      }
    });
  }

  private formatDate(ts: any): string {
    try {
      return new Date(ts).toLocaleString();
    } catch {
      return String(ts);
    }
  }

  private mapStatus(statusFromApi: string, fill: number): BinStatus {
    const s = (statusFromApi || '').toUpperCase();

    // mapping from backend telemetry status (OK/WARNING/FULL/OVERFLOW/ERROR)
    if (s === 'ERROR') return 'Maintenance';
    if (s === 'FULL' || s === 'OVERFLOW') return 'Plein';
    if (s === 'WARNING') return 'Partiel';

    // fallback by fill level if status is OK/empty/unknown
    if (fill >= 85) return 'Plein';
    if (fill >= 50) return 'Partiel';
    return 'Vide';
  }

  private recomputeStats(): void {
    this.stats.total = this.rows.length;
    this.stats.full = this.rows.filter(r => r.status === 'Plein').length;
    this.stats.partial = this.rows.filter(r => r.status === 'Partiel').length;
    this.stats.empty = this.rows.filter(r => r.status === 'Vide').length;
  }

  // ===== Données filtrées + triées =====
  get filteredRows(): BinRow[] {
    const q = this.query.trim().toLowerCase();

    let data = this.rows.filter(r => {
      const matchQuery =
        !q ||
        r.id.toLowerCase().includes(q) ||
        r.location.toLowerCase().includes(q);

      const matchStatus =
        this.statusFilter === 'All' ? true : r.status === this.statusFilter;

      return matchQuery && matchStatus;
    });

    data = data.sort((a, b) => {
      const diff = a.fill - b.fill;
      return this.sortBy === 'fill_asc' ? diff : -diff;
    });

    return data;
  }

  // ===== Couleur de la barre =====
  fillClass(fill: number, status: BinStatus) {
    if (status === 'Maintenance') return 'is-gray';
    if (fill >= 85) return 'is-red';
    if (fill >= 50) return 'is-orange';
    return 'is-green';
  }

  // ===== Export CSV =====
  exportCsv() {
    const headers = [
      'ID du bac',
      'Localisation',
      'Niveau de remplissage',
      'État',
      'Batterie',
      'Dernière collecte',
      'Température'
    ];

    const lines = this.filteredRows.map(r =>
      [
        r.id,
        r.location,
        `${r.fill}%`,
        r.status,
        `${r.battery}%`,
        r.lastCollection,
        `${r.temp}°C`
      ]
        .map(v => `"${String(v).replace(/"/g, '""')}"`)
        .join(',')
    );

    const csv = [headers.join(','), ...lines].join('\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = 'bacs-intelligents.csv';
    a.click();

    URL.revokeObjectURL(url);
  }
}