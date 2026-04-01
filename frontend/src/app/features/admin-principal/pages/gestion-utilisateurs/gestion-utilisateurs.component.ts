import { Component, OnInit } from '@angular/core';
import { UserAdminListResponse, UserService, UserStatsResponse } from '../../../../services/user.service';

type FiltreUtilisateur = 'Tous' | 'Actifs' | 'Inactifs' | 'Chauffeurs';
type StatutUtilisateur = 'Actif' | 'Inactif';
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
}

@Component({
  selector: 'app-gestion-utilisateurs',
  templateUrl: './gestion-utilisateurs.component.html',
  styleUrls: ['./gestion-utilisateurs.component.css']
})
export class GestionUtilisateursComponent implements OnInit {
  termeRecherche: string = '';
  filtreSelectionne: FiltreUtilisateur = 'Tous';

  filtres: FiltreUtilisateur[] = ['Tous', 'Actifs', 'Inactifs', 'Chauffeurs'];

  utilisateurs: Utilisateur[] = [];
  loading = false;
  errorMessage = '';

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
      statut: user.isEnabled ? 'Actif' : 'Inactif',
      role: this.mapRole(user.role),
      derniereActivite: this.formatDerniereActivite(user.lastLoginAt)
    };
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

    if (this.termeRecherche.trim()) {
      const terme = this.termeRecherche.toLowerCase();
      resultat = resultat.filter(utilisateur =>
        utilisateur.nom.toLowerCase().includes(terme) ||
        utilisateur.courriel.toLowerCase().includes(terme) ||
        utilisateur.telephone.toLowerCase().includes(terme) ||
        utilisateur.role.toLowerCase().includes(terme)
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
    return statut === 'Actif' ? 'statut-actif' : 'statut-inactif';
  }
}