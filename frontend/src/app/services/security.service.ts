import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SecurityDashboardResponse {
  sessionsActives: number;
  tentativesEchouees24h: number;
  accesApi24h: number;
  authentificationDeuxFacteurs: number;
  alerteMessage: string;
}

export interface SecurityEventResponse {
  type: 'connexion' | 'echec' | 'acces-api' | 'modification' | 'deconnexion';
  titre: string;
  statut: 'Succès' | 'Échec';
  utilisateur: string;
  appareil: string;
  adresseIp: string;
  localisation: string;
  dateHeure: string;
}

export interface SecuritySettingsResponse {
  doubleAuthentification: boolean;
  notificationsConnexion: boolean;
  limitationApi: boolean;
  detectionActiviteSuspecte: boolean;
}

export interface ApiKeyResponse {
  id: number;
  name: string;
  keyValue: string;
  isTest: boolean;
  isActive: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class SecurityService {
  private apiUrl = `${environment.apiUrl}/api/security`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<SecurityDashboardResponse> {
    return this.http.get<SecurityDashboardResponse>(`${this.apiUrl}/dashboard`);
  }

  getEvents(): Observable<SecurityEventResponse[]> {
    return this.http.get<SecurityEventResponse[]>(`${this.apiUrl}/events`);
  }

  getSettings(): Observable<SecuritySettingsResponse> {
    return this.http.get<SecuritySettingsResponse>(`${this.apiUrl}/settings`);
  }

  updateSettings(payload: Partial<SecuritySettingsResponse>): Observable<SecuritySettingsResponse> {
    return this.http.put<SecuritySettingsResponse>(`${this.apiUrl}/settings`, payload);
  }

  getApiKeys(): Observable<ApiKeyResponse[]> {
    return this.http.get<ApiKeyResponse[]>(`${this.apiUrl}/api-keys`);
  }

  generateApiKey(testKey: boolean): Observable<ApiKeyResponse> {
    return this.http.post<ApiKeyResponse>(`${this.apiUrl}/api-keys?testKey=${testKey}`, {});
  }
}