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

@Injectable({
  providedIn: 'root',
})
export class TruckIncidentService {
  private apiUrl = 'http://10.221.127.114:8081/api/truck-incidents';

  constructor(private http: HttpClient) {}

  getOpenIncidents(): Observable<TruckIncident[]> {
    return this.http.get<TruckIncident[]>(`${this.apiUrl}/open`);
  }

  resolveIncident(id: number, description?: string): Observable<TruckIncident> {
    return this.http.patch<TruckIncident>(`${this.apiUrl}/${id}/status`, {
      status: 'RESOLVED',
      description: description ?? '',
    });
  }
}