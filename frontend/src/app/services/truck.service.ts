import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type FuelType = 'DIESEL' | 'ESSENCE' | 'ELECTRIC' | 'HYBRID';

export type TruckStatus =
  | 'AVAILABLE'
  | 'ON_MISSION'
  | 'BREAKDOWN'
  | 'MAINTENANCE'
  | 'REFUELING'
  | 'UNAVAILABLE'
  | 'OUT_OF_SERVICE';

export interface TruckRequest {
  truckCode: string;
  plateNumber?: string;
  model?: string;
  brand?: string;
  fuelType: FuelType;
  tankCapacityLiters?: number;
  fuelLevelLiters?: number;
  fuelConsumptionPerKm?: number;
  maxLoadKg?: number;
  maxBinCapacity?: number;
  currentLoadKg?: number;
  status?: TruckStatus;
  lastKnownLat?: number;
  lastKnownLng?: number;
  zoneId?: number | null;
  zoneName?: string | null;
  isActive?: boolean;
  assignedDriverId?: number | null;
}

export interface TruckResponse {
  id: number;
  truckCode: string;
  plateNumber?: string;
  model?: string;
  brand?: string;
  fuelType: FuelType;
  tankCapacityLiters?: number;
  fuelLevelLiters?: number;
  fuelConsumptionPerKm?: number;
  maxLoadKg?: number;
  maxBinCapacity?: number;
  currentLoadKg?: number;
  status?: TruckStatus;
  assignedDriverId?: number;
  assignedDriverName?: string;
  zoneId?: number;
  zoneName?: string;
  lastKnownLat?: number;
  lastKnownLng?: number;
  lastStatusUpdate?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface TruckStatusUpdate {
  status?: TruckStatus;
  fuelLevelLiters?: number;
  currentLoadKg?: number;
  lastKnownLat?: number;
  lastKnownLng?: number;
}

export interface ZoneResponse {
  id: number;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class TruckService {
  private readonly apiUrl = 'http://localhost:8081/api/trucks';
  private readonly zonesUrl = 'http://localhost:8081/api/zones';

  constructor(private http: HttpClient) {}

  getAll(): Observable<TruckResponse[]> {
    return this.http.get<TruckResponse[]>(this.apiUrl);
  }

  getZones(): Observable<ZoneResponse[]> {
    return this.http.get<ZoneResponse[]>(this.zonesUrl);
  }

  getActive(): Observable<TruckResponse[]> {
    return this.http.get<TruckResponse[]>(`${this.apiUrl}/active`);
  }

  getById(id: number): Observable<TruckResponse> {
    return this.http.get<TruckResponse>(`${this.apiUrl}/${id}`);
  }

  create(data: TruckRequest): Observable<TruckResponse> {
    return this.http.post<TruckResponse>(this.apiUrl, data);
  }

  update(id: number, data: TruckRequest): Observable<TruckResponse> {
    return this.http.put<TruckResponse>(`${this.apiUrl}/${id}`, data);
  }

  updateStatus(id: number, data: TruckStatusUpdate): Observable<TruckResponse> {
    return this.http.patch<TruckResponse>(`${this.apiUrl}/${id}/status`, data);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}