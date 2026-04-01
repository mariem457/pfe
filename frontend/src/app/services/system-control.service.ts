import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SystemOverviewResponse {
  cpuUsage: number;
  memoryUsedGb: number;
  memoryTotalGb: number;
  activeServices: number;
  totalServices: number;
  uptime: string;
}

export interface SystemComponentResponse {
  name: string;
  description: string;
  status: string;
  metric1Label: string;
  metric1Value: string;
  metric2Label: string;
  metric2Value: string;
  metric3Label: string;
  metric3Value: string;
}

export interface SystemNotificationResponse {
  type: string;
  title: string;
  moment: string;
}

export interface SystemDatabaseStatusResponse {
  activeConnections: string;
  databaseSize: string;
  queriesPerSecond: string;
  lastBackup: string;
}

export interface SystemSettingsResponse {
  maintenanceMode: boolean;
  automaticBackup: boolean;
  realtimeMonitoring: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class SystemControlService {
  private baseUrl = `${environment.apiUrl}/api/system`;

  constructor(private http: HttpClient) {}

  getOverview(): Observable<SystemOverviewResponse> {
    return this.http.get<SystemOverviewResponse>(`${this.baseUrl}/overview`);
  }

  getServices(): Observable<SystemComponentResponse[]> {
    return this.http.get<SystemComponentResponse[]>(`${this.baseUrl}/services`);
  }

  getNotifications(): Observable<SystemNotificationResponse[]> {
    return this.http.get<SystemNotificationResponse[]>(`${this.baseUrl}/notifications`);
  }

  getDatabaseStatus(): Observable<SystemDatabaseStatusResponse> {
    return this.http.get<SystemDatabaseStatusResponse>(`${this.baseUrl}/database`);
  }

  getSettings(): Observable<SystemSettingsResponse> {
    return this.http.get<SystemSettingsResponse>(`${this.baseUrl}/settings`);
  }

  updateSettings(payload: Partial<SystemSettingsResponse>): Observable<SystemSettingsResponse> {
    return this.http.put<SystemSettingsResponse>(`${this.baseUrl}/settings`, payload);
  }
}