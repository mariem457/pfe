import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { tap } from 'rxjs/operators';

export interface LoginResponse {
  token: string;
  role: 'ADMIN' | 'MUNICIPALITY' | 'DRIVER' | 'MAINTENANCE';
  userId: number;
  username: string;
  mustChangePassword: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private API = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  login(email: string, password: string, rememberMe: boolean = true) {
    return this.http
      .post<LoginResponse>(
        `${this.API}/auth/login`,
        { email, password, rememberMe },
        { withCredentials: true }
      )
      .pipe(
        tap((res) => {
          const storage = rememberMe ? localStorage : sessionStorage;

          localStorage.clear();
          sessionStorage.clear();

          storage.setItem('token', res.token);
          storage.setItem('role', res.role);
          storage.setItem('userId', String(res.userId));
          storage.setItem('username', res.username);
          storage.setItem('mustChangePassword', String(res.mustChangePassword));
        })
      );
  }

  forgotPassword(usernameOrEmail: string) {
    return this.http.post(
      `${this.API}/auth/forgot-password`,
      { usernameOrEmail }
    );
  }

  verifyResetCode(identifier: string, code: string) {
    return this.http.post(
      `${this.API}/auth/verify-reset-code`,
      { identifier, code }
    );
  }

  resetPassword(token: string, newPassword: string) {
    return this.http.post(
      `${this.API}/auth/reset-password`,
      { token, newPassword }
    );
  }

  resetPasswordByCode(identifier: string, code: string, newPassword: string) {
    return this.http.post(
      `${this.API}/auth/reset-password-by-code`,
      { identifier, code, newPassword }
    );
  }

  refresh() {
    return this.http.post<LoginResponse>(
      `${this.API}/auth/refresh`,
      {},
      { withCredentials: true }
    );
  }

  logout() {
    const token = this.token;
    const headers = token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : undefined;

    return this.http.post(
      `${this.API}/auth/logout`,
      {},
      { headers, withCredentials: true }
    ).pipe(
      tap(() => this.clearSession())
    );
  }

  clearSession() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('mustChangePassword');

    sessionStorage.removeItem('token');
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('userId');
    sessionStorage.removeItem('username');
    sessionStorage.removeItem('mustChangePassword');
  }

  get token() {
    return localStorage.getItem('token') || sessionStorage.getItem('token');
  }

  get role() {
    return localStorage.getItem('role') || sessionStorage.getItem('role');
  }

  get mustChangePassword() {
    return (localStorage.getItem('mustChangePassword') || sessionStorage.getItem('mustChangePassword')) === 'true';
  }

  get userId() {
    return localStorage.getItem('userId') || sessionStorage.getItem('userId');
  }

  get username() {
    return localStorage.getItem('username') || sessionStorage.getItem('username');
  }

  isLoggedIn() {
    return !!this.token;
  }
}