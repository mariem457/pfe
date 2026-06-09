import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MaintenanceDashboardService {

  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  getBins(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/bins`).pipe(
      catchError(err => {
        console.error('GET BINS ERROR:', err);
        return of([]);
      })
    );
  }

  getSensors(): Observable<any[]> {
    return this.getBins();
  }

  getAlerts(): Observable<any[]> {
    return forkJoin({
      alerts: this.http.get<any[]>(`${this.apiUrl}/alerts/open`).pipe(
        catchError(err => {
          console.error('GET ALERTS ERROR:', err);
          return of([]);
        })
      ),
      bins: this.getBins()
    }).pipe(
      map(({ alerts, bins }) => this.mergeGeneratedBatteryAlerts(alerts || [], bins || []))
    );
  }

  resolveAlert(id: number): Observable<any> {
    if (Number(id) < 0) {
      return of({ generated: true, resolved: true });
    }

    return this.http.patch<any>(`${this.apiUrl}/alerts/${id}/resolve`, {}).pipe(
      catchError(err => {
        console.error('RESOLVE ALERT ERROR:', err);
        return of(null);
      })
    );
  }

  getTasks(): Observable<any[]> {
    return this.getAlerts();
  }

  private mergeGeneratedBatteryAlerts(alerts: any[], bins: any[]): any[] {
    const generatedAlerts = bins
      .map((bin, index) => this.toBatteryAlert(bin, index))
      .filter((alert): alert is any => !!alert)
      .filter((generated) => !this.hasOpenBatteryAlert(alerts, generated));

    return [...generatedAlerts, ...alerts];
  }

  private hasOpenBatteryAlert(alerts: any[], generated: any): boolean {
    const generatedBin = this.normalize(generated.binCode);

    return alerts.some((alert) => {
      const type = this.normalize(alert.alertType || alert.alert_type || alert.type);
      const bin = this.normalize(alert.binCode || alert.bin?.binCode || alert.bin_code);

      return bin === generatedBin && type.includes('BATTERY');
    });
  }

  private toBatteryAlert(bin: any, index: number): any | null {
    const status = this.getBatteryStatus(bin);

    if (!['FAIBLE', 'CRITIQUE', 'EN PANNE'].includes(status)) {
      return null;
    }

    const battery = this.getBatteryLevel(bin);
    const binId = Number(bin?.id ?? index + 1);
    const generatedId = -(100000 + (isNaN(binId) ? index + 1 : binId));
    const binCode = bin?.binCode || bin?.bin_code || `BIN-${bin?.id ?? index + 1}`;
    const zoneName = bin?.zoneName || bin?.zone || bin?.zone?.name || 'Zone inconnue';
    const isLow = status === 'FAIBLE';
    const title = isLow ? 'Batterie faible' : 'Batterie en panne';
    const levelText = battery === null ? 'niveau indisponible' : `${battery}%`;

    return {
      id: generatedId,
      generated: true,
      alertType: isLow ? 'BATTERY_LOW' : 'BATTERY_CRITICAL',
      severity: isLow ? 'MEDIUM' : 'CRITICAL',
      title,
      alertTitle: title,
      message: `${binCode} - ${title.toLowerCase()} détectée (${levelText}). Intervention maintenance nécessaire.`,
      description: `${binCode} - ${title.toLowerCase()} détectée (${levelText}). Intervention maintenance nécessaire.`,
      binCode,
      zoneName,
      bin,
      createdAt: bin?.lastTelemetryAt || bin?.last_telemetry_at || new Date().toISOString(),
      resolved: false
    };
  }

  private getBatteryLevel(bin: any): number | null {
    const value = bin?.batteryLevel ?? bin?.battery_level ?? null;

    if (value === null || value === undefined || value === '') {
      return null;
    }

    const numberValue = Number(value);
    return isNaN(numberValue) ? null : numberValue;
  }

  private getBatteryStatus(bin: any): string {
    const rawStatus = this.normalize(bin?.status);

    if (
      rawStatus.includes('OFFLINE') ||
      rawStatus.includes('HORS_SERVICE') ||
      rawStatus.includes('ERROR')
    ) {
      return 'EN PANNE';
    }

    const battery = this.getBatteryLevel(bin);

    if (battery === null) return 'SANS DONNEES';
    if (battery <= 5) return 'EN PANNE';
    if (battery < 20) return 'CRITIQUE';
    if (battery <= 60) return 'FAIBLE';

    return 'NORMAL';
  }

  private normalize(value: any): string {
    return (value || '')
      .toString()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase();
  }
}
