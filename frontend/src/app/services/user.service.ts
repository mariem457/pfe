import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type AccountStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface UserAdminListResponse {
  id: number;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  role: string;
  isEnabled: boolean;
  lastLoginAt?: string;
  accountStatus?: AccountStatus;
}

export interface PendingDriverRequestResponse {
  id: number;
  fullName: string;
  username: string;
  email: string;
  phone: string;
  status: AccountStatus;
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
  password: string;
}

export interface CreateDriverResponse {
  userId: number;
  driverId: number;
  username: string;
  role: string;
  accountStatus: string;
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
  private authUrl = `${environment.apiUrl}/api/auth`;

  constructor(private http: HttpClient) {}

  getUsers(): Observable<UserAdminListResponse[]> {
    return this.http.get<UserAdminListResponse[]>(this.baseUrl);
  }

  getPendingDriverRequests(): Observable<PendingDriverRequestResponse[]> {
    return this.http.get<PendingDriverRequestResponse[]>(
      `${this.authUrl}/pending-driver-requests`
    );
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

  approveDriver(requestId: number): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/approve-driver-request`, {
      requestId
    });
  }

  rejectDriver(requestId: number): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/reject-driver-request`, {
      requestId
    });
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