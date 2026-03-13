import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  PublicReportService,
  PublicReportDto,
  PublicReportDecisionDto
} from '../../../../services/public-report.service';
import { MapFocusService } from '../../../../services/map-focus.service';

type ReportStatus =
  | 'PendingReview'
  | 'Validated'
  | 'Rejected'
  | 'Assigned'
  | 'Resolved';

type Priority = 'High' | 'Medium' | 'Low';

interface ReportItem {
  id: number;
  code: string;
  status: ReportStatus;
  priority: Priority;
  timeAgo: string;

  location: string;
  description: string;
  lat: number;
  lng: number;

  assignedTo?: string;
  resolvedAgo?: string;
  resolvedNote?: string;
  reportType?: string;

  duplicateOfReportId?: number;
  qualificationNote?: string;
  decisionReason?: string;

  hasImage?: boolean;
  imageUrl?: string | null;
}

@Component({
  selector: 'app-public-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './public-reports.component.html',
  styleUrls: ['./public-reports.component.css']
})
export class PublicReportsComponent implements OnInit {
  query = '';
  statusFilter: 'All' | ReportStatus = 'All';
  reports: ReportItem[] = [];

  loading = false;
  error = '';

  actingId: number | null = null;

  constructor(
    private publicReportService: PublicReportService,
    private mapFocusService: MapFocusService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.error = '';

    this.publicReportService.getAllReports().subscribe({
      next: (data: PublicReportDto[]) => {
        this.reports = (data || []).map((r) => this.mapApiReport(r));
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement signalements:', err);
        this.error = 'Impossible de charger les signalements.';
        this.loading = false;
      }
    });
  }

  private mapApiReport(r: PublicReportDto): ReportItem {
    return {
      id: r.id,
      code: r.reportCode,
      status: this.mapStatus(r.status),
      priority: this.mapPriority(r.priority),
      timeAgo: this.timeAgoFromDate(r.createdAt),
      location: r.address || 'Adresse non disponible',
      description: r.description || 'Aucune description',
      lat: Number(r.latitude),
      lng: Number(r.longitude),
      assignedTo: r.assignedDriverName || undefined,
      resolvedAgo: r.resolvedAt ? this.timeAgoFromDate(r.resolvedAt) : undefined,
      resolvedNote: r.resolvedNote || undefined,
      reportType: r.reportType || undefined,
      duplicateOfReportId: r.duplicateOfReportId || undefined,
      qualificationNote: r.qualificationNote || undefined,
      decisionReason: r.decisionReason || undefined,
      hasImage: !!r.photoUrl,
      imageUrl: r.photoUrl ? `http://localhost:8081${r.photoUrl}` : null
    };
  }

  private mapStatus(status: string): ReportStatus {
    switch (status) {
      case 'VALIDE':
        return 'Validated';
      case 'REJETE':
        return 'Rejected';
      case 'AFFECTE':
        return 'Assigned';
      case 'RESOLU':
        return 'Resolved';
      case 'EN_ATTENTE':
      default:
        return 'PendingReview';
    }
  }

  private mapPriority(priority: string): Priority {
    switch (priority) {
      case 'HIGH':
        return 'High';
      case 'MEDIUM':
        return 'Medium';
      case 'LOW':
      default:
        return 'Low';
    }
  }

  timeAgoFromDate(dateString: string): string {
    if (!dateString) return '';

    const date = new Date(dateString).getTime();
    const now = Date.now();
    const diffMs = now - date;

    const minutes = Math.floor(diffMs / 60000);
    const hours = Math.floor(diffMs / 3600000);
    const days = Math.floor(diffMs / 86400000);

    if (minutes < 60) return `il y a ${minutes} minute${minutes > 1 ? 's' : ''}`;
    if (hours < 24) return `il y a ${hours} heure${hours > 1 ? 's' : ''}`;
    return `il y a ${days} jour${days > 1 ? 's' : ''}`;
  }

  statusLabel(s: ReportStatus) {
    return s === 'PendingReview' ? 'En attente de validation'
      : s === 'Validated' ? 'Validé'
      : s === 'Rejected' ? 'Rejeté'
      : s === 'Assigned' ? 'Affecté'
      : 'Résolu';
  }

  priorityLabel(p: Priority) {
    return p === 'High' ? 'Élevée'
      : p === 'Medium' ? 'Moyenne'
      : 'Faible';
  }

  get stats() {
    const total = this.reports.length;
    const pending = this.reports.filter(r => r.status === 'PendingReview').length;
    const validated = this.reports.filter(r => r.status === 'Validated').length;
    const rejected = this.reports.filter(r => r.status === 'Rejected').length;
    const resolved = this.reports.filter(r => r.status === 'Resolved').length;

    return { total, pending, validated, rejected, resolved };
  }

