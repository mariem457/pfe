import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

export interface LoginResponse {
  token: string;
  role: 'ADMIN' | 'MUNICIPALITY' | 'DRIVER';
  userId: number;
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private API = 'http://localhost:8080/api'; // بدّلها حسب back

  constructor(private http: HttpClient) {}

  login(usernameOrEmail: string, password: string) {
    return this.http.post<LoginResponse>(`${this.API}/auth/login`, { usernameOrEmail, password })
      .pipe(tap(res => {
        localStorage.setItem('token', res.token);
        localStorage.setItem('role', res.role);
        localStorage.setItem('userId', String(res.userId));
      }));
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
  }

  get token() { return localStorage.getItem('token'); }
  get role() { return localStorage.getItem('role'); }
  isLoggedIn() { return !!this.token; }
}