import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DriverResponse {
  id: number;
  fullName?: string;
  username?: string;
  email?: string;
  phoneNumber?: string;
  accountStatus?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DriverService {
  private readonly apiUrl = 'http://localhost:8081/api/drivers';

  constructor(private http: HttpClient) {}

  getAllDrivers(): Observable<DriverResponse[]> {
    return this.http.get<DriverResponse[]>(this.apiUrl);
  }
}