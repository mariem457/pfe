import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, of, forkJoin } from 'rxjs';

export interface RoutingExecutionLogDto {
  strategy: 'FULL_OPTIMIZATION' | 'MANDATORY_ONLY' | 'REFUEL_ONLY' | 'SKIP' | string;
  reason: string;
  shouldOptimize: boolean;
  refuelOnly: boolean;
  trucksCount: number;
  binsSentCount: number;
  mandatoryBinsCount: number;
  missionsCreatedCount: number;
  droppedBinsCount: number;
  matrixSource: string;
  createdAt: string;
}

export interface MandatoryBinInsightDto {
  binId: number;
  fillLevel: number;
  predictedPriority: number;
  predictedHoursToFull: number;
  mandatory: boolean;
  mandatoryByUrgency: boolean;
  mandatoryByFeedback: boolean;
  postponementCount: number;
  feedbackScore: number;
  reason: string;
}

export interface SmartRoutingDashboardResponse {
  lastExecution: RoutingExecutionLogDto | null;
  executionHistory: RoutingExecutionLogDto[];
  mandatoryInsights: MandatoryBinInsightDto[];
  feedbackMandatoryBins: MandatoryBinInsightDto[];
}

@Injectable({
  providedIn: 'root'
})
export class SmartRoutingDashboardService {
  private readonly baseUrl = 'http://localhost:8081/api/routing';

  constructor(private http: HttpClient) {}

  getLastExecution(): Observable<RoutingExecutionLogDto | null> {
    return this.http
      .get<RoutingExecutionLogDto>(`${this.baseUrl}/execution/last`)
      .pipe(
        catchError((error) => {
          console.warn('Smart routing /execution/last failed', error);
          return of(null);
        })
      );
  }

  getExecutionHistory(): Observable<RoutingExecutionLogDto[]> {
    return this.http
      .get<RoutingExecutionLogDto[]>(`${this.baseUrl}/execution/history`)
      .pipe(
        catchError((error) => {
          console.warn('Smart routing /execution/history failed', error);
          return of([]);
        })
      );
  }

  getMandatoryInsights(): Observable<MandatoryBinInsightDto[]> {
    return this.http
      .get<MandatoryBinInsightDto[]>(`${this.baseUrl}/feedback/mandatory-insights`)
      .pipe(
        catchError((error) => {
          console.warn('Smart routing /feedback/mandatory-insights failed', error);
          return of([]);
        })
      );
  }

  getMandatoryByFeedback(): Observable<MandatoryBinInsightDto[]> {
    return this.http
      .get<MandatoryBinInsightDto[]>(`${this.baseUrl}/feedback/mandatory-by-feedback`)
      .pipe(
        catchError((error) => {
          console.warn('Smart routing /feedback/mandatory-by-feedback failed', error);
          return of([]);
        })
      );
  }

  getDashboardData(): Observable<SmartRoutingDashboardResponse> {
    return forkJoin({
      lastExecution: this.getLastExecution(),
      executionHistory: this.getExecutionHistory(),
      mandatoryInsights: this.getMandatoryInsights(),
      feedbackMandatoryBins: this.getMandatoryByFeedback()
    });
  }
}