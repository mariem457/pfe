import { Component, OnInit } from '@angular/core';
import { MaintenanceDashboardService } from '../../../../services/maintenance-dashboard.service';

@Component({
  selector: 'app-etat-capteurs',
  templateUrl: './etat-capteurs.component.html',
  styleUrl: './etat-capteurs.component.css'
})
export class EtatCapteursComponent implements OnInit {

  loading = true;
  sensors: any[] = [];
  searchTerm = '';

  constructor(private service: MaintenanceDashboardService) {}

  ngOnInit(): void {
    this.service.getSensors().subscribe({
      next: (data) => {
        this.sensors = data || [];
        this.loading = false;
      },
      error: () => {
        this.sensors = [];
        this.loading = false;
      }
    });
  }

  get totalSensors(): number {
    return this.filteredSensors.length;
  }

  get actifs(): number {
    return this.filteredSensors.filter(s => this.getStatus(s) === 'ACTIF').length;
  }

  get enPanne(): number {
    return this.filteredSensors.filter(s => this.getStatus(s) === 'EN PANNE').length;
  }

  get sansDonnees(): number {
    return this.filteredSensors.filter(s => this.getStatus(s) === 'SANS DONNÉES').length;
  }

  get filteredSensors(): any[] {
    const query = this.searchTerm.trim().toLowerCase();

    if (!query) return this.sensors;

    return this.sensors.filter((sensor) => {
      const values = [
        this.getCode(sensor),
        this.getZone(sensor),
        this.getLastValue(sensor),
        this.getStatus(sensor)
      ];

      return values.some((value) =>
        value.toString().toLowerCase().includes(query)
      );
    });
  }

  onSearch(event: Event): void {
    this.searchTerm = (event.target as HTMLInputElement).value;
  }

  getCode(s: any): string {
    return s.binCode || s.bin_code || `CAP-${s.id}`;
  }

  getZone(s: any): string {
    return s.zoneName || s.zone || 'Non affectée';
  }

  getLastValue(s: any): string {
    const fill = s.fillLevel;
    const battery = s.batteryLevel;

    if (fill === null || fill === undefined || battery === null || battery === undefined) {
      return 'Aucune donnée';
    }

    return `Remplissage ${fill}% | Batterie ${battery}%`;
  }

  getStatus(s: any): string {
    const raw = (s.status || '').toString().toUpperCase();

    if (raw.includes('ERROR') || raw.includes('OFFLINE') || raw.includes('HORS_SERVICE')) {
      return 'EN PANNE';
    }

    if (s.isActive === false) {
      return 'INACTIF';
    }

    if (
      s.batteryLevel === null || s.batteryLevel === undefined ||
      s.fillLevel === null || s.fillLevel === undefined
    ) {
      return 'SANS DONNÉES';
    }

    return 'ACTIF';
  }

  getClass(s: any): string {
    const status = this.getStatus(s);

    if (status === 'ACTIF') return 'normal';
    if (status === 'EN PANNE') return 'panne';
    if (status === 'INACTIF') return 'faible';
    return 'sans';
  }
}
