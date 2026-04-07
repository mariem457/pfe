import { Component, OnInit } from '@angular/core';
import {
  AccountStatus,
  UserAdminListResponse,
  UserService,
  UserStatsResponse
} from '../../../../services/user.service';

type FiltreUtilisateur = 'Tous' | 'Actifs' | 'Inactifs' | 'Chauffeurs' | 'En attente';
type StatutUtilisateur = 'Actif' | 'Inactif' | 'En attente' | 'Refusé';
type RoleUtilisateur = 'Administrateur' | 'Chauffeur' | 'Superviseur' | 'Observateur';

interface Utilisateur {
  id: number;
  initiales: string;
  nom: string;
  courriel: string;
  telephone: string;
  statut: StatutUtilisateur;
  role: RoleUtilisateur;
  derniereActivite: string;
  accountStatus?: AccountStatus;
  isEnabled: boolean;
}

@Component({
  selector: 'app-gestion-utilisateurs',
  templateUrl: './gestion-utilisateurs.component.html',
  styleUrls: ['./gestion-utilisateurs.component.css']
})
export class GestionUtilisateursComponent implements OnInit {
  termeRecherche: string = '';
  filtreSelectionne: FiltreUtilisateur = 'Tous';

  filtres: FiltreUtilisateur[] = ['Tous', 'Actifs', 'Inactifs', 'Chauffeurs', 'En attente'];

  utilisateurs: Utilisateur[] = [];
  loading = false;
  errorMessage = '';
  actionLoadingId: number | null = null;

