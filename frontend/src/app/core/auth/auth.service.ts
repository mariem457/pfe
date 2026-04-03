import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginResponse {
  token: string;
  role: string;
  userId: number;
  username: string;
  mustChangePassword: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = 'http://localhost:8081/api/auth';

  constructor(private http: HttpClient) {}

  login(usernameOrEmail: string, password: string, rememberMe: boolean): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/login`,
      { usernameOrEmail, password, rememberMe },
      { withCredentials: true }
    );
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/forgot-password`,
      { email }
    );
  }

  resetPassword(token: string, newPassword: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/reset-password`,
      { token, newPassword }
    );
  }

  refresh(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/refresh`,
      {},
      { withCredentials: true }
    );
  }
}