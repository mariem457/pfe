import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
  termeRecherche = '';
  filtreSelectionne: FiltrePoubelle = 'Toutes';
  filtres: FiltrePoubelle[] = ['Toutes', 'Actives', 'Pleines', 'Maintenance'];

  poubelles: Poubelle[] = [];
  loading = false;
  errorMessage = '';

  showQrModal = false;
  qrPreviewUrl = '';
  selectedBin: Poubelle | null = null;
  qrLoading = false;

  showAddModal = false;
  addLoading = false;

  newBin = {
    binCode: '',
    type: '',
    zoneId: null as number | null,
    lat: null as number | null,
    lng: null as number | null,
    installationDate: '',
    isActive: true,
    notes: ''
  };

private readonly API = 'http://localhost:8081/api/bins';

  constructor(
    private binService: BinService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.chargerPoubelles();
  }

  chargerPoubelles(): void {
    this.loading = true;
    this.errorMessage = '';

    this.binService.getBins().subscribe({
      next: (data: BinStatusDto[]) => {
        this.poubelles = (data || []).map(item => this.mapBinToPoubelle(item));
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement poubelles', error);

        if (error?.status === 0) this.errorMessage = 'Backend inaccessible ou problème CORS.';
        else if (error?.status === 401) this.errorMessage = 'Non authentifié. Token manquant ou expiré.';
        else if (error?.status === 403) this.errorMessage = 'Accès refusé.';
        else if (error?.status === 404) this.errorMessage = 'Endpoint /api/bins introuvable.';
        else this.errorMessage = error?.error?.message || 'Impossible de charger les poubelles.';

        this.loading = false;
      }
    });
  }

  openAddModal(): void {
    this.showAddModal = true;
    const today = new Date().toISOString().split('T')[0];
    this.newBin = {
      binCode: '',
      type: '',
      zoneId: null,
      lat: null,
      lng: null,
      installationDate: today,
      isActive: true,
      notes: ''
    };
  }

  closeAddModal(): void {
    this.showAddModal = false;
  }