  stats: UserStatsResponse = {
    totalUsers: 0,
    activeUsers: 0,
    inactiveUsers: 0,
    drivers: 0
  };

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.chargerUtilisateurs();
    this.chargerStats();
  }

  chargerUtilisateurs(): void {
    this.loading = true;
    this.errorMessage = '';

    this.userService.getUsers().subscribe({
      next: (data) => {
        this.utilisateurs = data.map(user => this.mapUserToUtilisateur(user));
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement utilisateurs', err);
        this.errorMessage = 'Impossible de charger les utilisateurs depuis le backend';
        this.loading = false;
      }
    });
  }

  chargerStats(): void {
    this.userService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (err: any) => {
        console.error('Erreur chargement stats utilisateurs', err);
      }
    });
  }

  private mapUserToUtilisateur(user: UserAdminListResponse): Utilisateur {
    return {
      id: user.id,
      initiales: this.getInitiales(user.fullName || user.username || 'U'),
      nom: user.fullName || user.username || 'Utilisateur',
      courriel: user.email || '--',
      telephone: user.phone || '--',
      statut: this.mapStatut(user),
      role: this.mapRole(user.role),
      derniereActivite: this.formatDerniereActivite(user.lastLoginAt),
      accountStatus: user.accountStatus,
      isEnabled: user.isEnabled
    };
  }

  private mapStatut(user: UserAdminListResponse): StatutUtilisateur {
    const role = (user.role || '').toUpperCase();
    const accountStatus = (user.accountStatus || '').toUpperCase();

    if (role === 'DRIVER') {
      if (accountStatus === 'PENDING') return 'En attente';
      if (accountStatus === 'REJECTED') return 'Refusé';
      if (accountStatus === 'APPROVED' && user.isEnabled) return 'Actif';
      return 'Inactif';
    }

    return user.isEnabled ? 'Actif' : 'Inactif';
  }

  private getInitiales(nom: string): string {
    return nom
      .split(' ')
      .filter(part => part.trim().length > 0)
      .slice(0, 2)
      .map(part => part.charAt(0).toUpperCase())
      .join('');
  }

  private mapRole(role: string): RoleUtilisateur {
    const r = (role || '').toUpperCase();

    if (r === 'ADMIN') return 'Administrateur';
    if (r === 'DRIVER') return 'Chauffeur';
    if (r === 'MUNICIPALITY') return 'Superviseur';

    return 'Observateur';
  }

  private formatDerniereActivite(lastLoginAt?: string): string {
    if (!lastLoginAt) {
      return 'Jamais connecté';
    }

    const lastDate = new Date(lastLoginAt);
    if (isNaN(lastDate.getTime())) {
      return 'Activité inconnue';
    }

    const now = new Date();
    const diffMs = now.getTime() - lastDate.getTime();

    const minutes = Math.floor(diffMs / (1000 * 60));
    const hours = Math.floor(diffMs / (1000 * 60 * 60));
    const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (minutes < 1) return 'À l’instant';
    if (minutes < 60) return `Il y a ${minutes} min`;
    if (hours < 24) return `Il y a ${hours} h`;
    return `Il y a ${days} jour${days > 1 ? 's' : ''}`;
  }

  selectionnerFiltre(filtre: FiltreUtilisateur): void {
    this.filtreSelectionne = filtre;
  }

  approveDriver(utilisateur: Utilisateur): void {
    if (!this.peutEtreValide(utilisateur)) {
      return;
    }

    this.actionLoadingId = utilisateur.id;
    this.errorMessage = '';

    this.userService.approveDriver(utilisateur.id).subscribe({
      next: () => {
        this.actionLoadingId = null;
        this.chargerUtilisateurs();
        this.chargerStats();
      },
      error: (err) => {
        console.error('Erreur approve driver', err);
        this.errorMessage = 'Impossible de valider ce chauffeur.';
        this.actionLoadingId = null;
      }
    });
  }

  rejectDriver(utilisateur: Utilisateur): void {
    if (!this.peutEtreRefuse(utilisateur)) {
      return;
    }

    this.actionLoadingId = utilisateur.id;
    this.errorMessage = '';

    this.userService.rejectDriver(utilisateur.id).subscribe({
      next: () => {
        this.actionLoadingId = null;
        this.chargerUtilisateurs();
        this.chargerStats();
      },
      error: (err) => {
        console.error('Erreur reject driver', err);
        this.errorMessage = 'Impossible de refuser ce chauffeur.';
        this.actionLoadingId = null;
      }
    });
  }

  peutEtreValide(utilisateur: Utilisateur): boolean {
    return utilisateur.role === 'Chauffeur' && utilisateur.accountStatus === 'PENDING';
  }

  peutEtreRefuse(utilisateur: Utilisateur): boolean {
    return utilisateur.role === 'Chauffeur' && utilisateur.accountStatus === 'PENDING';
  }

  estEnCoursAction(utilisateur: Utilisateur): boolean {
    return this.actionLoadingId === utilisateur.id;
  }

  get nombreTotalUtilisateurs(): number {
    return this.stats.totalUsers;
  }

  get nombreUtilisateursActifs(): number {
    return this.stats.activeUsers;
  }

  get nombreUtilisateursInactifs(): number {
    return this.stats.inactiveUsers;
  }

  get nombreChauffeurs(): number {
    return this.stats.drivers;
  }

  get utilisateursFiltres(): Utilisateur[] {
    let resultat = [...this.utilisateurs];

    if (this.filtreSelectionne === 'Actifs') {
      resultat = resultat.filter(utilisateur => utilisateur.statut === 'Actif');
    }

    if (this.filtreSelectionne === 'Inactifs') {
      resultat = resultat.filter(utilisateur => utilisateur.statut === 'Inactif');
    }

    if (this.filtreSelectionne === 'Chauffeurs') {
      resultat = resultat.filter(utilisateur => utilisateur.role === 'Chauffeur');
    }

    if (this.filtreSelectionne === 'En attente') {
      resultat = resultat.filter(utilisateur => utilisateur.statut === 'En attente');
    }

    if (this.termeRecherche.trim()) {
      const terme = this.termeRecherche.toLowerCase();
      resultat = resultat.filter(utilisateur =>
        utilisateur.nom.toLowerCase().includes(terme) ||
        utilisateur.courriel.toLowerCase().includes(terme) ||
        utilisateur.telephone.toLowerCase().includes(terme) ||
        utilisateur.role.toLowerCase().includes(terme) ||
        utilisateur.statut.toLowerCase().includes(terme)
      );
    }

    return resultat;
  }

  obtenirClasseRole(role: RoleUtilisateur): string {
    if (role === 'Administrateur') return 'role-administrateur';
    if (role === 'Chauffeur') return 'role-chauffeur';
    if (role === 'Superviseur') return 'role-superviseur';
    return 'role-observateur';
  }

  obtenirClasseStatut(statut: StatutUtilisateur): string {
    if (statut === 'Actif') return 'statut-actif';
    if (statut === 'En attente') return 'statut-en-attente';
    if (statut === 'Refusé') return 'statut-refuse';
    return 'statut-inactif';
  }
}