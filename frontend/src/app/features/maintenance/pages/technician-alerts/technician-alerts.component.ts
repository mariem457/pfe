import { Component, OnInit } from '@angular/core';
import { MaintenanceDashboardService } from '../../../../services/maintenance-dashboard.service';

@Component({
  selector: 'app-technician-alerts',
  templateUrl: './technician-alerts.component.html',
  styleUrls: ['./technician-alerts.component.css']
})
export class TechnicianAlertsComponent implements OnInit {

  loading = true;
  alerts: any[] = [];

  constructor(private service: MaintenanceDashboardService) {}

  ngOnInit(): void {
    this.loadAlerts();
  }

  loadAlerts(): void {
    this.loading = true;

    this.service.getAlerts().subscribe({
      next: (data) => {

        console.log('RAW ALERTS = ', data);

        this.alerts = (data || []).filter((a: any) =>
          this.isMaintenanceAlert(a)
        );

        console.log('FILTERED ALERTS = ', this.alerts);

        this.loading = false;
      },

      error: (err) => {
        console.error('ALERTS ERROR:', err);
        this.alerts = [];
        this.loading = false;
      }
    });
  }

  resolve(alert: any): void {

    if (!alert?.id) return;

    this.service.resolveAlert(alert.id).subscribe({
      next: () => {
        this.loadAlerts();
      },
      error: (err) => {
        console.error('RESOLVE ERROR:', err);
      }
    });
  }

  private isMaintenanceAlert(alert: any): boolean {

    const type = (
      alert.alertType ||
      alert.alert_type ||
      alert.type ||
      ''
    ).toUpperCase();

    return [
      'BIN_SENSOR_STUCK',
      'SENSOR_OFFLINE',
      'BATTERY_LOW',
      'BATTERY_CRITICAL',
      'BATTERY_SOLAR_LOW',
      'NO_DATA',
      'CAPTEUR_BLOQUE'
    ].includes(type);
  }

  get highCount(): number {
    return this.alerts.filter(a =>
      this.getSeverity(a) === 'HIGH' ||
      this.getSeverity(a) === 'CRITICAL'
    ).length;
  }

  get mediumCount(): number {
    return this.alerts.filter(a =>
      this.getSeverity(a) === 'MEDIUM'
    ).length;
  }

  get lowCount(): number {
    return this.alerts.filter(a =>
      this.getSeverity(a) === 'LOW'
    ).length;
  }

  getTitle(alert: any): string {
    return (
      alert.title ||
      alert.alertTitle ||
      'Alerte technique'
    );
  }

  getMessage(alert: any): string {
    return (
      alert.message ||
      alert.description ||
      'Une intervention est nécessaire.'
    );
  }

  getBinCode(alert: any): string {
    return (
      alert.binCode ||
      alert.bin?.binCode ||
      alert.bin_code ||
      'Poubelle inconnue'
    );
  }

  getZone(alert: any): string {
    return (
      alert.zoneName ||
      alert.zone ||
      alert.bin?.zoneName ||
      alert.bin?.zone?.name ||
      'Zone inconnue'
    );
  }

  getSeverity(alert: any): string {

    return (
      alert.severity ||
      'MEDIUM'
    ).toString().toUpperCase();
  }

  getSeverityClass(alert: any): string {

    const s = this.getSeverity(alert);

    if (s === 'HIGH' || s === 'CRITICAL') {
      return 'high';
    }

    if (s === 'LOW') {
      return 'low';
    }

    return 'medium';
  }

  getType(alert: any): string {

    const type = (
      alert.alertType ||
      alert.alert_type ||
      alert.type ||
      ''
    ).toUpperCase();

    if (
      type.includes('BATTERY')
    ) {
      return 'BATTERIE';
    }

    if (
      type.includes('SENSOR') ||
      type.includes('NO_DATA') ||
      type.includes('OFFLINE') ||
      type.includes('STUCK')
    ) {
      return 'CAPTEUR';
    }

    return 'TECHNIQUE';
  }

  formatDate(alert: any): string {

    const value =
      alert.createdAt ||
      alert.created_at ||
      alert.date ||
      null;

    if (!value) return '-';

    const d = new Date(value);

    if (isNaN(d.getTime())) return '-';

    return d.toLocaleString('fr-FR');
  }
}