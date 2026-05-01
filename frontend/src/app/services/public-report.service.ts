import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PublicReportDto {
  id: number;
  reportCode: string;
  status: 'EN_ATTENTE' | 'VALIDE' | 'REJETE' | 'AFFECTE' | 'RESOLU';
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  address: string;
  latitude: number;
  longitude: number;
  createdAt: string;
  resolvedAt?: string | null;
  resolvedNote?: string | null;
  assignedDriverName?: string | null;
  photoUrl?: string | null;
  reportType?: string | null;
  duplicateOfReportId?: number | null;
  qualificationNote?: string | null;
  decisionReason?: string | null;
}

export interface PublicReportDecisionDto {
  id: number;
  reportId: number;
  actionType: string;
  reason?: string | null;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class PublicReportService {
  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  createReport(data: any, photo?: File): Observable<any> {
    const formData = new FormData();

    const jsonBlob = new Blob([JSON.stringify(data)], {
      type: 'application/json'
    });

    formData.append('data', jsonBlob);

    if (photo) {
      formData.append('photo', photo);
    }

    return this.http.post(`${this.apiUrl}/public-reports`, formData);
  }

  getAllReports(): Observable<PublicReportDto[]> {
    return this.http.get<PublicReportDto[]>(
      `${this.apiUrl}/municipality/public-reports`
    );
  }

  getOptimizationReports(): Observable<PublicReportDto[]> {
    return this.http.get<PublicReportDto[]>(
      `${this.apiUrl}/municipality/public-reports/optimization`
    );
  }

  getReportHistory(id: number): Observable<PublicReportDecisionDto[]> {
    return this.http.get<PublicReportDecisionDto[]>(
      `${this.apiUrl}/municipality/public-reports/${id}/history`
    );
  }

  validateReport(id: number): Observable<PublicReportDto> {
    return this.http.put<PublicReportDto>(
      `${this.apiUrl}/municipality/public-reports/${id}/validate`,
      {}
    );
  }

  rejectReport(id: number, reason: string): Observable<PublicReportDto> {
    return this.http.put<PublicReportDto>(
      `${this.apiUrl}/municipality/public-reports/${id}/reject`,
      { reason }
    );
  }

  qualifyReport(
    id: number,
    qualificationNote: string,
    duplicateOfReportId?: number | null
  ): Observable<PublicReportDto> {
    return this.http.put<PublicReportDto>(
      `${this.apiUrl}/municipality/public-reports/${id}/qualify`,
      {
        qualificationNote,
        duplicateOfReportId: duplicateOfReportId ?? null
      }
    );
  }

  assignReport(id: number, driverId: number): Observable<PublicReportDto> {
    return this.http.put<PublicReportDto>(
      `${this.apiUrl}/municipality/public-reports/${id}/assign`,
      { driverId }
    );
  }

  resolveReport(id: number, resolvedNote: string): Observable<PublicReportDto> {
    return this.http.put<PublicReportDto>(
      `${this.apiUrl}/municipality/public-reports/${id}/resolve`,
      { resolvedNote }
    );
  }

  reverseGeocode(lat: number, lon: number): Observable<any> {
    const url =
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}&addressdetails=1`;

    return this.http.get(url, {
      headers: new HttpHeaders({
        Accept: 'application/json'
      })
    });
  }

  geocodeAddress(address: string): Observable<any[]> {
    const url =
      `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(address)}&limit=5`;

    return this.http.get<any[]>(url, {
      headers: new HttpHeaders({
        Accept: 'application/json'
      })
    });
  }
}