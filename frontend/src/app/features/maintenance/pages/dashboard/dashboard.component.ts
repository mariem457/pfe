import { Component, OnInit } from '@angular/core';
import { MaintenanceDashboardService } from '../../../../services/maintenance-dashboard.service';
import { OnDestroy } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {

  loading = true;
  tasksLoading = true;
  weatherLoading = false;
  weatherUpdatedAt: Date | null = null;
  private weatherTimer: ReturnType<typeof setInterval> | null = null;

  bins: any[] = [];
  sensors: any[] = [];
  tasks: any[] = [];
  treatedTasks: any[] = [];
  taskView: 'open' | 'treated' = 'open';

  totalSensors = 0;

  sensorsActifs = 0;
  sensorsInactifs = 0;
  sensorsEnPanne = 0;
  sensorsSansDonnees = 0;

  batteriesNormal = 0;
  batteriesFaibles = 0;
  batteriesCritiques = 0;
  batteriesEnPanne = 0;
  batteriesSansDonnees = 0;

  weather = {
    temp: 18,
    city: 'Paris 15',
    wind: 11,
    humidity: 67,
    rain: 24,
    description: 'Météo en cours'
  };

  constructor(private maintenanceService: MaintenanceDashboardService) {}

  ngOnInit(): void {
    this.loadData();
    this.loadWeather();
    this.loadTasks();
    this.weatherTimer = setInterval(() => this.loadWeather(), 5 * 60 * 1000);
  }

  ngOnDestroy(): void {
    if (this.weatherTimer) {
      clearInterval(this.weatherTimer);
      this.weatherTimer = null;
    }
  }

  loadWeather(): void {
    this.weatherLoading = true;
    fetch(
      `https://api.open-meteo.com/v1/forecast?latitude=48.8414&longitude=2.3008&current=temperature_2m,relative_humidity_2m,wind_speed_10m,cloud_cover,weather_code&timezone=Europe%2FParis&_=${Date.now()}`,
      { cache: 'no-store' }
    )
      .then((res) => {
        if (!res.ok) {
          throw new Error('Weather API failed');
        }
        return res.json();
      })
      .then((data) => {
        const current = data?.current || {};

        this.weather = {
          temp: Math.round(current.temperature_2m ?? 18),
          city: 'Paris 15',
          wind: Math.round(current.wind_speed_10m ?? 11),
          humidity: Math.round(current.relative_humidity_2m ?? 67),
          rain: Math.round(current.cloud_cover ?? 24),
          description: this.getWeatherDescription(current.weather_code)
        };
        this.weatherUpdatedAt = new Date();
      })
      .catch(() => {
        this.weather = {
          temp: 18,
          city: 'Paris 15',
          wind: 11,
          humidity: 67,
          rain: 24,
          description: 'Météo indisponible'
        };
        this.weatherUpdatedAt = new Date();
      })
      .finally(() => {
        this.weatherLoading = false;
      });
  }

  getWeatherDescription(code: number | null | undefined): string {
    if (code === 0) return 'Ciel dégagé';
    if ([1, 2, 3].includes(Number(code))) return 'Partiellement nuageux';
    if ([45, 48].includes(Number(code))) return 'Brouillard';
    if ([51, 53, 55, 56, 57].includes(Number(code))) return 'Bruine';
    if ([61, 63, 65, 66, 67, 80, 81, 82].includes(Number(code))) return 'Pluie';
    if ([71, 73, 75, 77, 85, 86].includes(Number(code))) return 'Neige';
    if ([95, 96, 99].includes(Number(code))) return 'Orage';

    return 'Météo en cours';
  }

  loadData(): void {
    this.loading = true;

    this.maintenanceService.getBins().subscribe({
      next: (bins) => {
        this.bins = bins || [];
        this.sensors = bins || [];

        this.calculateBatteryStats();
        this.calculateSensorStats();

        this.loading = false;
      },
      error: () => {
        this.bins = [];
        this.sensors = [];

        this.calculateBatteryStats();
        this.calculateSensorStats();

        this.loading = false;
      }
    });
  }

  loadTasks(): void {
    this.tasksLoading = true;

    this.maintenanceService.getAlerts().subscribe({
      next: (alerts) => {
        this.tasks = (alerts || []).filter((alert: any) =>
          this.isMaintenanceAlert(alert)
        );
        this.tasksLoading = false;
      },
      error: () => {
        this.tasks = [];
        this.tasksLoading = false;
      }
    });
  }

  treatTask(task: any): void {
    if (!task?.id) return;

    this.maintenanceService.resolveAlert(task.id).subscribe({
      next: () => {
        this.tasks = this.tasks.filter((item) => item.id !== task.id);
        this.treatedTasks = [task, ...this.treatedTasks];
        this.taskView = 'treated';
      }
    });
  }

  setTaskView(view: 'open' | 'treated'): void {
    this.taskView = view;
  }

  get displayedTasks(): any[] {
    return this.taskView === 'open' ? this.tasks : this.treatedTasks;
  }

  get taskListTitle(): string {
    return this.taskView === 'open' ? 'Liste des tâches' : 'Tâches traitées';
  }

  get emptyTasksLabel(): string {
    return this.taskView === 'open'
      ? 'Aucune tâche active.'
      : 'Aucune tâche traitée pour le moment.';
  }

  get dashboardAlerts(): any[] {
    return this.tasks.slice(0, 5);
  }

  getAlertTitle(alert: any): string {
    return this.getTaskTitle(alert);
  }

  getAlertMessage(alert: any): string {
    return this.getTaskMessage(alert);
  }

  getAlertBin(alert: any): string {
    return this.getTaskBin(alert);
  }

  getAlertType(alert: any): string {
    return this.getTaskType(alert);
  }

  getAlertSeverity(alert: any): string {
    return (alert?.severity || 'MEDIUM').toString().toUpperCase();
  }

  getAlertCardClass(alert: any): string {
    const severity = this.getAlertSeverity(alert);

    if (severity === 'HIGH' || severity === 'CRITICAL') return 'alert-high';
    if (severity === 'LOW') return 'alert-low';
    return 'alert-medium';
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

  getTaskTitle(task: any): string {
    return task?.title || task?.alertTitle || 'Alerte technique';
  }

  getTaskMessage(task: any): string {
    return task?.message || task?.description || 'Une intervention est nécessaire.';
  }

  getTaskType(task: any): string {
    const type = (
      task?.alertType ||
      task?.alert_type ||
      task?.type ||
      ''
    ).toUpperCase();

    if (type.includes('BATTERY')) return 'Batterie';
    if (
      type.includes('SENSOR') ||
      type.includes('NO_DATA') ||
      type.includes('OFFLINE') ||
      type.includes('STUCK')
    ) {
      return 'Capteur';
    }

    return 'Technique';
  }

  getTaskBin(task: any): string {
    return task?.binCode || task?.bin?.binCode || task?.bin_code || 'Poubelle inconnue';
  }

  getTaskDate(task: any): string {
    const value = task?.createdAt || task?.created_at || task?.date || null;
    if (!value) return '-';

    const date = new Date(value);
    if (isNaN(date.getTime())) return '-';

    return date.toLocaleString('fr-FR');
  }

  getTaskSeverityClass(task: any): string {
    const severity = (task?.severity || 'MEDIUM').toString().toUpperCase();

    if (severity === 'HIGH' || severity === 'CRITICAL') return 'danger';
    if (severity === 'LOW') return 'soft';
    return 'warning';
  }

  getBatteryDonutStyle(): Record<string, string> {
    return {
      background: this.buildDonutGradient([
        this.batteriesNormal,
        this.batteriesFaibles,
        this.batteriesCritiques,
        this.batteriesSansDonnees + this.batteriesEnPanne
      ])
    };
  }

  getSensorDonutStyle(): Record<string, string> {
    return {
      background: this.buildDonutGradient([
        this.sensorsActifs,
        this.sensorsInactifs,
        this.sensorsEnPanne,
        this.sensorsSansDonnees
      ])
    };
  }

  private buildDonutGradient(values: number[]): string {
    const colors = ['#14b8a6', '#f59e0b', '#ef4444', '#cbd5e1'];
    const total = values.reduce((sum, value) => sum + value, 0);

    if (total <= 0) {
      return 'conic-gradient(#e2e8f0 0 100%)';
    }

    let cursor = 0;
    const parts = values.map((value, index) => {
      const start = cursor;
      cursor += (value / total) * 100;
      return `${colors[index]} ${start}% ${cursor}%`;
    });

    return `conic-gradient(${parts.join(', ')})`;
  }

  calculateBatteryStats(): void {
    this.batteriesNormal = 0;
    this.batteriesFaibles = 0;
    this.batteriesCritiques = 0;
    this.batteriesEnPanne = 0;
    this.batteriesSansDonnees = 0;

    this.bins.forEach(bin => {
      const status = this.getBatteryStatus(bin);

      if (status === 'NORMAL') this.batteriesNormal++;
      else if (status === 'FAIBLE') this.batteriesFaibles++;
      else if (status === 'CRITIQUE') this.batteriesCritiques++;
      else if (status === 'EN PANNE') this.batteriesEnPanne++;
      else this.batteriesSansDonnees++;
    });
  }

  calculateSensorStats(): void {
    this.totalSensors = this.sensors.length;

    this.sensorsActifs = 0;
    this.sensorsInactifs = 0;
    this.sensorsEnPanne = 0;
    this.sensorsSansDonnees = 0;

    this.sensors.forEach(sensor => {
      const status = this.getSensorStatus(sensor);

      if (status === 'ACTIF') this.sensorsActifs++;
      else if (status === 'INACTIF') this.sensorsInactifs++;
      else if (status === 'EN PANNE') this.sensorsEnPanne++;
      else this.sensorsSansDonnees++;
    });
  }

  getBatteryLevel(item: any): number | null {
    const value = item?.batteryLevel ?? item?.battery_level ?? null;

    if (value === null || value === undefined || value === '') {
      return null;
    }

    const numberValue = Number(value);
    return isNaN(numberValue) ? null : numberValue;
  }

  getBatteryStatus(item: any): string {
    const rawStatus = (item?.status || '').toString().toUpperCase();

    if (
      rawStatus.includes('OFFLINE') ||
      rawStatus.includes('HORS_SERVICE') ||
      rawStatus.includes('ERROR')
    ) {
      return 'EN PANNE';
    }

    const battery = this.getBatteryLevel(item);

    if (battery === null) return 'SANS DONNÉES';
    if (battery <= 5) return 'EN PANNE';
    if (battery < 20) return 'CRITIQUE';
    if (battery <= 60) return 'FAIBLE';

    return 'NORMAL';
  }

  getSensorStatus(sensor: any): string {
    const rawStatus = (sensor?.status || '').toString().toUpperCase();

    if (
      rawStatus.includes('OFFLINE') ||
      rawStatus.includes('HORS_SERVICE') ||
      rawStatus.includes('ERROR')
    ) {
      return 'EN PANNE';
    }

    if (sensor?.isActive === false) {
      return 'INACTIF';
    }

    if (
      sensor?.batteryLevel === null ||
      sensor?.batteryLevel === undefined ||
      sensor?.fillLevel === null ||
      sensor?.fillLevel === undefined
    ) {
      return 'SANS DONNÉES';
    }

    return 'ACTIF';
  }
}