  get filteredReports() {
    const q = this.query.trim().toLowerCase();

    return this.reports.filter(r => {
      const matchQuery =
        !q ||
        r.code.toLowerCase().includes(q) ||
        r.location.toLowerCase().includes(q) ||
        r.description.toLowerCase().includes(q) ||
        (r.qualificationNote || '').toLowerCase().includes(q) ||
        (r.decisionReason || '').toLowerCase().includes(q);

      const matchStatus =
        this.statusFilter === 'All' ? true : r.status === this.statusFilter;

      return matchQuery && matchStatus;
    });
  }

  statusClass(s: ReportStatus) {
    return s === 'PendingReview' ? 'is-pending-review'
      : s === 'Validated' ? 'is-validated'
      : s === 'Rejected' ? 'is-rejected'
      : s === 'Assigned' ? 'is-assigned'
      : 'is-resolved';
  }

  priorityClass(p: Priority) {
    return p === 'High' ? 'is-high' : p === 'Medium' ? 'is-med' : 'is-low';
  }

  validate(r: ReportItem) {
    if (this.actingId) return;

    this.actingId = r.id;

    this.publicReportService.validateReport(r.id).subscribe({
      next: () => {
        this.actingId = null;
        alert('Signalement validé.');
        this.loadReports();
      },
      error: (err) => {
        console.error(err);
        this.actingId = null;
        alert('Erreur lors de la validation.');
      }
    });
  }

  reject(r: ReportItem) {
    if (this.actingId) return;

    const reason = prompt(
      `Raison du rejet pour ${r.code}\n\nEx: Hors zone / Doublon / Fausse alerte / Déjà traité`
    );

    if (!reason || !reason.trim()) return;

    this.actingId = r.id;

    this.publicReportService.rejectReport(r.id, reason.trim()).subscribe({
      next: () => {
        this.actingId = null;
        alert('Signalement rejeté.');
        this.loadReports();
      },
      error: (err) => {
        console.error(err);
        this.actingId = null;
        alert('Erreur lors du rejet.');
      }
    });
  }

  qualify(r: ReportItem) {
    const note = prompt(
      `Qualification pour ${r.code}\n\nEx: possible doublon / infos insuffisantes / à surveiller`
    );

    if (note == null) return;

    const duplicateRaw = prompt(
      `ID du signalement doublon (optionnel) pour ${r.code}\n\nLaissez vide si aucun`
    );

    const duplicateId =
      duplicateRaw && duplicateRaw.trim() ? Number(duplicateRaw.trim()) : null;

    this.publicReportService.qualifyReport(r.id, note.trim(), duplicateId).subscribe({
      next: () => {
        alert('Qualification enregistrée.');
        this.loadReports();
      },
      error: (err) => {
        console.error(err);
        alert('Erreur lors de la qualification.');
      }
    });
  }

  viewHistory(r: ReportItem) {
    this.publicReportService.getReportHistory(r.id).subscribe({
      next: (rows: PublicReportDecisionDto[]) => {
        if (!rows || rows.length === 0) {
          alert('Aucun historique disponible.');
          return;
        }

        const text = rows
          .map((x) => `${x.actionType} - ${x.reason || '—'} - ${x.createdAt}`)
          .join('\n');

        alert(text);
      },
      error: (err) => {
        console.error(err);
        alert('Erreur lors du chargement de l’historique.');
      }
    });
  }

  assignToTruck(r: ReportItem) {
    const driverId = prompt(`Entrer l'identifiant du driver pour ${r.code}`);
    if (!driverId) return;

    this.publicReportService.assignReport(r.id, Number(driverId)).subscribe({
      next: () => {
        alert('Signalement affecté avec succès.');
        this.loadReports();
      },
      error: (err) => {
        console.error(err);
        alert('Erreur lors de l’affectation.');
      }
    });
  }

  resolveReport(r: ReportItem) {
    const note = prompt(`Note de résolution pour ${r.code}`) || 'Problème traité';

    this.publicReportService.resolveReport(r.id, note).subscribe({
      next: () => {
        alert('Signalement marqué comme résolu.');
        this.loadReports();
      },
      error: (err) => {
        console.error(err);
        alert('Erreur lors de la résolution.');
      }
    });
  }

  viewOnMap(r: ReportItem) {
    if (r.lat == null || r.lng == null || Number.isNaN(r.lat) || Number.isNaN(r.lng)) {
      alert('Coordonnées indisponibles pour ce signalement.');
      return;
    }

    this.mapFocusService.setTarget({
      type: 'report',
      id: r.id,
      lat: r.lat,
      lng: r.lng
    });

    this.router.navigate(['/municipality/dashboard']);
  }

  canValidate(r: ReportItem): boolean {
    return r.status === 'PendingReview';
  }

  canReject(r: ReportItem): boolean {
    return r.status === 'PendingReview';
  }

  canAssign(r: ReportItem): boolean {
    return r.status === 'Validated';
  }

  canResolve(r: ReportItem): boolean {
    return r.status === 'Assigned' || r.status === 'Validated';
  }
}