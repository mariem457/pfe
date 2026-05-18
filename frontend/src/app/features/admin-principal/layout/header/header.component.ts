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

        const exists = this.latestAlerts.some(a => a.id === alert.id);
        if (!exists) {
          this.latestAlerts = [alert, ...this.latestAlerts].slice(0, 5);
          this.unreadCount += 1;
        }
      })
    );

    this.sub.add(
      this.alertService.realtimeResolved$.subscribe(alert => {
        this.latestAlerts = this.latestAlerts.filter(a => a.id !== alert.id);
        this.unreadCount = Math.max(0, this.unreadCount - 1);
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
  }

  label(alert: AlertDto): string {
    return alert.binCode || alert.truckCode || `${alert.entityType || 'ALERTE'}-${alert.entityId || alert.id}`;
  }

  private loadAlerts(): void {
    this.alertService.getOpenAlerts().subscribe({
      next: alerts => {
        const list = alerts || [];
        this.latestAlerts = list.slice(0, 5);
        this.unreadCount = list.length;
      },
      error: err => {
        console.error('Admin principal header alerts error:', err);
        this.latestAlerts = [];
        this.unreadCount = 0;
      }
    });
  }
}
