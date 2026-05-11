import { Component, OnInit } from '@angular/core';
import { MaintenanceDashboardService } from '../../../../services/maintenance-dashboard.service';

@Component({
  selector: 'app-battery-captures',
  templateUrl: './battery-captures.component.html',
  styleUrl: './battery-captures.component.css'
})
export class BatteryCapturesComponent implements OnInit {

  loading = true;
  sensors: any[] = [];

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

  getLastValue(sensor: any): string {
    if (!sensor?.lastTelemetryAt) return 'Aucune donnée';

    return `Fill ${sensor.fillLevel ?? '-'}% | Battery ${sensor.batteryLevel ?? '-'}%`;
  }

  getStatus(sensor: any): string {
    if (!sensor?.lastTelemetryAt) return 'SANS DONNÉES';

    const status = (sensor.status || '').toUpperCase();

    if (status.includes('ERROR') || status.includes('OFFLINE')) return 'EN PANNE';
    if (sensor.isActive === false) return 'INACTIF';

    return 'ACTIF';
  }

  getClass(sensor: any): string {
    const s = this.getStatus(sensor);

    if (s === 'ACTIF') return 'normal';
    if (s === 'INACTIF') return 'faible';
    if (s === 'EN PANNE') return 'panne';
    return 'sans';
  }
}