import { Component } from '@angular/core';

type StatutService = 'En cours' | 'Arrêté';

interface ServiceSysteme {
  nom: string;
  description: string;
  statut: StatutService;
  dureeFonctionnement: string;
  processeur: number;
  memoire: number;
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
export class ControleSystemeComponent {
  utilisationProcesseur = 34;
  memoireUtilisee = 6.2;
  memoireTotale = 16;
  servicesActifs = 5;
  servicesTotal = 6;
  tempsFonctionnement = '15 j 3 h';

  modeMaintenance = false;
  sauvegardeAutomatique = true;
  surveillanceTempsReel = true;

  services: ServiceSysteme[] = [
    {
      nom: 'Passerelle API',
      description: 'Service principal de routage des requêtes API',
      statut: 'En cours',
      dureeFonctionnement: '15 j 3 h 24 min',
      processeur: 12,
      memoire: 45
    },
    {
      nom: 'Traitement des données IoT',
      description: 'Traitement des données des capteurs IoT en temps réel',
      statut: 'En cours',
      dureeFonctionnement: '15 j 3 h 24 min',
      processeur: 18,
      memoire: 38
    },
    {
      nom: 'Service de notifications',
      description: 'Gestion des notifications et alertes',
      statut: 'En cours',
      dureeFonctionnement: '15 j 3 h 24 min',
      processeur: 8,
      memoire: 23
    },
    {
      nom: 'Moteur d’analyse',
      description: 'Traitement et analyse des données historiques',
      statut: 'En cours',
      dureeFonctionnement: '12 j 8 h 15 min',
      processeur: 28,
      memoire: 52
    },
    {
      nom: 'Service de sauvegarde',
      description: 'Service de sauvegarde automatique',
      statut: 'Arrêté',
      dureeFonctionnement: '0 j 0 h 0 min',
      processeur: 0,
      memoire: 0
    }
  ];

  notifications: NotificationSysteme[] = [
    {
      type: 'succes',
      titre: 'Sauvegarde automatique terminée',
      moment: 'Il y a 1 h'
    },
    {
      type: 'information',
      titre: 'Mise à jour système installée',
      moment: 'Il y a 3 h'
    },
    {
      type: 'alerte',
      titre: 'Alerte : utilisation élevée du processeur',
      moment: 'Il y a 5 h'
    }
  ];

  get classeMemoire(): number {
    return (this.memoireUtilisee / this.memoireTotale) * 100;
  }

  get classeServiceActif(): number {
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