import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BinService {

  private baseUrl = `${environment.apiUrl}/api/bins`;

  constructor(private http: HttpClient) {}

  getBins(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/status`);
  }

  getAllBins(): Observable<any[]> {
    return this.http.get<any[]>(this.baseUrl);
  }

  createBin(data: {
    binCode?: string;
    type: string;
    lat: number;
    lng: number;
    installationDate?: string;
    isActive?: boolean;
    notes?: string | null;
  }): Observable<any> {
    return this.http.post(this.baseUrl, data);
  }

  updateBin(id: number, data: {
    binCode?: string;
    type?: string;
    lat?: number;
    lng?: number;
    installationDate?: string;
    isActive?: boolean;
    notes?: string | null;
  }): Observable<any> {
    return this.http.put(`${this.baseUrl}/${id}`, data);
  }
}