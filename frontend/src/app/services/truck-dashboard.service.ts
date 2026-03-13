import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TruckItem {
  driverId: number;
  truckCode: string;
  driverName: string;
  locationLabel: string;
  progress: number;
  collectedBins: number;
  remainingBins: number;
  fuelLevel?: number;
  etaMinutes?: number;
  active: boolean;
}

export interface TruckDashboardResponse {
  activeTrucks: number;
  totalRoutes: number;
  averageProgress: number;
  fuelStatus: string;
  trucks: TruckItem[];
}

@Injectable({
  providedIn: 'root'
})
export class TruckDashboardService {

  private api = 'http://localhost:8081/api/truck-locations/dashboard';

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<TruckDashboardResponse> {
    return this.http.get<TruckDashboardResponse>(this.api);
  }

}