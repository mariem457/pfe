import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AlertDto, AlertService } from '../../../../services/alert.service';
import { RealtimeService } from '../../../../services/realtime.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent implements OnInit, OnDestroy {
  isDark = false;
  openNotifications = false;
  unreadCount = 0;
  latestAlerts: AlertDto[] = [];

  private readonly readAlertsStorageKey = 'admin-principal-read-alert-ids';
  private openAlertIds: number[] = [];
  private sub = new Subscription();

  constructor(
    private alertService: AlertService,
    private realtimeService: RealtimeService
  ) {}

  ngOnInit(): void {
    this.isDark = localStorage.getItem('theme') === 'dark';
    document.body.classList.toggle('dark-mode', this.isDark);
    this.realtimeService.connectAll();
    this.loadAlerts();

    this.sub.add(
      this.alertService.realtimeAlert$.subscribe(alert => {
        if (!alert || alert.resolved) return;
        if (!this.isQrCodeProblemAlert(alert)) return;

        const exists = this.latestAlerts.some(a => a.id === alert.id);
        if (!exists) {
          this.openAlertIds = [alert.id, ...this.openAlertIds.filter(id => id !== alert.id)];
          this.latestAlerts = [alert, ...this.latestAlerts].slice(0, 5);
          this.syncUnreadCount();
        }
      })
    );

    this.sub.add(
      this.alertService.realtimeResolved$.subscribe(alert => {
        if (!this.isQrCodeProblemAlert(alert)) return;

        this.openAlertIds = this.openAlertIds.filter(id => id !== alert.id);
        this.latestAlerts = this.latestAlerts.filter(a => a.id !== alert.id);
        this.syncUnreadCount();
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    localStorage.setItem('theme', this.isDark ? 'dark' : 'light');
    document.body.classList.toggle('dark-mode', this.isDark);
  }

  toggleNotifications(): void {
    this.openNotifications = !this.openNotifications;

    if (this.openNotifications) {
      this.markNotificationsAsRead();
    }
  }

  label(alert: AlertDto): string {
    return this.alertText(alert.binCode || alert.truckCode || `${alert.entityType || 'ALERTE'}-${alert.entityId || alert.id}`);
  }

  severityLabel(severity?: string | null): string {
    const value = (severity || '').trim().toUpperCase();

    switch (value) {
      case 'CRITICAL':
        return 'Critique';
      case 'HIGH':
        return 'Élevée';
      case 'MEDIUM':
        return 'Moyenne';
      case 'LOW':
        return 'Faible';
      default:
        return severity || '';
    }
  }

  alertText(value?: string | null): string {
    if (!value) return '';

    return value
      .replace(/\bTRUCK[-\s]?/gi, 'Camion ')
      .replace(/\bBREAKDOWN\b/g, 'Panne')
      .replace(/\bFUEL_LOW\b/g, 'Carburant faible')
      .replace(/\bGPS_LOST\b/g, 'Perte GPS')
      .replace(/\bTRAFFIC_BLOCK\b/g, 'Blocage de circulation')
      .replace(/\bDELAY\b/g, 'Retard')
      .replace(/\bOVERLOAD\b/g, 'Surcharge')
      .replace(/\bDRIVER_UNAVAILABLE\b/g, 'Chauffeur indisponible')
      .replace(/\bBIN_FULL\b/g, 'Bac plein')
      .replace(/\bBIN_ALMOST_FULL\b/g, 'Bac presque plein')
      .replace(/\bBIN_BATTERY_LOW\b/g, 'Batterie faible')
      .replace(/\bSENSOR_ERROR\b/g, 'Erreur capteur')
      .replace(/\bBLOCKED\b/g, 'Accès bloqué')
      .replace(/\bDAMAGED\b/g, 'Endommagé')
      .replace(/\bOTHER\b/g, 'Autre')
      .replace(/\bALERTE-/g, 'Alerte ');
  }

  private loadAlerts(): void {
    this.alertService.getOpenAlerts().subscribe({
      next: alerts => {
        const list = (alerts || []).filter(alert => this.isQrCodeProblemAlert(alert));
        this.openAlertIds = list.map(alert => alert.id);
        this.latestAlerts = list.slice(0, 5);
        this.syncUnreadCount();
      },
      error: err => {
        console.error('Admin principal header alerts error:', err);
        this.openAlertIds = [];
        this.latestAlerts = [];
        this.unreadCount = 0;
      }
    });
  }

  private markNotificationsAsRead(): void {
    if (this.openAlertIds.length === 0) {
      this.unreadCount = 0;
      return;
    }

    const readIds = this.getReadAlertIds();
    this.openAlertIds.forEach(id => readIds.add(id));
    this.saveReadAlertIds(readIds);
    this.unreadCount = 0;
  }

  private syncUnreadCount(): void {
    const readIds = this.getReadAlertIds();
    this.unreadCount = this.openAlertIds.filter(id => !readIds.has(id)).length;
  }

  private getReadAlertIds(): Set<number> {
    try {
      const raw = localStorage.getItem(this.readAlertsStorageKey);
      const ids = raw ? JSON.parse(raw) : [];
      return new Set(Array.isArray(ids) ? ids.filter(id => Number.isFinite(id)) : []);
    } catch {
      return new Set();
    }
  }

  private saveReadAlertIds(readIds: Set<number>): void {
    localStorage.setItem(this.readAlertsStorageKey, JSON.stringify([...readIds]));
  }

  private isQrCodeProblemAlert(alert: AlertDto): boolean {
    const type = (alert.alertType || '').trim().toUpperCase();
    const text = `${alert.title || ''} ${alert.message || ''}`.toLowerCase();

    return type === 'TRUCK_QR_CODE_PROBLEM' || text.includes('qr code') || text.includes('qrcode');
  }
}
