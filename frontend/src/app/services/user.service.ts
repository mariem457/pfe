import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserAdminListResponse {
  id: number;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  role: string;
  isEnabled: boolean;
  lastLoginAt?: string;
}

export interface UserStatsResponse {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  drivers: number;
}

export interface CreateDriverRequest {
  fullName: string;
  username: string;
  email: string;
  phone: string;
  vehicleCode: string;
}

export interface CreateDriverResponse {
  userId: number;
  driverId: number;
  username: string;
  role: string;
  tempPassword: string;
}

export interface CreateUserRequest {
  fullName: string;
  username: string;
  email: string;
  phone: string;
  role: string;
  isEnabled: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private baseUrl = `${environment.apiUrl}/api/users`;
  private driversUrl = `${environment.apiUrl}/api/drivers`;

  constructor(private http: HttpClient) {}

  getUsers(): Observable<UserAdminListResponse[]> {
    return this.http.get<UserAdminListResponse[]>(this.baseUrl);
  }

  getStats(): Observable<UserStatsResponse> {
    return this.http.get<UserStatsResponse>(`${this.baseUrl}/stats`);
  }

  createDriver(payload: CreateDriverRequest): Observable<CreateDriverResponse> {
    return this.http.post<CreateDriverResponse>(this.driversUrl, payload);
  }

  createUser(payload: CreateUserRequest): Observable<any> {
    return this.http.post<any>(this.baseUrl, payload);
  }

  updateStatus(id: number, isEnabled: boolean): Observable<UserAdminListResponse> {
    return this.http.patch<UserAdminListResponse>(`${this.baseUrl}/${id}/status`, {
      isEnabled
    });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}