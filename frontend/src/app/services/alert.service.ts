import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AlertDto {
  id: number;
  binId: number;
  binCode: string;
  telemetryId?: number | null;
  alertType: string;   // THRESHOLD | ANOMALY | MAINTENANCE | SYSTEM
  severity: string;    // LOW | MEDIUM | HIGH
  title: string;
  message: string;
  createdAt: string;   // ISO date
  resolved: boolean;
  resolvedAt?: string | null;
  resolvedByUserId?: number | null;
}

export interface AlertDetailsDto extends AlertDto {
  binLat?: number | null;
  binLng?: number | null;
  zoneName?: string | null;

  telemetryTimestamp?: string | null;
  fillLevel?: number | null;
  batteryLevel?: number | null;
}

@Injectable({ providedIn: 'root' })
export class AlertService {
  private baseUrl = `${environment.apiUrl}/api/alerts`;

  constructor(private http: HttpClient) {}

  // ✅ open alerts
  getOpenAlerts(): Observable<AlertDto[]> {
    return this.http.get<AlertDto[]>(`${this.baseUrl}/open`);
  }

  // ✅ alerts by bin
  getAlertsByBin(binId: number, onlyOpen = false): Observable<AlertDto[]> {
    const params = new HttpParams().set('onlyOpen', String(onlyOpen));
    return this.http.get<AlertDto[]>(`${this.baseUrl}/bins/${binId}`, { params });
  }

  // ✅ resolve
  resolveAlert(alertId: number) {
  return this.http.patch<AlertDto>(`${this.baseUrl}/${alertId}/resolve`, {});

}
  // ✅ details
  getAlertDetails(alertId: number): Observable<AlertDetailsDto> {
    return this.http.get<AlertDetailsDto>(`${this.baseUrl}/${alertId}`);
  }

  // ✅ search / filters
  // مثال:
  // searchAlerts({ resolved:false, severity:'HIGH', alertType:'MAINTENANCE', q:'bin-001' })
  searchAlerts(filters: {
    resolved?: boolean | null;
    severity?: string | null;
    alertType?: string | null;
    q?: string | null;
  }): Observable<AlertDto[]> {

    let params = new HttpParams();
    if (filters.resolved !== undefined && filters.resolved !== null) {
      params = params.set('resolved', String(filters.resolved));
    }
    if (filters.severity) {
      params = params.set('severity', filters.severity);
    }
    if (filters.alertType) {
      params = params.set('alertType', filters.alertType);
    }
    if (filters.q) {
      params = params.set('q', filters.q);
    }

    return this.http.get<AlertDto[]>(`${this.baseUrl}`, { params });
  }
}