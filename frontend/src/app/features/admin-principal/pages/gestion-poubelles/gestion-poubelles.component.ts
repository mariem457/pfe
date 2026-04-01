import { Component, OnInit } from '@angular/core';
import { BinService, BinStatusDto } from '../../../../services/bin.service';

type FiltrePoubelle = 'Toutes' | 'Actives' | 'Pleines' | 'Maintenance';
type StatutPoubelle = 'Active' | 'Full' | 'Maintenance';

interface Poubelle {
  id?: number;
  nom: string;
  zone: string;
  code: string;
  statut: StatutPoubelle;
  niveauRemplissage: number;
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
export class GestionPoubellesComponent implements OnInit {
  termeRecherche: string = '';
  filtreSelectionne: FiltrePoubelle = 'Toutes';

  filtres: FiltrePoubelle[] = ['Toutes', 'Actives', 'Pleines', 'Maintenance'];

  poubelles: Poubelle[] = [];
  loading = false;
  errorMessage = '';

  constructor(private binService: BinService) {}

  ngOnInit(): void {
    this.chargerPoubelles();
  }

  chargerPoubelles(): void {
    this.loading = true;
    this.errorMessage = '';

    this.binService.getBinsStatus().subscribe({
      next: (data) => {
        this.poubelles = data.map(item => this.mapBinStatusToPoubelle(item));
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement poubelles', error);
        this.errorMessage = 'Impossible de charger les poubelles depuis le backend';
        this.loading = false;
      }
    });
  }

  private mapBinStatusToPoubelle(item: BinStatusDto): Poubelle {
    const niveau = item.fillLevel ?? 0;

    return {
      id: item.id,
      nom: item.zoneName || item.binCode || 'Poubelle',
      zone: item.zoneName || 'Zone inconnue',
      code: item.binCode || '--',
      statut: this.mapStatut(item.status, niveau, item.isActive),
      niveauRemplissage: niveau,
      capacite: '1100L',
      batterie: item.batteryLevel ?? 0,
      signal: item.rssi ?? 0,
      temperature: item.temperature != null ? `${item.temperature}°C` : '--',
      derniereCollecte: item.lastTelemetryAt ? this.formatDate(item.lastTelemetryAt) : '--',
      prochaineCollecte: this.calculerProchaineCollecte(item.lastTelemetryAt)
    };
  }

  private mapStatut(status?: string, fillLevel?: number, isActive?: boolean): StatutPoubelle {
    if (isActive === false) {
      return 'Maintenance';
    }

    const s = (status ?? '').toUpperCase();

    if (s.includes('MAINTENANCE') || s.includes('OFFLINE') || s.includes('ERROR')) {
      return 'Maintenance';
    }

    if ((fillLevel ?? 0) >= 80) {
      return 'Full';
    }

    return 'Active';
  }

  private formatDate(dateValue: string): string {
    const date = new Date(dateValue);
    if (isNaN(date.getTime())) return '--';
    return date.toLocaleDateString('fr-FR');
  }

  private calculerProchaineCollecte(lastTelemetryAt?: string): string {
    if (!lastTelemetryAt) return '--';

    const date = new Date(lastTelemetryAt);
    if (isNaN(date.getTime())) return '--';

    date.setDate(date.getDate() + 3);
    return date.toLocaleDateString('fr-FR');
  }

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
        poubelle.code.toLowerCase().includes(terme)
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
}