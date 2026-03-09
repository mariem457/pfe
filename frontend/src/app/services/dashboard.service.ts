import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardKpiResponse {
  totalBins: number;
  fullBins: number;
  activeTrucks: number;
  averageFillLevel: number;
}

export interface ChartPointDto {
  label: string;
  value: number;
}

export interface BinDistributionDto {
  emptyBins: number;
  partialBins: number;
  fullBins: number;
  totalBins: number;
}

export interface DashboardChartsResponse {
  fillTrend: ChartPointDto[];
  weeklyCollections: ChartPointDto[];
  distribution: BinDistributionDto;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private apiUrl = 'http://localhost:8081/api/dashboard';

  constructor(private http: HttpClient) {}

  getKpis(): Observable<DashboardKpiResponse> {
    return this.http.get<DashboardKpiResponse>(`${this.apiUrl}/kpis`);
  }

  getCharts(): Observable<DashboardChartsResponse> {
    return this.http.get<DashboardChartsResponse>(`${this.apiUrl}/charts`);
  }
}