import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TruckItem {
  driverId: number | null;
  truckCode: string;
  driverName: string;

  lat?: number;
  lng?: number;
  lastKnownLat?: number;
  lastKnownLng?: number;

  locationLabel: string;

  progress: number;
  collectedBins: number;
  remainingBins: number;

  fuelLevel?: number;
  etaMinutes?: number;

  active: boolean;
  truckStatus: string;

  currentMissionId?: number | null;
}

export interface TruckDashboardResponse {
  activeTrucks: number;
  totalRoutes: number;
  averageProgress: number;
  fuelStatus: string;
  trucks: TruckItem[];
}

export interface RouteCoordinate {
  lat: number;
  lng: number;
}

export interface MissionRouteResponse {
  missionId: number;
  routePlanId: number;
  truckId: number;

  totalDistanceKm?: number;
  estimatedDurationMin?: number;

  routeCoordinates: RouteCoordinate[];
  collectionRouteCoordinates: RouteCoordinate[];
  transferRouteCoordinates: RouteCoordinate[];

  matrixSource?: string;
  geometrySource?: string;
}

@Injectable({
  providedIn: 'root',
})
export class TruckDashboardService {
  private dashboardApi = 'http://localhost:8081/api/truck-locations/dashboard';
  private missionsApi = 'http://localhost:8081/api/missions';

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<TruckDashboardResponse> {
    return this.http.get<TruckDashboardResponse>(this.dashboardApi);
  }

  getMissionRoute(missionId: number): Observable<MissionRouteResponse> {
    return this.http.get<MissionRouteResponse>(
      `${this.missionsApi}/${missionId}/route`
    );
  }
}