import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

export interface BinStatusDto {
  id?: number;
  binCode?: string;
  zoneName?: string;
  fillLevel?: number;
  batteryLevel?: number;
  rssi?: number;
  status?: string;
  temperature?: number;
  lastTelemetryAt?: string;
  isActive?: boolean;
  wasteType?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BinService {
  private baseUrl = `${environment.apiUrl}/api/bins`;

  constructor(private http: HttpClient) {}

  getBins(): Observable<any[]> {
    return this.http.get<any[]>(this.baseUrl);
  }

  getBinsStatus(): Observable<BinStatusDto[]> {
    return this.http.get<BinStatusDto[]>(`${this.baseUrl}/status`);
  }

  getAllBins(): Observable<any[]> {
    return this.http.get<any[]>(this.baseUrl);
  }

  getBinById(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  createBin(data: {
    binCode?: string;
    type: string;
    wasteType: string;
    lat: number;
    lng: number;
    installationDate?: string;
    isActive?: boolean;
    notes?: string | null;
  }): Observable<any> {
    return this.http.post<any>(this.baseUrl, data);
  }

  updateBin(id: number, data: {
    binCode?: string;
    type?: string;
    wasteType?: string;
    lat?: number;
    lng?: number;
    installationDate?: string;
    isActive?: boolean;
    notes?: string | null;
  }): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, data);
  }

  deleteBin(id: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/${id}`);
  }
}