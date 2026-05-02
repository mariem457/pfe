import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TruckIncident {
  id: number;
  truckId: number;
  truckCode: string;
  missionId?: number | null;
  incidentType: string;
  severity: string;
  description: string;
  status: string;
  autoDetected: boolean;
  lat?: number | null;
  lng?: number | null;
  reportedAt: string;
  resolvedAt?: string | null;
  reportedByUserId?: number | null;
  reportedByUsername?: string | null;
}

export interface AutoIncidentRunResponse {
  scannedTrucks: number;
  createdIncidents: number;
}

@Injectable({
  providedIn: 'root',
})
export class TruckIncidentService {
  private incidentApiUrl = 'http://localhost:8081/api/truck-incidents';
  private autoIncidentApiUrl = 'http://localhost:8081/api/auto-incidents';

  constructor(private http: HttpClient) {}

  getOpenIncidents(): Observable<TruckIncident[]> {
    return this.http.get<TruckIncident[]>(`${this.incidentApiUrl}/open`);
  }

  runAutoDetection(): Observable<AutoIncidentRunResponse> {
    return this.http.post<AutoIncidentRunResponse>(
      `${this.autoIncidentApiUrl}/run`,
      {}
    );
  }

  resolveIncident(id: number, description?: string): Observable<TruckIncident> {
    return this.http.patch<TruckIncident>(`${this.incidentApiUrl}/${id}/status`, {
      status: 'RESOLVED',
      description: description ?? '',
    });
  }
}