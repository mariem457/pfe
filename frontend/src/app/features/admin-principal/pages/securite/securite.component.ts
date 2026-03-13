import { Component } from '@angular/core';

type TypeEvenement = 'connexion' | 'echec' | 'acces-api' | 'modification' | 'deconnexion';
type StatutEvenement = 'Succès' | 'Échec';

interface EvenementSecurite {
  type: TypeEvenement;
  titre: string;
  statut: StatutEvenement;
  utilisateur: string;
  appareil: string;
  adresseIp: string;
  localisation: string;
  dateHeure: string;
}

@Component({
  selector: 'app-securite',
  templateUrl: './securite.component.html',
  styleUrls: ['./securite.component.css']
})
export class SecuriteComponent {
  ongletSelectionne: 'Logs' | 'Sessions' = 'Logs';

  doubleAuthentification = true;
  notificationsConnexion = true;
  limitationApi = true;
  detectionActiviteSuspecte = true;

  sessionsActives = 3;
  tentativesEchouees = 3;
  accesApi24h = 1247;
  authentificationDeuxFacteurs = 89;

  cleApiPrincipaleVisible = false;
  cleApiTestVisible = false;

  evenements: EvenementSecurite[] = [
    {
      type: 'connexion',
      titre: 'Connexion',
      statut: 'Succès',
      utilisateur: 'Jean Dupont',
      appareil: 'Ordinateur',
      adresseIp: '192.168.1.45',
      localisation: 'Paris, France',
      dateHeure: '2026-03-02 14:32:15'
    },
    {
      type: 'echec',
      titre: 'Échec de connexion',
      statut: 'Échec',
      utilisateur: 'Inconnu',
      appareil: 'Mobile',
      adresseIp: '45.142.212.61',
      localisation: 'Moscou, Russie',
      dateHeure: '2026-03-02 14:28:42'
    },
    {
      type: 'acces-api',
      titre: 'Accès API',
      statut: 'Succès',
      utilisateur: 'Système',
      appareil: 'Serveur',
      adresseIp: '10.0.0.12',
      localisation: 'Réseau interne',
      dateHeure: '2026-03-02 13:15:22'
    },
    {
      type: 'modification',
      titre: 'Changement de permission',
      statut: 'Succès',
      utilisateur: 'Administrateur',
      appareil: 'Ordinateur',
      adresseIp: '192.168.1.20',
      localisation: 'Paris, France',
      dateHeure: '2026-03-02 12:45:18'
    },
    {
      type: 'deconnexion',
      titre: 'Déconnexion',
      statut: 'Succès',
      utilisateur: 'Marie Martin',
      appareil: 'Tablette',
      adresseIp: '192.168.1.33',
      localisation: 'Lyon, France',
      dateHeure: '2026-03-02 11:30:05'
    }
  ];

  selectionnerOnglet(onglet: 'Logs' | 'Sessions'): void {
    this.ongletSelectionne = onglet;
  }

  basculerVisibiliteClePrincipale(): void {
    this.cleApiPrincipaleVisible = !this.cleApiPrincipaleVisible;
  }

  basculerVisibiliteCleTest(): void {
    this.cleApiTestVisible = !this.cleApiTestVisible;
  }

  obtenirClasseStatut(statut: StatutEvenement): string {
    return statut === 'Succès' ? 'statut-succes' : 'statut-echec';
  }

  obtenirClasseIcone(type: TypeEvenement): string {
    if (type === 'echec') return 'icone-echec';
    if (type === 'acces-api') return 'icone-api';
    if (type === 'modification') return 'icone-modification';
    if (type === 'deconnexion') return 'icone-deconnexion';
    return 'icone-connexion';
  }

  obtenirSymboleIcone(type: TypeEvenement): string {
    if (type === 'echec') return '⚠';
    if (type === 'acces-api') return '⌘';
    if (type === 'modification') return '🛡';
    if (type === 'deconnexion') return '◔';
    return '◉';
  }

  obtenirTexteClePrincipale(): string {
    return this.cleApiPrincipaleVisible
      ? 'sk-principale-7F4A-92KD-118X'
      : '••••••••••••••••••••••••';
  }

  obtenirTexteCleTest(): string {
    return this.cleApiTestVisible
      ? 'sk-test-2D8Q-54LM-773P'
      : '••••••••••••••••••••••••';
  }
}