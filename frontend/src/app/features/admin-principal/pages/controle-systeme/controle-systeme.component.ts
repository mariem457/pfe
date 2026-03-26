import { Component, OnInit } from '@angular/core';
import {
  SystemComponentResponse,
  SystemControlService,
  SystemDatabaseStatusResponse,
  SystemNotificationResponse,
  SystemOverviewResponse,
  SystemSettingsResponse
} from '../../../../services/system-control.service';

type StatutService = 'En cours' | 'Arrêté';

interface ServiceSysteme {
  nom: string;
  description: string;
  statut: StatutService;
  metric1Label: string;
  metric1Value: string;
  metric2Label: string;
  metric2Value: string;
  metric3Label: string;
  metric3Value: string;
}

interface NotificationSysteme {
  type: 'information' | 'succes' | 'alerte';
  titre: string;
  moment: string;
}

@Component({
  selector: 'app-controle-systeme',
  templateUrl: './controle-systeme.component.html',
  styleUrls: ['./controle-systeme.component.css']
})
export class ControleSystemeComponent implements OnInit {
  utilisationProcesseur = 0;
  memoireUtilisee = 0;
  memoireTotale = 0;
  servicesActifs = 0;
  servicesTotal = 0;
  tempsFonctionnement = '--';

  modeMaintenance = false;
  sauvegardeAutomatique = false;
  surveillanceTempsReel = false;

  loading = false;
  errorMessage = '';

  services: ServiceSysteme[] = [];
  notifications: NotificationSysteme[] = [];

  databaseStatus: SystemDatabaseStatusResponse = {
    activeConnections: '--',
    databaseSize: '--',
    queriesPerSecond: '--',
    lastBackup: '--'
  };

  constructor(private systemControlService: SystemControlService) {}

  ngOnInit(): void {
    this.chargerToutesLesDonnees();
  }

  chargerToutesLesDonnees(): void {
    this.loading = true;
    this.errorMessage = '';

    this.systemControlService.getOverview().subscribe({
      next: (data: SystemOverviewResponse) => {
        this.utilisationProcesseur = data.cpuUsage;
        this.memoireUtilisee = data.memoryUsedGb;
        this.memoireTotale = data.memoryTotalGb;
        this.servicesActifs = data.activeServices;
        this.servicesTotal = data.totalServices;
        this.tempsFonctionnement = data.uptime;
        this.loading = false;
      },
      error: (err: any) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger les données système';
        this.loading = false;
      }
    });

    this.systemControlService.getServices().subscribe({
      next: (data: SystemComponentResponse[]) => {
        this.services = data.map(item => ({
          nom: item.name,
          description: item.description,
          statut: item.status === 'RUNNING' ? 'En cours' : 'Arrêté',
          metric1Label: item.metric1Label,
          metric1Value: item.metric1Value,
          metric2Label: item.metric2Label,
          metric2Value: item.metric2Value,
          metric3Label: item.metric3Label,
          metric3Value: item.metric3Value
        }));
      },
      error: (err: any) => console.error(err)
    });

    this.systemControlService.getNotifications().subscribe({
      next: (data: SystemNotificationResponse[]) => {
        this.notifications = data.map(item => ({
          type: this.mapNotificationType(item.type),
          titre: item.title,
          moment: item.moment
        }));
      },
      error: (err: any) => console.error(err)
    });

    this.systemControlService.getDatabaseStatus().subscribe({
      next: (data: SystemDatabaseStatusResponse) => {
        this.databaseStatus = data;
      },
      error: (err: any) => console.error(err)
    });

    this.systemControlService.getSettings().subscribe({
      next: (data: SystemSettingsResponse) => {
        this.modeMaintenance = data.maintenanceMode;
        this.sauvegardeAutomatique = data.automaticBackup;
        this.surveillanceTempsReel = data.realtimeMonitoring;
      },
      error: (err: any) => console.error(err)
    });
  }

  onMaintenanceModeChange(): void {
    this.systemControlService.updateSettings({
      maintenanceMode: this.modeMaintenance
    }).subscribe();
  }

  onAutomaticBackupChange(): void {
    this.systemControlService.updateSettings({
      automaticBackup: this.sauvegardeAutomatique
    }).subscribe();
  }

  onRealtimeMonitoringChange(): void {
    this.systemControlService.updateSettings({
      realtimeMonitoring: this.surveillanceTempsReel
    }).subscribe();
  }

  private mapNotificationType(type: string): 'information' | 'succes' | 'alerte' {
    const value = (type || '').toUpperCase();
    if (value === 'SUCCESS') return 'succes';
    if (value === 'ALERT') return 'alerte';
    return 'information';
  }

  get classeMemoire(): number {
    if (!this.memoireTotale) return 0;
    return (this.memoireUtilisee / this.memoireTotale) * 100;
  }

  get classeServiceActif(): number {
    if (!this.servicesTotal) return 0;
    return (this.servicesActifs / this.servicesTotal) * 100;
  }

  obtenirClasseStatut(statut: StatutService): string {
    return statut === 'En cours' ? 'statut-en-cours' : 'statut-arrete';
  }

  obtenirClasseNotification(type: 'information' | 'succes' | 'alerte'): string {
    if (type === 'succes') return 'notification-succes';
    if (type === 'alerte') return 'notification-alerte';
    return 'notification-information';
  }
}