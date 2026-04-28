import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RoutingService {

  private api = 'http://localhost:8081/api/routing';

  constructor(private http: HttpClient) {}

  replanMission(missionId: number): Observable<any> {
    return this.http.post(`${this.api}/replan/${missionId}`, {});
  }

}