import { Component, OnInit } from '@angular/core';
import { MaintenanceDashboardService } from '../../../../services/maintenance-dashboard.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  title = 'Espace Maintenance';
  isDark = false;
  alertCount = 0;
  alertsOpen = false;
  latestAlerts: any[] = [];

  constructor(private maintenanceService: MaintenanceDashboardService) {}

  ngOnInit(): void {
    this.isDark = document.body.classList.contains('dark-mode');
    this.loadAlertCount();
  }

  loadAlertCount(): void {
    this.maintenanceService.getAlerts().subscribe({
      next: (alerts) => {
        const list = alerts || [];
        this.alertCount = list.length;
        this.latestAlerts = list.slice(0, 5);
      },
      error: () => {
        this.alertCount = 0;
        this.latestAlerts = [];
      }
    });
  }

  toggleAlerts(): void {
    this.alertsOpen = !this.alertsOpen;
  }

  getAlertTitle(alert: any): string {
    return alert?.title || alert?.alertTitle || alert?.binCode || 'Alerte technique';
  }

  getAlertMessage(alert: any): string {
    return alert?.message || alert?.description || alert?.alertType || 'Intervention nécessaire.';
  }

  getAlertSeverity(alert: any): string {
    return (alert?.severity || 'MEDIUM').toString().toUpperCase();
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    document.body.classList.toggle('dark-mode', this.isDark);
    localStorage.setItem('theme', this.isDark ? 'dark' : 'light');
  }
}
