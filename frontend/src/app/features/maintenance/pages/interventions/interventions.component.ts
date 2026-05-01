import { Component } from '@angular/core';

interface InterventionTask {
  code: string;
  asset: string;
  assetType: string;
  title: string;
  status: 'En Attente' | 'En Cours' | 'Complétée' | 'Planifiée';
  priority: 'Critique' | 'Haute' | 'Moyenne';
  assignedTo: string;
  plannedDate: string;
  duration: string;
  location: string;
  progress?: number;
  completedDate?: string;
}

@Component({
  selector: 'app-interventions',
  templateUrl: './interventions.component.html',
  styleUrls: ['./interventions.component.css']
})
export class InterventionsComponent {
  activeStatus = 'Tous';
  activePriority = 'Toutes';

  stats = [
    {
      icon: 'assignment',
      value: 127,
      label: 'Total Tâches',
      iconClass: 'icon-blue',
      badge: ''
    },
    {
      icon: 'schedule',
      value: 18,
      label: 'En Attente',
      iconClass: 'icon-yellow',
      badge: '+4'
    },
    {
      icon: 'build',
      value: 9,
      label: 'En Cours',
      iconClass: 'icon-indigo',
      badge: ''
    },
    {
      icon: 'check_circle',
      value: 42,
      label: 'Complétées (7j)',
      iconClass: 'icon-green',
      badge: ''
    }
  ];

  chartBars = [8, 12, 10, 15, 9, 5, 3];
  chartLabels = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  tasks: InterventionTask[] = [
    {
      code: 'TASK-2024-158',
      asset: 'SENSOR-1842',
      assetType: 'Capteur',
      title: 'Remplacement batterie',
      status: 'En Attente',
      priority: 'Critique',
      assignedTo: 'Non assigné',
      plannedDate: '2024-03-25',
      duration: '30 min',
      location: 'Rue de la République'
    },
    {
      code: 'TASK-2024-157',
      asset: 'TRUCK-042',
      assetType: 'Camion',
      title: 'Réparation moteur',
      status: 'En Cours',
      priority: 'Haute',
      assignedTo: 'Pierre Martin',
      plannedDate: '2024-03-24',
      duration: '2 jours',
      location: 'Atelier Central',
      progress: 45
    },
    {
      code: 'TASK-2024-156',
      asset: 'SENSOR-3471',
      assetType: 'Capteur',
      title: 'Remplacement batterie',
      status: 'En Attente',
      priority: 'Moyenne',
      assignedTo: 'Luc Fontaine',
      plannedDate: '2024-03-26',
      duration: '30 min',
      location: 'Rue Victor Hugo'
    },
    {
      code: 'TASK-2024-155',
      asset: 'TRUCK-038',
      assetType: 'Camion',
      title: 'Réparation système hydraulique',
      status: 'En Cours',
      priority: 'Haute',
      assignedTo: 'Pierre Martin',
      plannedDate: '2024-03-23',
      duration: '1 jour',
      location: 'Atelier Central',
      progress: 70
    },
    {
      code: 'TASK-2024-154',
      asset: 'SENSOR-0923',
      assetType: 'Capteur',
      title: 'Diagnostic et réparation',
      status: 'En Attente',
      priority: 'Haute',
      assignedTo: 'Antoine Blanc',
      plannedDate: '2024-03-25',
      duration: '1 heure',
      location: 'Boulevard Saint-Michel'
    },
    {
      code: 'TASK-2024-153',
      asset: 'TRUCK-035',
      assetType: 'Camion',
      title: 'Changement pneu',
      status: 'Complétée',
      priority: 'Moyenne',
      assignedTo: 'Luc Fontaine',
      plannedDate: '2024-03-22',
      duration: '1 heure',
      location: 'Atelier Zone Ouest',
      completedDate: '2024-03-22'
    },
    {
      code: 'TASK-2024-152',
      asset: 'SENSOR-2156',
      assetType: 'Capteur',
      title: 'Contrôle niveau batterie',
      status: 'Planifiée',
      priority: 'Moyenne',
      assignedTo: 'Sami Ben Ali',
      plannedDate: '2024-03-28',
      duration: '20 min',
      location: 'Avenue des Champs'
    }
  ];

  setStatusFilter(status: string) {
    this.activeStatus = status;
  }

  setPriorityFilter(priority: string) {
    this.activePriority = priority;
  }

  get filteredTasks(): InterventionTask[] {
    return this.tasks.filter(task => {
      const statusOk =
        this.activeStatus === 'Tous' || task.status === this.activeStatus;
      const priorityOk =
        this.activePriority === 'Toutes' || task.priority === this.activePriority;

      return statusOk && priorityOk;
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'En Attente':
        return 'status-waiting';
      case 'En Cours':
        return 'status-progress';
      case 'Complétée':
        return 'status-done';
      case 'Planifiée':
        return 'status-planned';
      default:
        return '';
    }
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'Critique':
        return 'priority-critical';
      case 'Haute':
        return 'priority-high';
      case 'Moyenne':
        return 'priority-medium';
      default:
        return '';
    }
  }

  getActionLabel(task: InterventionTask): string {
    if (task.status === 'En Attente') return 'Commencer';
    if (task.status === 'En Cours') return 'Compléter';
    return 'Voir Détails';
  }

  isPrimaryAction(task: InterventionTask): boolean {
    return task.status === 'En Attente' || task.status === 'En Cours';
  }
}