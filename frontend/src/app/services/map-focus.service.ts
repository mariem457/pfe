import { Injectable } from '@angular/core';

export interface MapFocusTarget {
  type: 'report' | 'bin';
  id?: number | string;
  lat: number;
  lng: number;
  code?: string;
  zone?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MapFocusService {
  private target: MapFocusTarget | null = null;

  setTarget(target: MapFocusTarget): void {
    this.target = target;
  }

  getTarget(): MapFocusTarget | null {
    return this.target;
  }

  clearTarget(): void {
    this.target = null;
  }
}