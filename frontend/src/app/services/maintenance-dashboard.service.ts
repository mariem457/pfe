import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MaintenanceDashboardService {

  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  getBins(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/bins`).pipe(
      catchError(err => {
        console.error('GET BINS ERROR:', err);
        return of([]);
      })
    );
  }

  getSensors(): Observable<any[]> {
    return this.getBins();
  }

  getAlerts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/alerts/open`).pipe(
      catchError(err => {
        console.error('GET ALERTS ERROR:', err);
        return of([]);
      })
    );
  }

  resolveAlert(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/alerts/${id}/resolve`, {}).pipe(
      catchError(err => {
        console.error('RESOLVE ALERT ERROR:', err);
        return of(null);
      })
    );
  }

  getTasks(): Observable<any[]> {
    return of([]);
  }
}