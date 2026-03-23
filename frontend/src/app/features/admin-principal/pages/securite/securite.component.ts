import { Component, OnInit } from '@angular/core';
import {
  ApiKeyResponse,
  SecurityDashboardResponse,
  SecurityEventResponse,
  SecurityService,
  SecuritySettingsResponse
} from '../../../../services/security.service';

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
export class SecuriteComponent implements OnInit {
  ongletSelectionne: 'Logs' | 'Sessions' = 'Logs';

  doubleAuthentification = true;
  notificationsConnexion = true;
  limitationApi = true;
  detectionActiviteSuspecte = true;

  sessionsActives = 0;
  tentativesEchouees = 0;
  accesApi24h = 0;
  authentificationDeuxFacteurs = 0;

  alerteMessage = 'Aucune alerte de sécurité critique détectée au cours des dernières 24 heures.';

  cleApiPrincipaleVisible = false;
  cleApiTestVisible = false;

  cleApiPrincipale = '••••••••••••••••••••••••';
  cleApiTest = '••••••••••••••••••••••••';

  evenements: EvenementSecurite[] = [];

  loading = false;
  errorMessage = '';

  constructor(private securityService: SecurityService) {}

  ngOnInit(): void {
    this.chargerDonnees();
  }

  chargerDonnees(): void {
    this.loading = true;
    this.errorMessage = '';

    this.securityService.getDashboard().subscribe({
      next: (data: SecurityDashboardResponse) => {
        this.sessionsActives = data.sessionsActives;
        this.tentativesEchouees = data.tentativesEchouees24h;
        this.accesApi24h = data.accesApi24h;
        this.authentificationDeuxFacteurs = data.authentificationDeuxFacteurs;
        this.alerteMessage = data.alerteMessage;
        this.loading = false;
      },
      error: (err: any) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger les données de sécurité';
        this.loading = false;
      }
    });

    this.securityService.getEvents().subscribe({
      next: (data: SecurityEventResponse[]) => {
        this.evenements = data;
      },
      error: (err: any) => console.error(err)
    });

    this.securityService.getSettings().subscribe({
      next: (data: SecuritySettingsResponse) => {
        this.doubleAuthentification = data.doubleAuthentification;
        this.notificationsConnexion = data.notificationsConnexion;
        this.limitationApi = data.limitationApi;
        this.detectionActiviteSuspecte = data.detectionActiviteSuspecte;
      },
      error: (err: any) => console.error(err)
    });

    this.securityService.getApiKeys().subscribe({
      next: (data: ApiKeyResponse[]) => {
        const principale = data.find(k => !k.isTest);
        const test = data.find(k => k.isTest);

        this.cleApiPrincipale = principale?.keyValue ?? 'Aucune clé';
        this.cleApiTest = test?.keyValue ?? 'Aucune clé';
      },
      error: (err: any) => console.error(err)
    });
  }

  selectionnerOnglet(onglet: 'Logs' | 'Sessions'): void {
    this.ongletSelectionne = onglet;
  }

  basculerVisibiliteClePrincipale(): void {
    this.cleApiPrincipaleVisible = !this.cleApiPrincipaleVisible;
  }

  basculerVisibiliteCleTest(): void {
    this.cleApiTestVisible = !this.cleApiTestVisible;
  }

  onDoubleAuthentificationChange(): void {
    this.securityService.updateSettings({
      doubleAuthentification: this.doubleAuthentification
    }).subscribe();
  }

  onNotificationsConnexionChange(): void {
    this.securityService.updateSettings({
      notificationsConnexion: this.notificationsConnexion
    }).subscribe();
  }

  onLimitationApiChange(): void {
    this.securityService.updateSettings({
      limitationApi: this.limitationApi
    }).subscribe();
  }

  onDetectionActiviteSuspecteChange(): void {
    this.securityService.updateSettings({
      detectionActiviteSuspecte: this.detectionActiviteSuspecte
    }).subscribe();
  }

  genererNouvelleCle(testKey: boolean): void {
    this.securityService.generateApiKey(testKey).subscribe({
      next: () => this.chargerDonnees(),
      error: (err: any) => console.error(err)
    });
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
      ? this.cleApiPrincipale
      : '••••••••••••••••••••••••';
  }

  obtenirTexteCleTest(): string {
    return this.cleApiTestVisible
      ? this.cleApiTest
      : '••••••••••••••••••••••••';
  }
}