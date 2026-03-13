import { Component, OnInit, OnDestroy } from '@angular/core';
import { NgFor, NgClass, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertService, AlertDto, AlertDetailsDto } from '../../../../services/alert.service';
import {
  DashboardService,
  DashboardKpiResponse,
  DashboardChartsResponse,
  ChartPointDto,
  BinDistributionDto
} from '../../../../services/dashboard.service';
import { FleetMapComponent } from '../trucks/fleet-map/fleet-map.component';
import {
  PublicReportService,
  PublicReportDto
} from '../../../../services/public-report.service';

type DeltaType = 'up' | 'down';

interface KpiCard {
  icon: 'trash' | 'alert' | 'truck' | 'leaf';
  label: string;
  value: string;
  delta: string;
  deltaType: DeltaType;
}

interface AlertItem {
  alertId: number;
  id: string;
  timeAgo: string;
  location: string;
  status: 'Plein' | 'Maintenance';
  severity?: string;
  alertType?: string;
  title?: string;
  message?: string;
  createdAt?: string;
}

export interface MapReportItem {
  id: number;
  code: string;
  status: 'Pending' | 'Validated' | 'Assigned';
  priority: 'High' | 'Medium' | 'Low';
  description: string;
  location: string;
  lat: number;
  lng: number;
  assignedTo?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NgFor, NgClass, NgIf, FormsModule, FleetMapComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  kpis: KpiCard[] = [
    { icon: 'trash', label: 'Nombre total de bacs', value: '0', delta: '+0', deltaType: 'up' },
    { icon: 'alert', label: 'Bacs pleins', value: '0', delta: '+0', deltaType: 'up' },
    { icon: 'truck', label: 'Camions actifs', value: '0', delta: '+0', deltaType: 'up' },
    { icon: 'leaf', label: 'Taux moyen de remplissage', value: '0 %', delta: '+0 %', deltaType: 'up' }
  ];

  dashboardLoading = false;
  dashboardError = '';

  chartsLoading = false;
  chartsError = '';

  fillTrend: ChartPointDto[] = [];
  weeklyCollections: ChartPointDto[] = [];
  distribution: BinDistributionDto = {
    emptyBins: 0,
    partialBins: 0,
    fullBins: 0,
    totalBins: 0
  };

  loadingAlerts = true;
  alertsError = '';
  alerts: AlertItem[] = [];

  reportsLoading = false;
  reportsError = '';
  mapReports: MapReportItem[] = [];

  filterResolved: '' | 'false' | 'true' = 'false';
  filterSeverity: '' | 'LOW' | 'MEDIUM' | 'HIGH' = '';
  filterType: '' | 'THRESHOLD' | 'ANOMALY' | 'MAINTENANCE' | 'SYSTEM' = '';
  searchText = '';

  resolvingId: number | null = null;

  showDetails = false;
  detailsLoading = false;
  detailsError = '';
  selectedDetails: AlertDetailsDto | null = null;

  private kpiInterval: any;
  private alertsInterval: any;
  private chartsInterval: any;
  private reportsInterval: any;

  constructor(
    private alertService: AlertService,
    private dashboardService: DashboardService,
    private publicReportService: PublicReportService
  ) {}

  ngOnInit(): void {
    this.loadKpis();
    this.loadCharts();
    this.loadAlerts();
    this.loadReportsForMap();

    this.kpiInterval = setInterval(() => {
      this.loadKpis();
    }, 10000);

    this.chartsInterval = setInterval(() => {
      this.loadCharts();
    }, 15000);

    this.alertsInterval = setInterval(() => {
      this.loadAlerts();
    }, 15000);

    this.reportsInterval = setInterval(() => {
      this.loadReportsForMap();
    }, 15000);
  }

  ngOnDestroy(): void {
    if (this.kpiInterval) clearInterval(this.kpiInterval);
    if (this.chartsInterval) clearInterval(this.chartsInterval);
    if (this.alertsInterval) clearInterval(this.alertsInterval);
    if (this.reportsInterval) clearInterval(this.reportsInterval);
  }

  loadKpis(): void {
    this.dashboardLoading = true;
    this.dashboardError = '';

    this.dashboardService.getKpis().subscribe({
      next: (data: DashboardKpiResponse) => {
        this.kpis = [
          {
            icon: 'trash',
            label: 'Nombre total de bacs',
            value: this.formatNumber(data.totalBins),
            delta: '+0',
            deltaType: 'up'
          },
          {
            icon: 'alert',
            label: 'Bacs pleins',
            value: this.formatNumber(data.fullBins),
            delta: '+0',
            deltaType: 'up'
          },
          {
            icon: 'truck',
            label: 'Camions actifs',
            value: this.formatNumber(data.activeTrucks),
            delta: '+0',
            deltaType: 'up'
          },
          {
            icon: 'leaf',
            label: 'Taux moyen de remplissage',
            value: `${this.formatDecimal(data.averageFillLevel)} %`,
            delta: '+0 %',
            deltaType: 'up'
          }
        ];

        this.dashboardLoading = false;
      },
      error: (err) => {
        console.error('GET /api/dashboard/kpis failed', err);
        this.dashboardError = 'Impossible de charger les indicateurs du tableau de bord.';
        this.dashboardLoading = false;
      }
    });
  }

