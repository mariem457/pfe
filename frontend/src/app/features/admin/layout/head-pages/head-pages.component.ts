import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter, map, Subscription } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';

import { ThemeService } from '../../../../services/theme.service';
import { AlertService, AlertDto } from '../../../../services/alert.service';
import { RealtimeService } from '../../../../services/realtime.service';

type AlertCategory = 'ALL' | 'BIN' | 'TRUCK' | 'MISSION' | 'DRIVER' | 'SYSTEM';

@Component({
  selector: 'app-head-pages',
  standalone: true,
  imports: [MatIconModule, CommonModule],

  // كان ملفاتك اسمهم header.component.* بدّل السطرين هاذم:
  // templateUrl: './header.component.html',
  // styleUrls: ['./header.component.css']

  templateUrl: './head-pages.component.html',
  styleUrls: ['./head-pages.component.css']
})
export class HeadPagesComponent implements OnInit, OnDestroy {
  title = '';
  unreadCount = 0;

  todayAlerts: AlertDto[] = [];
  openMenu = false;

  selectedAlertCategory: AlertCategory = 'ALL';

  alertCategories: Array<{ key: AlertCategory; label: string }> = [
    { key: 'ALL', label: 'Toutes' },
    { key: 'BIN', label: 'Bacs' },
    { key: 'TRUCK', label: 'Camions' },
    { key: 'MISSION', label: 'Missions' },
    { key: 'DRIVER', label: 'Chauffeurs' },
    { key: 'SYSTEM', label: 'Système' }
  ];

  private sub = new Subscription();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    public themeService: ThemeService,
    private alertService: AlertService,
    private realtimeService: RealtimeService
  ) {
    this.sub.add(
      this.router.events
        .pipe(
          filter(event => event instanceof NavigationEnd),
          map(() => {
            let currentRoute = this.route;
            while (currentRoute.firstChild) {
              currentRoute = currentRoute.firstChild;
            }
            return currentRoute.snapshot.data['title'];
          })
        )
        .subscribe(title => {
          this.title = title || '';
        })
    );
  }

  ngOnInit(): void {
    this.realtimeService.connectAll();
    this.loadTodayAlerts();

    this.sub.add(
      this.alertService.realtimeAlert$.subscribe(alert => {
        if (!alert || alert.resolved) return;
        if (!this.isToday(alert.createdAt)) return;

        const exists = this.todayAlerts.some(a => a.id === alert.id);

        if (!exists) {
          this.todayAlerts = [alert, ...this.todayAlerts].sort(
            (a, b) =>
              new Date(b.createdAt || '').getTime() -
              new Date(a.createdAt || '').getTime()
          );

          this.unreadCount = this.todayAlerts.length;
        }
      })
    );

    this.sub.add(
      this.alertService.realtimeResolved$.subscribe(alert => {
        if (!alert) return;

        this.todayAlerts = this.todayAlerts.filter(a => a.id !== alert.id);
        this.unreadCount = this.todayAlerts.length;
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  loadTodayAlerts(): void {
    this.alertService.searchAlerts({
      resolved: false
    }).subscribe({
      next: alerts => {
        this.todayAlerts = (alerts || [])
          .filter(a => this.isToday(a.createdAt))
          .sort((a, b) =>
            new Date(b.createdAt || '').getTime() -
            new Date(a.createdAt || '').getTime()
          );

        this.unreadCount = this.todayAlerts.length;
      },
      error: err => {
        console.error('LOAD TODAY ALERTS FAILED', err);
        this.todayAlerts = [];
        this.unreadCount = 0;
      }
    });
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  toggleMenu(): void {
    this.openMenu = !this.openMenu;

    if (this.openMenu) {
      this.loadTodayAlerts();
    }
  }

  get filteredTodayAlerts(): AlertDto[] {
    if (this.selectedAlertCategory === 'ALL') {
      return this.todayAlerts;
    }

    return this.todayAlerts.filter(a =>
      this.normalizedEntityType(a) === this.selectedAlertCategory
    );
  }

  get urgentAlertsCount(): number {
    return this.todayAlerts.filter(a =>
      ['CRITICAL', 'HIGH'].includes((a.severity || '').toUpperCase())
    ).length;
  }

  get mediumAlertsCount(): number {
    return this.todayAlerts.filter(a =>
      (a.severity || '').toUpperCase() === 'MEDIUM'
    ).length;
  }

  label(a: AlertDto): string {
    return (
      a.binCode ||
      a.truckCode ||
      this.missionLabel(a) ||
      `${this.normalizedEntityType(a)}-${a.entityId || a.id}`
    );
  }

  missionLabel(a: AlertDto): string | null {
    if (a.missionId) {
      return `MISSION-${a.missionId}`;
    }

    if ((a.entityType || '').toUpperCase() === 'MISSION') {
      return `MISSION-${a.entityId || a.id}`;
    }

    return null;
  }

  normalizedEntityType(a: AlertDto): AlertCategory {
    const e = (a.entityType || '').toUpperCase();

    if (e === 'BIN') return 'BIN';
    if (e === 'TRUCK') return 'TRUCK';
    if (e === 'MISSION') return 'MISSION';
    if (e === 'DRIVER') return 'DRIVER';

    if (a.binId || a.binCode) return 'BIN';
    if (a.truckId || a.truckCode || a.incidentId) return 'TRUCK';
    if (a.missionId) return 'MISSION';

    return 'SYSTEM';
  }

  entityLabel(a: AlertDto): string {
    switch (this.normalizedEntityType(a)) {
      case 'BIN':
        return 'Bac';
      case 'TRUCK':
        return 'Camion';
      case 'MISSION':
        return 'Mission';
      case 'DRIVER':
        return 'Chauffeur';
      default:
        return 'Système';
    }
  }

  entityIcon(a: AlertDto): string {
    switch (this.normalizedEntityType(a)) {
      case 'BIN':
        return 'delete';
      case 'TRUCK':
        return 'local_shipping';
      case 'MISSION':
        return 'assignment';
      case 'DRIVER':
        return 'person';
      default:
        return 'warning';
    }
  }

  severityLabel(severity?: string | null): string {
    switch ((severity || '').toUpperCase()) {
      case 'CRITICAL':
        return 'Critique';
      case 'HIGH':
        return 'Élevée';
      case 'MEDIUM':
        return 'Moyenne';
      case 'LOW':
        return 'Faible';
      default:
        return severity || '—';
    }
  }

  severityClass(a: AlertDto): string {
    return `is-${(a.severity || 'low').toLowerCase()}`;
  }

  timeAgo(iso?: string | null): string {
    if (!iso) return '—';

    const d = new Date(iso).getTime();
    if (Number.isNaN(d)) return '—';

    const diff = Date.now() - d;
    const min = Math.floor(diff / 60000);

    if (min < 1) return "à l'instant";
    if (min < 60) return `il y a ${min} min`;

    const h = Math.floor(min / 60);
    if (h < 24) return `il y a ${h} h`;

    return `il y a ${Math.floor(h / 24)} j`;
  }

  private isToday(iso?: string | null): boolean {
    if (!iso) return false;

    const d = new Date(iso);

    if (Number.isNaN(d.getTime())) {
      return false;
    }

    const now = new Date();

    return (
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate()
    );
  }
}