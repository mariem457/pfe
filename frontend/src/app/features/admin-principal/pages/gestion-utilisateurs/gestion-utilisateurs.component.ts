import { Component, OnInit } from '@angular/core';
import {
  AccountStatus,
  UserAdminListResponse,
  UserService
} from '../../../../services/user.service';

type RoleUtilisateur =
  | 'Chauffeur'
  | 'Maintenance'
  | 'Municipalité'
  | 'Administrateur'
  | 'Inconnu';

type StatutCompte = 'En attente' | 'Validé' | 'Refusé' | 'Désactivé';

type FiltreUtilisateur =
  | 'Tous'
  | 'Chauffeurs'
  | 'Chauffeurs en attente'
  | 'Maintenance'
  | 'Municipalité';

type ModalMode = 'details' | 'confirm';
type ConfirmAction = 'validate' | 'reject' | 'disable' | 'delete' | null;
interface Utilisateur {
  id: number;
  username: string;
  initiales: string;
  nom: string;
  email: string;
  telephone: string;
  role: RoleUtilisateur;
  statut: StatutCompte;
  dateInscription: string;
  derniereConnexion: string;
  accountStatus?: AccountStatus;
  isEnabled: boolean;
}

@Component({
  selector: 'app-gestion-utilisateurs',
  templateUrl: './gestion-utilisateurs.component.html',
  styleUrls: ['./gestion-utilisateurs.component.css']
})
export class GestionUtilisateursComponent implements OnInit {
  utilisateurs: Utilisateur[] = [];

  termeRecherche = '';
  filtreSelectionne: FiltreUtilisateur = 'Tous';

  filtres: FiltreUtilisateur[] = [
    'Tous',
    'Chauffeurs',
    'Chauffeurs en attente',
    'Maintenance',
    'Municipalité'
  ];

  loading = false;
  actionLoading = false;
  errorMessage = '';
  successMessage = '';

  showModal = false;
  modalMode: ModalMode = 'details';
  confirmAction: ConfirmAction = null;
  selectedUtilisateur: Utilisateur | null = null;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.chargerUtilisateurs();
  }

  chargerUtilisateurs(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.getUsers().subscribe({
      next: (users: UserAdminListResponse[]) => {
        this.utilisateurs = users.map((user) => this.mapUserToUtilisateur(user));
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement utilisateurs', err);
        this.errorMessage = this.getBackendErrorMessage(
          err,
          'Impossible de charger les utilisateurs.'
        );
        this.loading = false;
      }
    });
  }

