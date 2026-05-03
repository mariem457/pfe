import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
<<<<<<< HEAD
import { Observable, Subject } from 'rxjs';
=======
import { Observable } from 'rxjs';
>>>>>>> origin/enleve-mqtt
import { environment } from '../../environments/environment';

export interface AlertDto {
  id: number;
<<<<<<< HEAD

  binId?: number | null;
  binCode?: string | null;
  telemetryId?: number | null;

  truckId?: number | null;
  truckCode?: string | null;
  missionId?: number | null;
  incidentId?: number | null;

  entityType?: string | null;
  entityId?: number | null;

  alertType: string;
  severity: string;
  title: string;
  message: string;
  recommendation?: string | null;
  actionType?: string | null;

  createdAt: string;
=======
  binId: number;
  binCode: string;
  telemetryId?: number | null;
  alertType: string;   // THRESHOLD | ANOMALY | MAINTENANCE | SYSTEM
  severity: string;    // LOW | MEDIUM | HIGH
  title: string;
  message: string;
  createdAt: string;   // ISO date
>>>>>>> origin/enleve-mqtt
  resolved: boolean;
  resolvedAt?: string | null;
  resolvedByUserId?: number | null;
}

export interface AlertDetailsDto extends AlertDto {
  binLat?: number | null;
  binLng?: number | null;
  zoneName?: string | null;
<<<<<<< HEAD
  telemetryTimestamp?: string | null;
  fillLevel?: number | null;
  batteryLevel?: number | null;
  truckLat?: number | null;
  truckLng?: number | null;
=======

  telemetryTimestamp?: string | null;
  fillLevel?: number | null;
  batteryLevel?: number | null;
>>>>>>> origin/enleve-mqtt
}

@Injectable({ providedIn: 'root' })
export class AlertService {
  private baseUrl = `${environment.apiUrl}/api/alerts`;

<<<<<<< HEAD
  private realtimeAlertSubject = new Subject<AlertDto>();
  realtimeAlert$ = this.realtimeAlertSubject.asObservable();

  private realtimeResolvedSubject = new Subject<AlertDto>();
  realtimeResolved$ = this.realtimeResolvedSubject.asObservable();

  constructor(private http: HttpClient) {}

  pushRealtimeAlert(alert: AlertDto): void {
    this.realtimeAlertSubject.next(alert);
  }

  pushRealtimeResolved(alert: AlertDto): void {
    this.realtimeResolvedSubject.next(alert);
  }

=======
  constructor(private http: HttpClient) {}

  // ✅ open alerts
>>>>>>> origin/enleve-mqtt
  getOpenAlerts(): Observable<AlertDto[]> {
    return this.http.get<AlertDto[]>(`${this.baseUrl}/open`);
  }

<<<<<<< HEAD
=======
  // ✅ alerts by bin
>>>>>>> origin/enleve-mqtt
  getAlertsByBin(binId: number, onlyOpen = false): Observable<AlertDto[]> {
    const params = new HttpParams().set('onlyOpen', String(onlyOpen));
    return this.http.get<AlertDto[]>(`${this.baseUrl}/bins/${binId}`, { params });
  }

<<<<<<< HEAD
  resolveAlert(alertId: number) {
    return this.http.patch<AlertDto>(`${this.baseUrl}/${alertId}/resolve`, {});
  }

=======
  // ✅ resolve
  resolveAlert(alertId: number) {
  return this.http.patch<AlertDto>(`${this.baseUrl}/${alertId}/resolve`, {});

}
  // ✅ details
>>>>>>> origin/enleve-mqtt
  getAlertDetails(alertId: number): Observable<AlertDetailsDto> {
    return this.http.get<AlertDetailsDto>(`${this.baseUrl}/${alertId}`);
  }

<<<<<<< HEAD
=======
  // ✅ search / filters
  // مثال:
  // searchAlerts({ resolved:false, severity:'HIGH', alertType:'MAINTENANCE', q:'bin-001' })
>>>>>>> origin/enleve-mqtt
  searchAlerts(filters: {
    resolved?: boolean | null;
    severity?: string | null;
    alertType?: string | null;
<<<<<<< HEAD
    entityType?: string | null;
    q?: string | null;
  }): Observable<AlertDto[]> {
    let params = new HttpParams();

    if (filters.resolved !== undefined && filters.resolved !== null) {
      params = params.set('resolved', String(filters.resolved));
    }
    if (filters.severity) params = params.set('severity', filters.severity);
    if (filters.alertType) params = params.set('alertType', filters.alertType);
    if (filters.entityType) params = params.set('entityType', filters.entityType);
    if (filters.q) params = params.set('q', filters.q);

    return this.http.get<AlertDto[]>(`${this.baseUrl}`, { params });
  }
  getAlertsByMission(missionId: number): Observable<AlertDto[]> {
  return this.http.get<AlertDto[]>(`${this.baseUrl}/missions/${missionId}`);
}
=======
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
>>>>>>> origin/enleve-mqtt
}