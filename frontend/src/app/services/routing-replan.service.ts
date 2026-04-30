import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ReplanRequest {
  affectedTruckId: number;
  incidentType: string;
  reason: string;
}

@Injectable({
  providedIn: 'root',
})
export class RoutingReplanService {
  private apiUrl = 'http://localhost:8081/api/routing';

  constructor(private http: HttpClient) {}

  replanMission(missionId: number, request: ReplanRequest): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/replan/${missionId}`,
      request
    );
  }
}