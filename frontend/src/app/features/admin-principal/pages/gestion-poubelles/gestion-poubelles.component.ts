import { Component } from '@angular/core';

type FiltrePoubelle = 'Toutes' | 'Actives' | 'Pleines' | 'Maintenance';
type StatutPoubelle = 'Active' | 'Full' | 'Maintenance';
type TypePoubelle = 'General' | 'Recyclable' | 'Organic';

interface Poubelle {
  nom: string;
  zone: string;
  code: string;
  statut: StatutPoubelle;
  niveauRemplissage: number;
  type: TypePoubelle;
  capacite: string;
  batterie: number;
  signal: number;
  temperature: string;
  derniereCollecte: string;
  prochaineCollecte: string;
}

@Component({
  selector: 'app-gestion-poubelles',
  templateUrl: './gestion-poubelles.component.html',
  styleUrls: ['./gestion-poubelles.component.css']
})
export class GestionPoubellesComponent {
  termeRecherche: string = '';
  filtreSelectionne: FiltrePoubelle = 'Toutes';

  filtres: FiltrePoubelle[] = ['Toutes', 'Actives', 'Pleines', 'Maintenance'];

  poubelles: Poubelle[] = [
    {
      nom: 'Place de la République',
      zone: 'Zone Centre',
      code: 'BIN-001',
      statut: 'Full',
      niveauRemplissage: 87,
      type: 'General',
      capacite: '1100L',
      batterie: 78,
      signal: 85,
      temperature: '18°C',
      derniereCollecte: '01/03/2026',
      prochaineCollecte: '04/03/2026'
    },
    {
      nom: 'Avenue des Champs-Élysées',
      zone: 'Zone Ouest',
      code: 'BIN-002',
      statut: 'Active',
      niveauRemplissage: 45,
      type: 'Recyclable',
      capacite: '1100L',
      batterie: 88,
      signal: 92,
      temperature: '16°C',
      derniereCollecte: '02/03/2026',
      prochaineCollecte: '05/03/2026'
    },
    {
      nom: 'Jardin du Luxembourg',
      zone: 'Zone Sud',
      code: 'BIN-003',
      statut: 'Active',
      niveauRemplissage: 23,
      type: 'Organic',
      capacite: '800L',
      batterie: 65,
      signal: 72,
      temperature: '15°C',
      derniereCollecte: '02/03/2026',
      prochaineCollecte: '05/03/2026'
    },
    {
      nom: 'Montmartre',
      zone: 'Zone Nord',
      code: 'BIN-004',
      statut: 'Full',
      niveauRemplissage: 92,
      type: 'General',
      capacite: '1100L',
      batterie: 45,
      signal: 65,
      temperature: '10°C',
      derniereCollecte: '27/02/2026',
      prochaineCollecte: '03/03/2026'
    },
    {
      nom: 'Tour Eiffel',
      zone: 'Zone Ouest',
      code: 'BIN-005',
      statut: 'Active',
      niveauRemplissage: 68,
      type: 'Recyclable',
      capacite: '1100L',
      batterie: 82,
      signal: 89,
      temperature: '17°C',
      derniereCollecte: '03/03/2026',
      prochaineCollecte: '06/03/2026'
    },
    {
      nom: 'Gare du Nord',
      zone: 'Zone Nord',
      code: 'BIN-006',
      statut: 'Maintenance',
      niveauRemplissage: 15,
      type: 'General',
      capacite: '1100L',
      batterie: 20,
      signal: 40,
      temperature: '12°C',
      derniereCollecte: '28/02/2026',
      prochaineCollecte: '07/03/2026'
    }
  ];

  selectionnerFiltre(filtre: FiltrePoubelle): void {
    this.filtreSelectionne = filtre;
  }

  get nombreTotalPoubelles(): number {
    return this.poubelles.length;
  }

  get nombrePoubellesPleines(): number {
    return this.poubelles.filter(poubelle => poubelle.statut === 'Full').length;
  }

  get nombrePoubellesActives(): number {
    return this.poubelles.filter(poubelle => poubelle.statut === 'Active').length;
  }

  get nombrePoubellesMaintenance(): number {
    return this.poubelles.filter(poubelle => poubelle.statut === 'Maintenance').length;
  }

  get poubellesFiltrees(): Poubelle[] {
    let resultat = [...this.poubelles];

    if (this.filtreSelectionne === 'Actives') {
      resultat = resultat.filter(poubelle => poubelle.statut === 'Active');
    }

    if (this.filtreSelectionne === 'Pleines') {
      resultat = resultat.filter(poubelle => poubelle.statut === 'Full');
    }

    if (this.filtreSelectionne === 'Maintenance') {
      resultat = resultat.filter(poubelle => poubelle.statut === 'Maintenance');
    }

    if (this.termeRecherche.trim()) {
      const terme = this.termeRecherche.toLowerCase();
      resultat = resultat.filter(poubelle =>
        poubelle.nom.toLowerCase().includes(terme) ||
        poubelle.zone.toLowerCase().includes(terme) ||
        poubelle.code.toLowerCase().includes(terme) ||
        poubelle.type.toLowerCase().includes(terme)
      );
    }

    return resultat;
  }

  getClasseStatut(statut: StatutPoubelle): string {
    if (statut === 'Active') return 'statut-actif';
    if (statut === 'Full') return 'statut-plein';
    return 'statut-maintenance';
  }

  getLibelleStatut(statut: StatutPoubelle): string {
    if (statut === 'Active') return 'Active';
    if (statut === 'Full') return 'Full';
    return 'Maintenance';
  }

  getClasseType(type: TypePoubelle): string {
    if (type === 'Recyclable') return 'type-recyclable';
    if (type === 'Organic') return 'type-organique';
    return 'type-general';
  }

  getLibelleType(type: TypePoubelle): string {
    if (type === 'Organic') return 'Organic';
    return type;
  }
}