import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

type Bin = {
  id: string;
  address: string;
  fillPercent: number;   // 0..100
  distanceKm: number;
  etaMin: number;
  priority: 1 | 2 | 3 | 4 | 5;
  collected: boolean;
};

@Component({
  selector: 'app-chauffeur-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chauffeur-dashboard.component.html',
  styleUrls: ['./chauffeur-dashboard.component.css'],
})
export class ChauffeurDashboardComponent {
  // Top info
  nowTime = this.formatTime(new Date());
  nowDate = this.formatDate(new Date());

  // Tour data (fake for now)
  zoneName = 'Zone Nord';
  estimatedTime = '2h 45m';
  fuelSavingPercent = -25;

  // Bins list (fake)
  bins: Bin[] = [
    { id: 'BIN-001', address: 'Avenue Habib Bourguiba', fillPercent: 95, distanceKm: 0.8, etaMin: 5, priority: 1, collected: false },
    { id: 'BIN-002', address: 'Rue de la Liberté', fillPercent: 78, distanceKm: 1.2, etaMin: 8, priority: 2, collected: false },
    { id: 'BIN-003', address: 'Place de la République', fillPercent: 85, distanceKm: 1.5, etaMin: 10, priority: 3, collected: false },
    { id: 'BIN-004', address: 'Boulevard Mohamed V', fillPercent: 62, distanceKm: 2.1, etaMin: 14, priority: 4, collected: false },
    { id: 'BIN-005', address: 'Rue de Marseille', fillPercent: 70, distanceKm: 2.6, etaMin: 18, priority: 5, collected: false },
  ];

  tourStarted = false;

  get totalBins(): number {
    return this.bins.length;
  }

  get collectedCount(): number {
    return this.bins.filter(b => b.collected).length;
  }

  get progressPercent(): number {
    if (this.totalBins === 0) return 0;
    return Math.round((this.collectedCount / this.totalBins) * 100);
  }

  get binsProgressLabel(): string {
    return `${this.collectedCount}/${this.totalBins}`;
  }

  startTour(): void {
    this.tourStarted = true;
  }

  collect(bin: Bin): void {
    if (!this.tourStarted) return; // اختياري
    bin.collected = true;
  }

  // UI helpers
  badgeClass(priority: number): string {
    switch (priority) {
      case 1: return 'badge badge-red';
      case 2: return 'badge badge-yellow';
      case 3: return 'badge badge-red';
      case 4: return 'badge badge-yellow';
      default: return 'badge badge-green';
    }
  }

  cardOutlineClass(priority: number): string {
    // كيما في الصورة: BIN-004 عندو outline أخضر
    if (priority === 4) return 'bin-card outline-green';
    return 'bin-card';
  }

  private formatTime(d: Date): string {
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    return `${hh}:${mm}`;
  }

  private formatDate(d: Date): string {
    // FR-like: lundi 23 février 2026
    return d.toLocaleDateString('fr-FR', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' });
  }
}