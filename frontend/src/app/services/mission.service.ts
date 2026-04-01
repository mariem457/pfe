import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MissionResponse {
  id: number;
  missionCode: string;
  driverId: number | null;
  driverName: string | null;
  zoneId: number | null;
  zoneName: string | null;
  status: string;
  priority: string;
  plannedDate: string;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  createdByUserId: number | null;
  notes: string | null;
}

export interface MissionBinResponse {
  id: number;
  missionId: number;
  binId: number;
  binCode: string | null;
  lat: number | null;
  lng: number | null;
  visitOrder: number;
  targetFillThreshold: number | null;
  assignedReason: string | null;
  collected: boolean;
  collectedAt: string | null;
  collectedByDriverId: number | null;
  driverNote: string | null;
  issueType: string | null;
  photoUrl: string | null;
  assignmentStatus: string | null;
  reassignedFromTruckId: number | null;
  reassignedToTruckId: number | null;
  plannedArrival: string | null;
  actualArrival: string | null;
  skippedReason: string | null;
}

export interface RouteCoordinate {
  lat: number;
  lng: number;
}

export interface MissionRouteStop {
  stopOrder: number;
  stopType: string | null;
  binId: number | null;
  lat: number;
  lng: number;
}

export interface MissionRouteResponse {
  missionId: number;
  routePlanId: number | null;
  truckId: number | null;
  totalDistanceKm: number | null;
  estimatedDurationMin: number | null;
  routeCoordinates: RouteCoordinate[];
  routeStops: MissionRouteStop[];
  snappedWaypoints: RouteCoordinate[];
  matrixSource?: string | null;
  geometrySource?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class MissionService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/api/missions';

  getAllMissions(): Observable<MissionResponse[]> {
    return this.http.get<MissionResponse[]>(this.apiUrl);
  }

  getMissionById(id: number): Observable<MissionResponse> {
    return this.http.get<MissionResponse>(`${this.apiUrl}/${id}`);
  }

  getMissionBins(id: number): Observable<MissionBinResponse[]> {
    return this.http.get<MissionBinResponse[]>(`${this.apiUrl}/${id}/bins`);
  }

  getMissionRoute(id: number): Observable<MissionRouteResponse> {
    return this.http.get<MissionRouteResponse>(`${this.apiUrl}/${id}/route`);
  }

  startMission(id: number): Observable<MissionResponse> {
    return this.http.post<MissionResponse>(`${this.apiUrl}/${id}/start`, {});
  }

  completeMission(id: number): Observable<MissionResponse> {
    return this.http.post<MissionResponse>(`${this.apiUrl}/${id}/complete`, {});
  }

  collectMissionBin(
    missionId: number,
    missionBinId: number,
    payload: {
      driverId?: number | null;
      driverNote?: string | null;
      issueType?: string | null;
      photoUrl?: string | null;
    }
  ): Observable<MissionResponse> {
    return this.http.post<MissionResponse>(
      `${this.apiUrl}/${missionId}/bins/${missionBinId}/collect`,
      payload
    );
  }
}