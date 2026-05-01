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
  phone?: string;
  role: string;
  isEnabled: boolean;
  accountStatus?: AccountStatus;
  lastLoginAt?: string;
  createdAt?: string;
  registrationDate?: string;
  created_at?: string;
}

export interface DriverRegistrationRequestResponse {
  id: number;
  fullName: string;
  username: string;
  email: string;
  phone: string;
  status: AccountStatus;
  createdAt?: string;
  emailVerified?: boolean;
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

export interface CreateMaintenanceRequest {
  fullName: string;
  username: string;
  email: string;
  phone: string;
  password: string;
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

  getStats(): Observable<UserStatsResponse> {
    return this.http.get<UserStatsResponse>(`${this.baseUrl}/stats`);
  }

  getPendingDriverRequests(): Observable<DriverRegistrationRequestResponse[]> {
    return this.http.get<DriverRegistrationRequestResponse[]>(
      `${this.authUrl}/pending-driver-requests`
    );
  }

  approveDriverRequest(requestId: number): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/approve-driver-request`, {
      requestId
    });
  }

  rejectDriverRequest(requestId: number): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/reject-driver-request`, {
      requestId
    });
  }

  createDriver(payload: CreateDriverRequest): Observable<CreateDriverResponse> {
    return this.http.post<CreateDriverResponse>(this.driversUrl, payload);
  }

  createMaintenance(payload: CreateMaintenanceRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/maintenance`, payload);
  }

  approveDriver(userId: number): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/approve-driver`, { userId });
  }

  rejectDriver(userId: number): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/reject-driver`, { userId });
  }

  updateStatus(id: number, isEnabled: boolean): Observable<UserAdminListResponse> {
    return this.http.patch<UserAdminListResponse>(`${this.baseUrl}/${id}/status`, {
      isEnabled
    });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  disableDriver(userId: number): Observable<UserAdminListResponse> {
    return this.updateStatus(userId, false);
  }

  deleteDriver(userId: number): Observable<void> {
    return this.deleteUser(userId);
  }
}