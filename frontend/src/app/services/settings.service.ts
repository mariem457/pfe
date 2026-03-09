import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SettingsProfileResponse {
  firstName: string;
  lastName: string;
  email: string;
  function: string;
  organization: string;
}

export interface UpdateSettingsProfileRequest {
  firstName: string;
  lastName: string;
  email: string;
  function: string;
  organization: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class SettingsService {
  private apiUrl = 'http://localhost:8081/api/settings';

  constructor(private http: HttpClient) {}

  getProfile(): Observable<SettingsProfileResponse> {
    return this.http.get<SettingsProfileResponse>(`${this.apiUrl}/profile`);
  }

  updateProfile(payload: UpdateSettingsProfileRequest): Observable<SettingsProfileResponse> {
    return this.http.put<SettingsProfileResponse>(`${this.apiUrl}/profile`, payload);
  }

  changePassword(payload: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/password`, payload);
  }
}