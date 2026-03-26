import { Component } from '@angular/core';

interface TechnicalAlert {
  code: string;
  severity: 'Critique' | 'Haute' | 'Moyenne';
  type: string;
  message: string;
  equipment: string;
  location: string;
  timestamp: string;
  status: 'Actives' | 'Non Acquittées' | 'Acquittées' | 'Résolues';
  icon: string;
  iconClass: string;
  highlighted?: boolean;
}

@Component({
  selector: 'app-technician-alerts',
  templateUrl: './technician-alerts.component.html',
  styleUrls: ['./technician-alerts.component.css']
})
export class TechnicianAlertsComponent {
  activeStatus = 'Actives';
  activeSeverity = 'Toutes';

  stats = [
    {
      icon: 'warning',
      value: 42,
      label: 'Alertes Actives',
      badge: '+7',
      iconClass: 'icon-red'
    },
    {
      icon: 'notifications_active',
      value: 12,
      label: 'Critiques',
      badge: '',
      iconClass: 'icon-pink'
    },
    {
      icon: 'notifications_off',
      value: 18,
      label: 'Non Acquittées',
      badge: '',
      iconClass: 'icon-yellow'
    },
    {
      icon: 'battery_6_bar',
      value: 156,
      label: 'Résolues (7j)',
      badge: '',
      iconClass: 'icon-green'
    }
  ];

  trendPoints = [18, 24, 16, 22, 28, 12, 8];
  trendLabels = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  distribution = [
    { label: 'Batterie Critique', value: 12 },
    { label: 'Batterie Faible', value: 15 },
    { label: 'Capteur Offline', value: 8 },
    { label: 'Panne Camion', value: 4 },
    { label: 'Connectivité', value: 6 },
    { label: 'Température', value: 3 }
  ];

  alerts: TechnicalAlert[] = [
    {
      code: 'ALERT-2024-542',
      severity: 'Critique',
      type: 'Batterie Critique',
      message: 'Niveau de batterie critique: 3%',
      equipment: 'SENSOR-1842',
      location: 'Rue de la République',
      timestamp: '2024-03-24 09:00:15',
      status: 'Non Acquittées',
      icon: 'battery_alert',
      iconClass: 'alert-red',
      highlighted: true
    },
    {
      code: 'ALERT-2024-541',
      severity: 'Moyenne',
      type: 'Batterie Faible',
      message: 'Batterie faible: 18%',
      equipment: 'SENSOR-2156',
      location: 'Avenue des Champs',
      timestamp: '2024-03-24 08:30:22',
      status: 'Actives',
      icon: 'battery_3_bar',
      iconClass: 'alert-yellow'
    },
    {
      code: 'ALERT-2024-540',
      severity: 'Haute',
      type: 'Capteur Offline',
      message: 'Capteur hors ligne depuis 3 jours',
      equipment: 'SENSOR-0923',
      location: 'Boulevard Saint-Michel',
      timestamp: '2024-03-21 14:45:00',
      status: 'Non Acquittées',
      icon: 'wifi_off',
      iconClass: 'alert-orange',
      highlighted: true
    },
    {
      code: 'ALERT-2024-539',
      severity: 'Critique',
      type: 'Panne Camion',
      message: 'Panne moteur détectée',
      equipment: 'TRUCK-042',
      location: 'Zone Nord',
      timestamp: '2024-03-24 08:45:30',
      status: 'Actives',
      icon: 'local_shipping',
      iconClass: 'alert-red'
    },
    {
      code: 'ALERT-2024-537',
      severity: 'Critique',
      type: 'Batterie Critique',
      message: 'Niveau de batterie critique: 8%',
      equipment: 'SENSOR-4782',
      location: 'Place de la Liberté',
      timestamp: '2024-03-24 07:15:45',
      status: 'Non Acquittées',
      icon: 'battery_alert',
      iconClass: 'alert-red',
      highlighted: true
    },
    {
      code: 'ALERT-2024-536',
      severity: 'Moyenne',
      type: 'Problème Connectivité',
      message: 'Problème de connectivité réseau',
      equipment: 'SENSOR-6234',
      location: 'Rue de la Paix',
      timestamp: '2024-03-24 06:30:12',
      status: 'Actives',
      icon: 'wifi',
      iconClass: 'alert-yellow'
    },
    {
      code: 'ALERT-2024-535',
      severity: 'Critique',
      type: 'Batterie Critique',
      message: 'Niveau de batterie critique: 11%',
      equipment: 'SENSOR-9267',
      location: 'Boulevard Haussmann',
      timestamp: '2024-03-24 05:20:33',
      status: 'Non Acquittées',
      icon: 'battery_alert',
      iconClass: 'alert-red',
      highlighted: true
    }
  ];

  setStatusFilter(status: string) {
    this.activeStatus = status;
  }

  setSeverityFilter(severity: string) {
    this.activeSeverity = severity;
  }

  get filteredAlerts(): TechnicalAlert[] {
    return this.alerts.filter(alert => {
      const statusOk =
        this.activeStatus === 'Actives' ? alert.status === 'Actives' || alert.status === 'Non Acquittées'
        : alert.status === this.activeStatus;

      const severityOk =
        this.activeSeverity === 'Toutes' || alert.severity === this.activeSeverity;

      return statusOk && severityOk;
    });
  }

  getSeverityClass(severity: string): string {
    switch (severity) {
      case 'Critique':
        return 'severity-critical';
      case 'Haute':
        return 'severity-high';
      case 'Moyenne':
        return 'severity-medium';
      default:
        return '';
    }
  }

  getStatusChipClass(status: string): string {
    switch (status) {
      case 'Non Acquittées':
        return 'status-unacked';
      case 'Acquittées':
        return 'status-acked';
      case 'Résolues':
        return 'status-resolved';
      default:
        return '';
    }
  }

  showAcknowledge(alert: TechnicalAlert): boolean {
    return alert.status === 'Non Acquittées';
  }
}