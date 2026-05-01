import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent {
  stats = [
    {
      icon: '⚠',
      value: 12,
      label: 'Incidents Ouverts',
      badge: '+3',
      badgeClass: 'red',
      iconClass: 'danger'
    },
    {
      icon: '🚚',
      value: 4,
      label: 'Camions en Panne',
      badge: '+1',
      badgeClass: 'red',
      iconClass: 'warning'
    },
    {
      icon: '🔋',
      value: 23,
      label: 'Batteries Faibles',
      badge: '+8',
      badgeClass: 'red',
      iconClass: 'warning'
    },
    {
      icon: '📟',
      value: 7,
      label: 'Capteurs Critiques',
      badge: '-2',
      badgeClass: 'green',
      iconClass: 'danger'
    }
  ];

  interventionBars = [50, 75, 95, 65, 88, 40, 28];
  interventionLabels = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  recentAlerts = [
    {
      title: 'SENSOR-1842',
      type: 'Batterie Critique',
      location: 'Rue de la République',
      time: 'Il y a 5 min',
      rightLabel: '3%',
      chipClass: 'chip-red',
      rightClass: 'text-red'
    },
    {
      title: 'TRUCK-042',
      type: 'Incident Camion',
      location: 'Zone Nord',
      time: 'Il y a 12 min',
      rightLabel: 'En Panne',
      chipClass: 'chip-orange',
      rightClass: 'pill-red'
    },
    {
      title: 'SENSOR-2156',
      type: 'Batterie Faible',
      location: 'Avenue des Champs',
      time: 'Il y a 28 min',
      rightLabel: '18%',
      chipClass: 'chip-yellow',
      rightClass: 'text-red'
    },
    {
      title: 'SENSOR-0923',
      type: 'Capteur Hors Service',
      location: 'Boulevard Saint-Michel',
      time: 'Il y a 1 h',
      rightLabel: 'Hors Service',
      chipClass: 'chip-orange',
      rightClass: 'pill-red'
    },
    {
      title: 'SENSOR-3471',
      type: 'Batterie Faible',
      location: 'Rue Victor Hugo',
      time: 'Il y a 2 h',
      rightLabel: '15%',
      chipClass: 'chip-yellow',
      rightClass: 'text-red'
    }
  ];

  priorityItems = [
    {
      title: 'SENSOR-1842',
      badge: 'Critique',
      badgeClass: 'status-red',
      subtitle: 'Capteur Batterie',
      location: 'Rue de la République',
      problem: 'Batterie 3%'
    },
    {
      title: 'TRUCK-042',
      badge: 'Haute',
      badgeClass: 'status-gray',
      subtitle: 'Camion',
      location: 'Zone Nord',
      problem: 'Panne moteur'
    },
    {
      title: 'SENSOR-0923',
      badge: 'Haute',
      badgeClass: 'status-gray',
      subtitle: 'Capteur',
      location: 'Boulevard Saint-Michel',
      problem: 'Hors service'
    },
    {
      title: 'SENSOR-2156',
      badge: 'Moyenne',
      badgeClass: 'status-green',
      subtitle: 'Capteur Batterie',
      location: 'Avenue des Champs',
      problem: 'Batterie 18%'
    }
  ];

}
