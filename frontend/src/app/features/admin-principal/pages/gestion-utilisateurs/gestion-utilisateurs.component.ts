import { Component } from '@angular/core';

type FiltreUtilisateur = 'Tous' | 'Actifs' | 'Inactifs' | 'Chauffeurs';
type StatutUtilisateur = 'Actif' | 'Inactif';
type RoleUtilisateur = 'Administrateur' | 'Chauffeur' | 'Superviseur' | 'Observateur';

interface Utilisateur {
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
export class GestionUtilisateursComponent {
  termeRecherche: string = '';
  filtreSelectionne: FiltreUtilisateur = 'Tous';

  filtres: FiltreUtilisateur[] = ['Tous', 'Actifs', 'Inactifs', 'Chauffeurs'];

  utilisateurs: Utilisateur[] = [
    {
      initiales: 'JD',
      nom: 'Jean Dupont',
      courriel: 'jean.dupont@city.gov',
      telephone: '+33 6 12 34 56 78',
      statut: 'Actif',
      role: 'Administrateur',
      derniereActivite: 'Il y a 5 min'
    },
    {
      initiales: 'MM',
      nom: 'Marie Martin',
      courriel: 'marie.martin@city.gov',
      telephone: '+33 6 23 45 67 89',
      statut: 'Actif',
      role: 'Chauffeur',
      derniereActivite: 'Il y a 2 h'
    },
    {
      initiales: 'PB',
      nom: 'Pierre Bernard',
      courriel: 'pierre.bernard@city.gov',
      telephone: '+33 6 34 56 78 90',
      statut: 'Actif',
      role: 'Superviseur',
      derniereActivite: 'Il y a 30 min'
    },
    {
      initiales: 'SD',
      nom: 'Sophie Dubois',
      courriel: 'sophie.dubois@city.gov',
      telephone: '+33 6 45 67 89 01',
      statut: 'Actif',
      role: 'Chauffeur',
      derniereActivite: 'Il y a 1 h'
    },
    {
      initiales: 'LP',
      nom: 'Luc Petit',
      courriel: 'luc.petit@city.gov',
      telephone: '+33 6 56 78 90 12',
      statut: 'Inactif',
      role: 'Observateur',
      derniereActivite: 'Il y a 3 jours'
    }
  ];

  selectionnerFiltre(filtre: FiltreUtilisateur): void {
    this.filtreSelectionne = filtre;
  }

  get nombreTotalUtilisateurs(): number {
    return this.utilisateurs.length;
  }

  get nombreUtilisateursActifs(): number {
    return this.utilisateurs.filter(utilisateur => utilisateur.statut === 'Actif').length;
  }

  get nombreUtilisateursInactifs(): number {
    return this.utilisateurs.filter(utilisateur => utilisateur.statut === 'Inactif').length;
  }

  get nombreChauffeurs(): number {
    return this.utilisateurs.filter(utilisateur => utilisateur.role === 'Chauffeur').length;
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