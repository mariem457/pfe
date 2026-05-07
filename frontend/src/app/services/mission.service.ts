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

  // Backend may return camelCase or snake_case depending on DTO mapping
  missionStatusDetail?: string | null;
  mission_status_detail?: string | null;

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
  wasteType?: string | null;
  assignedReason: string | null;
  collected: boolean;
  collectedAt: string | null;
  collectedByDriverId: number | null;
  driverNote: string | null;
  issueType: string | null;
  photoUrl: string | null;

  // Used for dynamic replanning display
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

  fuelStationId?: number | null;
  fuelStationName?: string | null;

  disposalSiteId?: number | null;
  disposalSiteName?: string | null;

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
  stopLegDistancesKm?: number[];

  collectionRouteCoordinates?: RouteCoordinate[];
  transferRouteCoordinates?: RouteCoordinate[];

  trafficEnabled?: boolean;
  trafficSource?: string | null;
  estimatedBaseDurationMin?: number | null;
  trafficDelayMin?: number | null;
  roadClosed?: boolean;
  trafficLevel?: 'FLUID' | 'LIGHT' | 'MODERATE' | 'HEAVY' | string;
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

  planAndSaveMissions(): Observable<MissionResponse[]> {
  return this.http.post<MissionResponse[]>(
    'http://localhost:8081/api/routing/plan-and-save',
    {}
  );
}
}