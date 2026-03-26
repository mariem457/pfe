import { Component } from '@angular/core';

@Component({
  selector: 'app-battery-captures',
  templateUrl: './battery-captures.component.html',
  styleUrls: ['./battery-captures.component.css']
})
export class BatteryCapturesComponent {
  activeFilter = 'all';

  stats = [
    {
      icon: 'sensors',
      value: '1,121',
      label: 'Total Capteurs',
      badge: '',
      badgeClass: '',
      iconClass: 'blue'
    },
    {
      icon: 'battery_3_bar',
      value: '23',
      label: 'Batteries Faibles',
      badge: '+8',
      badgeClass: 'red',
      iconClass: 'yellow'
    },
    {
      icon: 'battery_alert',
      value: '7',
      label: 'Batteries Critiques',
      badge: '+3',
      badgeClass: 'red',
      iconClass: 'pink'
    },
    {
      icon: 'warning_amber',
      value: '4',
      label: 'Hors Service',
      badge: '+1',
      badgeClass: 'red',
      iconClass: 'gray'
    }
  ];

  chartLabels = ['Sem -4', 'Sem -3', 'Sem -2', 'Sem -1', 'Actuel'];

  sensors = [
    {
      id: 'SENSOR-1842',
      bin: 'BIN-1842',
      location: 'Rue de la République',
      battery: 3,
      status: 'Critique',
      fill: 45,
      updated: 'Il y a 5 min',
      maintenance: 'Urgent'
    },
    {
      id: 'SENSOR-2156',
      bin: 'BIN-2156',
      location: 'Avenue des Champs',
      battery: 18,
      status: 'Faible',
      fill: 62,
      updated: 'Il y a 28 min',
      maintenance: 'Cette semaine'
    },
    {
      id: 'SENSOR-3471',
      bin: 'BIN-3471',
      location: 'Rue Victor Hugo',
      battery: 15,
      status: 'Faible',
      fill: 28,
      updated: 'Il y a 2 h',
      maintenance: 'Cette semaine'
    },
    {
      id: 'SENSOR-0923',
      bin: 'BIN-0923',
      location: 'Boulevard Saint-Michel',
      battery: 0,
      status: 'Hors Service',
      fill: 0,
      updated: 'Il y a 3 jours',
      maintenance: 'Immédiat'
    },
    {
      id: 'SENSOR-4782',
      bin: 'BIN-4782',
      location: 'Place de la Liberté',
      battery: 8,
      status: 'Critique',
      fill: 87,
      updated: 'Il y a 45 min',
      maintenance: 'Urgent'
    },
    {
      id: 'SENSOR-5621',
      bin: 'BIN-5621',
      location: 'Rue Nationale',
      battery: 42,
      status: 'Faible',
      fill: 53,
      updated: 'Il y a 1 h',
      maintenance: 'Ce mois'
    },
    {
      id: 'SENSOR-6842',
      bin: 'BIN-6842',
      location: 'Avenue de la Gare',
      battery: 78,
      status: 'Normal',
      fill: 34,
      updated: 'Il y a 15 min',
      maintenance: '2 mois'
    },
    {
      id: 'SENSOR-7923',
      bin: 'BIN-7923',
      location: 'Rue du Commerce',
      battery: 92,
      status: 'Normal',
      fill: 67,
      updated: 'Il y a 30 min',
      maintenance: '3 mois'
    },
    {
      id: 'SENSOR-8145',
      bin: 'BIN-8145',
      location: 'Place du Marché',
      battery: 23,
      status: 'Faible',
      fill: 41,
      updated: 'Il y a 1 h 20 min',
      maintenance: 'Cette semaine'
    },
    {
      id: 'SENSOR-9267',
      bin: 'BIN-9267',
      location: 'Boulevard Haussmann',
      battery: 11,
      status: 'Critique',
      fill: 73,
      updated: 'Il y a 2 h 10 min',
      maintenance: 'Urgent'
    }
  ];

  get filteredSensors() {
    if (this.activeFilter === 'all') return this.sensors;
    if (this.activeFilter === 'critical') {
      return this.sensors.filter(s => s.status === 'Critique');
    }
    if (this.activeFilter === 'low') {
      return this.sensors.filter(s => s.status === 'Faible');
    }
    if (this.activeFilter === 'out') {
      return this.sensors.filter(s => s.status === 'Hors Service');
    }
    return this.sensors;
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
  }

  getBatteryClass(value: number): string {
    if (value === 0) return 'battery-off';
    if (value < 20) return 'battery-critical';
    if (value <= 60) return 'battery-low';
    return 'battery-good';
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Critique':
        return 'status-critical';
      case 'Faible':
        return 'status-low';
      case 'Normal':
        return 'status-normal';
      case 'Hors Service':
        return 'status-out';
      default:
        return '';
    }
  }

  getMaintenanceClass(value: string): string {
    if (value === 'Urgent' || value === 'Immédiat') return 'maint-red';
    if (value === 'Cette semaine') return 'maint-orange';
    return 'maint-gray';
  }

  getFillClass(value: number): string {
    if (value >= 70) return 'fill-red';
    if (value >= 60) return 'fill-yellow';
    return 'fill-green';
  }
}