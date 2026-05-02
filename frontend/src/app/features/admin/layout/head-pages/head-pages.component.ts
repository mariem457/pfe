import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter, map, Subscription } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../../../services/theme.service';
import { AlertService, AlertDto } from '../../../../services/alert.service';
import { RealtimeService } from '../../../../services/realtime.service';

@Component({
  selector: 'app-head-pages',
  standalone: true,
  imports: [MatIconModule, CommonModule],
  templateUrl: './head-pages.component.html',
  styleUrls: ['./head-pages.component.css']
})
export class HeadPagesComponent implements OnInit, OnDestroy {
  title = '';
  unreadCount = 0;
  latestAlerts: AlertDto[] = [];
  openMenu = false;

  private sub = new Subscription();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    public themeService: ThemeService,
    private alertService: AlertService,
    private realtimeService: RealtimeService
  ) {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        map(() => {
          let currentRoute = this.route;
          while (currentRoute.firstChild) currentRoute = currentRoute.firstChild;
          return currentRoute.snapshot.data['title'];
        })
      )
      .subscribe(title => this.title = title || '');
  }

  ngOnInit(): void {
    this.realtimeService.connectAll();

    this.alertService.getOpenAlerts().subscribe({
      next: alerts => {
        this.latestAlerts = (alerts || []).slice(0, 5);
        this.unreadCount = alerts?.length || 0;
      }
    });

    this.sub.add(
      this.alertService.realtimeAlert$.subscribe(alert => {
        if (!alert || alert.resolved) return;

        const exists = this.latestAlerts.some(a => a.id === alert.id);
        if (!exists) {
          this.latestAlerts = [alert, ...this.latestAlerts].slice(0, 5);
          this.unreadCount++;
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
    this.themeService.toggleTheme();
  }

  toggleMenu(): void {
    this.openMenu = !this.openMenu;
  }

  label(a: AlertDto): string {
    return a.binCode || a.truckCode || `${a.entityType || 'ALERT'}-${a.entityId || a.id}`;
  }
}