private mapUserToUtilisateur(user: UserAdminListResponse): Utilisateur {
  const username = user.username || 'utilisateur';
  const fullName = user.fullName || username;

  return {
    id: user.id,
    username,
    initiales: this.getInitiales(username),
    nom: fullName,
    email: user.email || '--',
    telephone: user.phone && user.phone.trim() ? user.phone : 'Non renseigné',
    role: this.mapRole(user.role),
    statut: this.mapStatut(user),
    dateInscription: this.formatDate(
      user.createdAt || user.registrationDate || user.created_at
    ),
    derniereConnexion: this.formatDerniereConnexion(user.lastLoginAt),
    accountStatus: user.accountStatus,
    isEnabled: user.isEnabled
  };
}

  private mapRole(role: string): RoleUtilisateur {
    const r = (role || '').toUpperCase();

    if (r === 'DRIVER') return 'Chauffeur';
    if (r === 'MAINTENANCE' || r === 'MAINTENANCE_AGENT') return 'Maintenance';

    if (
      r === 'MUNICIPALITY' ||
      r === 'MUNICIPAL_AGENT' ||
      r === 'AGENT_MUNICIPAL'
    ) {
      return 'Municipalité';
    }

    if (r === 'ADMIN') return 'Administrateur';

    return 'Inconnu';
  }

  private mapStatut(user: UserAdminListResponse): StatutCompte {
    const role = (user.role || '').toUpperCase();
    const accountStatus = (user.accountStatus || '').toUpperCase();

    if (role === 'DRIVER') {
      if (accountStatus === 'PENDING') return 'En attente';
      if (accountStatus === 'REJECTED') return 'Refusé';
      if (accountStatus === 'APPROVED') {
        return user.isEnabled ? 'Validé' : 'Désactivé';
      }
    }

    return user.isEnabled ? 'Validé' : 'Désactivé';
  }

  selectionnerFiltre(filtre: FiltreUtilisateur): void {
    this.filtreSelectionne = filtre;
  }

  get utilisateursFiltres(): Utilisateur[] {
    let resultat = [...this.utilisateurs];

    if (this.filtreSelectionne === 'Chauffeurs') {
      resultat = resultat.filter((u) => u.role === 'Chauffeur');
    }

    if (this.filtreSelectionne === 'Chauffeurs en attente') {
      resultat = resultat.filter(
        (u) => u.role === 'Chauffeur' && u.statut === 'En attente'
      );
    }

    if (this.filtreSelectionne === 'Maintenance') {
      resultat = resultat.filter((u) => u.role === 'Maintenance');
    }

    if (this.filtreSelectionne === 'Municipalité') {
      resultat = resultat.filter((u) => u.role === 'Municipalité');
    }

    const terme = this.termeRecherche.trim().toLowerCase();

    if (terme) {
      resultat = resultat.filter((u) =>
        u.nom.toLowerCase().includes(terme) ||
        u.email.toLowerCase().includes(terme) ||
        u.telephone.toLowerCase().includes(terme) ||
        u.role.toLowerCase().includes(terme) ||
        u.statut.toLowerCase().includes(terme)
      );
    }

    return resultat;
  }

  get totalUtilisateurs(): number {
    return this.utilisateurs.length;
  }

  get totalChauffeurs(): number {
    return this.utilisateurs.filter((u) => u.role === 'Chauffeur').length;
  }

  get chauffeursEnAttente(): number {
    return this.utilisateurs.filter(
      (u) => u.role === 'Chauffeur' && u.statut === 'En attente'
    ).length;
  }

  get totalMaintenance(): number {
    return this.utilisateurs.filter((u) => u.role === 'Maintenance').length;
  }

  get totalMunicipalite(): number {
    return this.utilisateurs.filter((u) => u.role === 'Municipalité').length;
  }

  get titreFiltreActuel(): string {
    if (this.filtreSelectionne === 'Chauffeurs') return 'Tous les chauffeurs';
    if (this.filtreSelectionne === 'Chauffeurs en attente') {
      return 'Chauffeurs en attente d’acceptation';
    }
    if (this.filtreSelectionne === 'Maintenance') return 'Compte de maintenance';
    if (this.filtreSelectionne === 'Municipalité') return 'Compte municipal';

    return 'Tous les utilisateurs';
  }

  openDetails(utilisateur: Utilisateur): void {
    this.selectedUtilisateur = utilisateur;
    this.modalMode = 'details';
    this.showModal = true;
  }

  openConfirm(action: ConfirmAction, utilisateur: Utilisateur): void {
    this.selectedUtilisateur = utilisateur;
    this.confirmAction = action;
    this.modalMode = 'confirm';
    this.showModal = true;
  }

  closeModal(): void {
    if (this.actionLoading) return;

    this.showModal = false;
    this.selectedUtilisateur = null;
    this.confirmAction = null;
  }

  get confirmationTitle(): string {
    if (this.confirmAction === 'validate') return 'Valider le chauffeur';
    if (this.confirmAction === 'reject') return 'Refuser le chauffeur';
    if (this.confirmAction === 'disable') return 'Désactiver le chauffeur';
    if (this.confirmAction === 'delete') return 'Supprimer le chauffeur';
    return 'Confirmation';
  }

  get confirmationMessage(): string {
    if (this.confirmAction === 'validate') {
      return 'Voulez-vous vraiment valider ce compte chauffeur ?';
    }

    if (this.confirmAction === 'reject') {
      return 'Voulez-vous vraiment refuser ce compte chauffeur ?';
    }

    if (this.confirmAction === 'disable') {
      return 'Voulez-vous vraiment désactiver ce compte chauffeur ?';
    }

    if (this.confirmAction === 'delete') {
      return 'Voulez-vous vraiment supprimer ce compte chauffeur ?';
    }

    return '';
  }

  confirmerAction(): void {
    if (!this.selectedUtilisateur || !this.confirmAction) return;

    if (this.confirmAction === 'validate') {
      this.validerChauffeur(this.selectedUtilisateur);
      return;
    }

    if (this.confirmAction === 'reject') {
      this.refuserChauffeur(this.selectedUtilisateur);
      return;
    }

    if (this.confirmAction === 'disable') {
      this.desactiverChauffeur(this.selectedUtilisateur);
      return;
    }

    if (this.confirmAction === 'delete') {
      this.supprimerChauffeur(this.selectedUtilisateur);
    }
  }

  private validerChauffeur(utilisateur: Utilisateur): void {
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.approveDriver(utilisateur.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.closeModal();
        this.successMessage = 'Compte chauffeur validé avec succès.';
        this.chargerUtilisateurs();
      },
      error: (err: any) => {
        console.error('Erreur validation chauffeur', err);
        this.actionLoading = false;
        this.errorMessage = this.getBackendErrorMessage(
          err,
          'Impossible de valider ce chauffeur.'
        );
        this.closeModal();
      }
    });
  }

  private refuserChauffeur(utilisateur: Utilisateur): void {
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.rejectDriver(utilisateur.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.closeModal();
        this.successMessage = 'Compte chauffeur refusé avec succès.';
        this.chargerUtilisateurs();
      },
      error: (err: any) => {
        console.error('Erreur refus chauffeur', err);
        this.actionLoading = false;
        this.errorMessage = this.getBackendErrorMessage(
          err,
          'Impossible de refuser ce chauffeur.'
        );
        this.closeModal();
      }
    });
  }

  private desactiverChauffeur(utilisateur: Utilisateur): void {
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.disableDriver(utilisateur.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.closeModal();
        this.successMessage = 'Compte chauffeur désactivé avec succès.';
        this.chargerUtilisateurs();
      },
      error: (err: any) => {
        console.error('Erreur désactivation chauffeur', err);
        this.actionLoading = false;
        this.errorMessage = this.getBackendErrorMessage(
          err,
          'Impossible de désactiver ce chauffeur.'
        );
        this.closeModal();
      }
    });
  }

  private supprimerChauffeur(utilisateur: Utilisateur): void {
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.deleteDriver(utilisateur.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.closeModal();
        this.successMessage = 'Compte chauffeur supprimé avec succès.';
        this.chargerUtilisateurs();
      },
      error: (err: any) => {
        console.error('Erreur suppression chauffeur', err);
        this.actionLoading = false;
        this.errorMessage = this.getBackendErrorMessage(
          err,
          'Impossible de supprimer ce chauffeur.'
        );
        this.closeModal();
      }
    });
  }

  peutValiderOuRefuser(utilisateur: Utilisateur): boolean {
    return utilisateur.role === 'Chauffeur' && utilisateur.statut === 'En attente';
  }

  peutDesactiverOuSupprimer(utilisateur: Utilisateur): boolean {
    return utilisateur.role === 'Chauffeur' && utilisateur.statut === 'Validé';
  }

  peutSupprimerRefuse(utilisateur: Utilisateur): boolean {
    return utilisateur.role === 'Chauffeur' && utilisateur.statut === 'Refusé';
  }

  obtenirClasseRole(role: RoleUtilisateur): string {
    if (role === 'Chauffeur') return 'role-driver';
    if (role === 'Maintenance') return 'role-maintenance';
    if (role === 'Municipalité') return 'role-municipality';
    if (role === 'Administrateur') return 'role-admin';

    return 'role-unknown';
  }

  obtenirClasseStatut(statut: StatutCompte): string {
    if (statut === 'En attente') return 'statut-pending';
    if (statut === 'Validé') return 'statut-approved';
    if (statut === 'Refusé') return 'statut-rejected';

    return 'statut-disabled';
  }

  private getInitiales(nom: string): string {
    const initials = nom
      .split(' ')
      .filter((p) => p.trim().length > 0)
      .slice(0, 2)
      .map((p) => p.charAt(0).toUpperCase())
      .join('');

    return initials || 'U';
  }

  private formatDate(dateValue?: string): string {
    if (!dateValue) return '--';

    const date = new Date(dateValue);
    if (isNaN(date.getTime())) return '--';

    return date.toLocaleDateString('fr-FR');
  }

  private formatDerniereConnexion(lastLoginAt?: string): string {
    if (!lastLoginAt) return 'Jamais connecté';

    const date = new Date(lastLoginAt);
    if (isNaN(date.getTime())) return '--';

    return date.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  trackByUtilisateurId(index: number, utilisateur: Utilisateur): number {
    return utilisateur.id;
  }

  private getBackendErrorMessage(err: any, fallback: string): string {
    if (err?.status === 0) return 'Backend inaccessible ou problème CORS.';
    if (err?.status === 401) return 'Non authentifié. Token manquant ou expiré.';
    if (err?.status === 403) return 'Accès refusé.';
    if (err?.status === 404) return 'Endpoint backend introuvable.';

    return err?.error?.message || err?.error?.error || fallback;
  }
}