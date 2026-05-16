import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DriverNotificationResponse {
  id: number;
  type: string;
  title: string;
  message: string;
  createdAt: string;
  read: boolean;

  incidentId?: number | null;
  truckId?: number | null;
  truckCode?: string | null;
  missionId?: number | null;

  status?: string;
  response?: string | null;
  readAt?: string | null;
  respondedAt?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class DriverNotificationService {
  private readonly apiUrl = 'http://localhost:8081/api/driver/notifications';

  constructor(private http: HttpClient) {}

  contactDriver(incidentId: number, message?: string): Observable<DriverNotificationResponse> {
    return this.http.post<DriverNotificationResponse>(`${this.apiUrl}/contact-driver`, {
      incidentId,
      message: message ?? null
    });
  }

  getLatestByIncident(incidentId: number): Observable<DriverNotificationResponse> {
    return this.http.get<DriverNotificationResponse>(`${this.apiUrl}/incident/${incidentId}/latest`);
  }
}