addBin(): void {
  if (this.newBin.lat === null || this.newBin.lng === null) {
    alert('Latitude et longitude sont obligatoires.');
    return;
  }

  const wasteType = this.newBin.type?.trim().toUpperCase();

  const payload = {
    binCode: this.newBin.binCode?.trim() || null,

    type: 'REAL',
    wasteType: wasteType,

    zoneId: this.newBin.zoneId,
    lat: Number(this.newBin.lat),
    lng: Number(this.newBin.lng),
    accessLat: Number(this.newBin.lat),
    accessLng: Number(this.newBin.lng),
    installationDate: this.newBin.installationDate,
    isActive: this.newBin.isActive,
    notes: this.newBin.notes
  };

  this.addLoading = true;

  this.http.post(`${this.API}`, payload).subscribe({
    next: () => {
      this.addLoading = false;
      this.closeAddModal();
      this.chargerPoubelles();
      alert('Poubelle ajoutée avec succès.');
    },
    error: (error) => {
      console.error('Erreur ajout poubelle:', error);
      this.addLoading = false;
      alert(error?.error?.message || error?.error || 'Erreur lors de l’ajout de la poubelle.');
    }
  });
}
printQR(poubelle: Poubelle): void {
  if (!poubelle.id) return;

  this.http.get(`${this.API}/${poubelle.id}/qrcode`, {
    responseType: 'blob'
  }).subscribe({
    next: (blob) => {
      const reader = new FileReader();

      reader.onloadend = () => {
        const base64 = reader.result as string;
        this.printQrBase64(base64, poubelle.code);
      };

      reader.readAsDataURL(blob);
    },
    error: (error) => {
      console.error('Erreur récupération QR', error);
      alert('Erreur lors de la récupération du QR code.');
    }
  });
}
regenerateQR(poubelle: Poubelle): void {
  if (!poubelle.id) return;

  this.qrLoading = true;
  this.selectedBin = poubelle;

  this.http.post(`${this.API}/${poubelle.id}/qrcode/regenerate`, {}, {
    responseType: 'blob'
  }).subscribe({
    next: (blob) => {
      if (this.qrPreviewUrl) {
        URL.revokeObjectURL(this.qrPreviewUrl);
      }

      this.qrPreviewUrl = URL.createObjectURL(blob);
      this.showQrModal = true;
      this.qrLoading = false;
    },
    error: (error) => {
      console.error('Erreur régénération QR', error);
      alert('Erreur lors de la régénération du QR code.');
      this.qrLoading = false;
    }
  });
}
  printQrBase64(base64: string, code: string): void {
    const printWindow = window.open('', '_blank', 'width=600,height=700');

    if (!printWindow) {
      alert('Veuillez autoriser les pop-ups pour imprimer le QR code.');
      return;
    }

    printWindow.document.open();
    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>QR Code - ${code}</title>
          <style>
            body {
              margin: 0;
              padding: 40px;
              font-family: Arial, sans-serif;
              display: flex;
              justify-content: center;
              align-items: center;
              min-height: 100vh;
              background: white;
            }

            .qr-card {
              border: 2px solid #111827;
              border-radius: 12px;
              padding: 24px;
              text-align: center;
            }

            h2 {
              margin: 0 0 16px;
              font-size: 24px;
              color: #111827;
            }

            img {
              width: 300px;
              height: 300px;
            }

            .code {
              margin-top: 16px;
              font-size: 18px;
              font-weight: bold;
              color: #111827;
            }
          </style>
        </head>

        <body>
          <div class="qr-card">
            <h2>Wise Trash</h2>
            <img src="${base64}" alt="QR Code ${code}" />
            <div class="code">${code}</div>
          </div>

          <script>
            window.onload = function() {
              window.focus();
              window.print();
            };
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  }

  downloadQR(): void {
    if (!this.qrPreviewUrl || !this.selectedBin) return;

    const a = document.createElement('a');
    a.href = this.qrPreviewUrl;
    a.download = `qr-${this.selectedBin.code}.png`;
    a.click();
  }

  printFromModal(): void {
    if (this.selectedBin) this.printQR(this.selectedBin);
  }

  closeQrModal(): void {
    this.showQrModal = false;

    if (this.qrPreviewUrl) {
      URL.revokeObjectURL(this.qrPreviewUrl);
      this.qrPreviewUrl = '';
    }

    this.selectedBin = null;
  }

  private mapBinToPoubelle(item: BinStatusDto): Poubelle {
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
    if (isActive === false) return 'Maintenance';

    const s = (status ?? '').toUpperCase();

    if (s.includes('MAINTENANCE') || s.includes('OFFLINE') || s.includes('ERROR')) {
      return 'Maintenance';
    }

    if ((fillLevel ?? 0) >= 80) return 'Full';

    return 'Active';
  }

  private formatDate(dateValue: string): string {
    const date = new Date(dateValue);
    return isNaN(date.getTime()) ? '--' : date.toLocaleDateString('fr-FR');
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
    return this.poubelles.filter(p => p.statut === 'Full').length;
  }

  get nombrePoubellesActives(): number {
    return this.poubelles.filter(p => p.statut === 'Active').length;
  }

  get nombrePoubellesMaintenance(): number {
    return this.poubelles.filter(p => p.statut === 'Maintenance').length;
  }

  get poubellesFiltrees(): Poubelle[] {
    let res = [...this.poubelles];

    if (this.filtreSelectionne === 'Actives') {
      res = res.filter(p => p.statut === 'Active');
    }

    if (this.filtreSelectionne === 'Pleines') {
      res = res.filter(p => p.statut === 'Full');
    }

    if (this.filtreSelectionne === 'Maintenance') {
      res = res.filter(p => p.statut === 'Maintenance');
    }

    const t = this.termeRecherche.trim().toLowerCase();

    if (t) {
      res = res.filter(p =>
        p.nom.toLowerCase().includes(t) ||
        p.zone.toLowerCase().includes(t) ||
        p.code.toLowerCase().includes(t)
      );
    }

    return res;
  }

  getClasseStatut(statut: StatutPoubelle): string {
    if (statut === 'Active') return 'statut-actif';
    if (statut === 'Full') return 'statut-plein';
    return 'statut-maintenance';
  }

  getLibelleStatut(statut: StatutPoubelle): string {
    if (statut === 'Active') return 'Active';
    if (statut === 'Full') return 'Pleine';
    return 'Maintenance';
  }

  getClasseRemplissage(niveau: number): string {
    if (niveau >= 80) return 'fill-high';
    if (niveau >= 50) return 'fill-medium';
    return 'fill-low';
  }
}