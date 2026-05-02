import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AlertDto {
  id: number;

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
  truckLat?: number | null;
  truckLng?: number | null;
}

@Injectable({ providedIn: 'root' })
export class AlertService {
  private baseUrl = `${environment.apiUrl}/api/alerts`;

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

  getOpenAlerts(): Observable<AlertDto[]> {
    return this.http.get<AlertDto[]>(`${this.baseUrl}/open`);
  }

  getAlertsByBin(binId: number, onlyOpen = false): Observable<AlertDto[]> {
    const params = new HttpParams().set('onlyOpen', String(onlyOpen));
    return this.http.get<AlertDto[]>(`${this.baseUrl}/bins/${binId}`, { params });
  }

  resolveAlert(alertId: number) {
    return this.http.patch<AlertDto>(`${this.baseUrl}/${alertId}/resolve`, {});
  }

  getAlertDetails(alertId: number): Observable<AlertDetailsDto> {
    return this.http.get<AlertDetailsDto>(`${this.baseUrl}/${alertId}`);
  }

  searchAlerts(filters: {
    resolved?: boolean | null;
    severity?: string | null;
    alertType?: string | null;
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
}