  loadCharts(): void {
    this.chartsLoading = true;
    this.chartsError = '';

    this.dashboardService.getCharts().subscribe({
      next: (data: DashboardChartsResponse) => {
        this.fillTrend = data.fillTrend || [];
        this.weeklyCollections = data.weeklyCollections || [];
        this.distribution = data.distribution || {
          emptyBins: 0,
          partialBins: 0,
          fullBins: 0,
          totalBins: 0
        };
        this.chartsLoading = false;
      },
      error: (err) => {
        console.error('GET /api/dashboard/charts failed', err);
        this.chartsError = 'Impossible de charger les graphiques du tableau de bord.';
        this.chartsLoading = false;
      }
    });
  }

  loadAlerts(): void {
    this.loadingAlerts = true;
    this.alertsError = '';

    const resolvedParam =
      this.filterResolved === '' ? null :
      this.filterResolved === 'true' ? true : false;

    this.alertService.searchAlerts({
      resolved: resolvedParam,
      severity: this.filterSeverity || null,
      alertType: this.filterType || null,
      q: this.searchText?.trim() ? this.searchText.trim() : null
    }).subscribe({
      next: (data: AlertDto[]) => {
        const arr = data || [];
        this.alerts = arr.slice(0, 6).map(a => this.mapAlert(a));
        this.loadingAlerts = false;
      },
      error: (err) => {
        console.error('GET /api/alerts failed', err);
        this.alertsError = 'Impossible de charger les alertes.';
        this.loadingAlerts = false;
      }
    });
  }

  loadReportsForMap(): void {
    this.reportsLoading = true;
    this.reportsError = '';

    this.publicReportService.getAllReports().subscribe({
      next: (data: PublicReportDto[]) => {
        const visibleStatuses = ['EN_ATTENTE', 'VALIDE', 'AFFECTE'];

        this.mapReports = (data || [])
          .filter(r => visibleStatuses.includes(r.status))
          .map((r): MapReportItem => ({
            id: r.id,
            code: r.reportCode,
            status:
              r.status === 'AFFECTE' ? 'Assigned' :
              r.status === 'VALIDE' ? 'Validated' :
              'Pending',
            priority:
              r.priority === 'HIGH' ? 'High' :
              r.priority === 'MEDIUM' ? 'Medium' :
              'Low',
            description: r.description || 'Aucune description',
            location: r.address || 'Adresse non disponible',
            lat: Number(r.latitude),
            lng: Number(r.longitude),
            assignedTo: r.assignedDriverName || undefined
          }))
          .filter(r => !Number.isNaN(r.lat) && !Number.isNaN(r.lng));

        this.reportsLoading = false;
      },
      error: (err) => {
        console.error('GET /municipality/public-reports failed', err);
        this.reportsError = 'Impossible de charger les signalements sur la carte.';
        this.reportsLoading = false;
      }
    });
  }

  resetFilters(): void {
    this.filterResolved = 'false';
    this.filterSeverity = '';
    this.filterType = '';
    this.searchText = '';
    this.loadAlerts();
  }

  resolve(a: AlertItem): void {
    if (this.resolvingId) return;

    this.resolvingId = a.alertId;

    this.alertService.resolveAlert(a.alertId).subscribe({
      next: () => {
        this.resolvingId = null;
        this.loadAlerts();
        this.loadKpis();
        this.loadCharts();
      },
      error: (err) => {
        console.error('PATCH /api/alerts/{id}/resolve failed', err);
        this.resolvingId = null;
        this.alertsError = "Impossible de résoudre l'alerte.";
      }
    });
  }

  openDetails(a: AlertItem): void {
    this.showDetails = true;
    this.detailsLoading = true;
    this.detailsError = '';
    this.selectedDetails = null;

    this.alertService.getAlertDetails(a.alertId).subscribe({
      next: (d) => {
        this.selectedDetails = d;
        this.detailsLoading = false;
      },
      error: (err) => {
        console.error('GET /api/alerts/{id} failed', err);
        this.detailsError = "Impossible de charger les détails de l'alerte.";
        this.detailsLoading = false;
      }
    });
  }

  closeDetails(): void {
    this.showDetails = false;
    this.selectedDetails = null;
    this.detailsError = '';
    this.detailsLoading = false;
  }

  get donutStrokeEmpty(): string {
    const total = this.distribution.totalBins || 1;
    return `${(this.distribution.emptyBins / total) * 490} 490`;
  }

  get donutStrokePartial(): string {
    const total = this.distribution.totalBins || 1;
    return `${(this.distribution.partialBins / total) * 490} 490`;
  }

  get donutStrokeFull(): string {
    const total = this.distribution.totalBins || 1;
    return `${(this.distribution.fullBins / total) * 490} 490`;
  }

  private mapAlert(a: AlertDto): AlertItem {
    const type = (a.alertType || '').toUpperCase();
    const sev = (a.severity || '').toUpperCase();

    const status: 'Plein' | 'Maintenance' =
      type === 'MAINTENANCE' ? 'Maintenance' : 'Plein';

    return {
      alertId: a.id,
      id: a.binCode || `BIN-${a.binId}`,
      timeAgo: a.createdAt ? this.timeAgo(a.createdAt) : '',
      location: a.title || a.message || '(—)',
      status,
      severity: sev,
      alertType: type,
      title: a.title,
      message: a.message,
      createdAt: a.createdAt
    };
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value ?? 0);
  }

  private formatDecimal(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1
    }).format(value ?? 0);
  }

  timeAgo(iso: string): string {
    const d = new Date(iso).getTime();
    const diff = Date.now() - d;
    const min = Math.floor(diff / 60000);

    if (min < 1) return "à l'instant";
    if (min < 60) return `il y a ${min} min`;

    const h = Math.floor(min / 60);
    if (h < 24) return `il y a ${h} h`;

    const days = Math.floor(h / 24);
    return `il y a ${days} j`;
  }
}