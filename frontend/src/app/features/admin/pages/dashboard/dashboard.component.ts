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

interface KpiCard {
  icon: 'trash' | 'alert' | 'truck' | 'leaf';
  label: string;
  value: string;
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

interface SvgPoint {
  x: number;
  y: number;
  label: string;
  value: number;
}

interface BarItem {
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  value: number;
  labelX: number;
  valueY: number;
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
    { icon: 'trash', label: 'Nombre total de bacs', value: '0' },
    { icon: 'alert', label: 'Bacs pleins', value: '0' },
    { icon: 'truck', label: 'Camions actifs', value: '0' },
    { icon: 'leaf', label: 'Taux moyen de remplissage', value: '0 %' }
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

  lineChartPoints: SvgPoint[] = [];
  weeklyBarItems: BarItem[] = [];

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

  lastUpdatedLabel = '—';

  private kpiInterval: any;
  private alertsInterval: any;
  private chartsInterval: any;
  private reportsInterval: any;

  private readonly lineChartConfig = {
    width: 520,
    height: 220,
    left: 30,
    right: 20,
    top: 24,
    bottom: 190
  };

  private readonly barChartConfig = {
    width: 520,
    height: 220,
    left: 30,
    right: 20,
    top: 24,
    bottom: 190
  };

  constructor(
    private alertService: AlertService,
    private dashboardService: DashboardService,
    private publicReportService: PublicReportService
  ) {}

  ngOnInit(): void {
    this.refreshAll();

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

  refreshAll(): void {
    this.loadKpis();
    this.loadCharts();
    this.loadAlerts();
    this.loadReportsForMap();
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
            value: this.formatNumber(data.totalBins)
          },
          {
            icon: 'alert',
            label: 'Bacs pleins',
            value: this.formatNumber(data.fullBins)
          },
          {
            icon: 'truck',
            label: 'Camions actifs',
            value: this.formatNumber(data.activeTrucks)
          },
          {
            icon: 'leaf',
            label: 'Taux moyen de remplissage',
            value: `${this.formatDecimal(data.averageFillLevel)} %`
          }
        ];

        this.dashboardLoading = false;
        this.updateLastRefresh();
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

        this.lineChartPoints = this.buildLineChartPoints(this.fillTrend);
        this.weeklyBarItems = this.buildWeeklyBarItems(this.weeklyCollections);

        this.chartsLoading = false;
        this.updateLastRefresh();
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
      this.filterResolved === ''
        ? null
        : this.filterResolved === 'true'
        ? true
        : false;

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
        this.updateLastRefresh();
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
              r.status === 'AFFECTE'
                ? 'Assigned'
                : r.status === 'VALIDE'
                ? 'Validated'
                : 'Pending',
            priority:
              r.priority === 'HIGH'
                ? 'High'
                : r.priority === 'MEDIUM'
                ? 'Medium'
                : 'Low',
            description: r.description || 'Aucune description',
            location: r.address || 'Adresse non disponible',
            lat: Number(r.latitude),
            lng: Number(r.longitude),
            assignedTo: r.assignedDriverName || undefined
          }))
          .filter(r => !Number.isNaN(r.lat) && !Number.isNaN(r.lng));

        this.reportsLoading = false;
        this.updateLastRefresh();
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

  get fillTrendLinePath(): string {
    if (!this.lineChartPoints.length) return '';
    return this.lineChartPoints
      .map((p, index) => `${index === 0 ? 'M' : 'L'} ${p.x} ${p.y}`)
      .join(' ');
  }

  get fillTrendAreaPath(): string {
    if (!this.lineChartPoints.length) return '';

    const first = this.lineChartPoints[0];
    const last = this.lineChartPoints[this.lineChartPoints.length - 1];
    const bottom = this.lineChartConfig.bottom;

    return `${this.fillTrendLinePath} L ${last.x} ${bottom} L ${first.x} ${bottom} Z`;
  }

  get safeDistributionTotal(): number {
    const sum =
      (this.distribution.emptyBins || 0) +
      (this.distribution.partialBins || 0) +
      (this.distribution.fullBins || 0);

    return this.distribution.totalBins || sum || 1;
  }

  get donutCircumference(): number {
    return 2 * Math.PI * 78;
  }

  get donutStrokeEmpty(): string {
    return `${((this.distribution.emptyBins || 0) / this.safeDistributionTotal) * this.donutCircumference} ${this.donutCircumference}`;
  }

  get donutStrokePartial(): string {
    return `${((this.distribution.partialBins || 0) / this.safeDistributionTotal) * this.donutCircumference} ${this.donutCircumference}`;
  }

  get donutStrokeFull(): string {
    return `${((this.distribution.fullBins || 0) / this.safeDistributionTotal) * this.donutCircumference} ${this.donutCircumference}`;
  }

  get donutOffsetPartial(): number {
    return -(((this.distribution.emptyBins || 0) / this.safeDistributionTotal) * this.donutCircumference);
  }

  get donutOffsetFull(): number {
    return -((((this.distribution.emptyBins || 0) + (this.distribution.partialBins || 0)) / this.safeDistributionTotal) * this.donutCircumference);
  }

  distributionPercent(value: number): string {
    const pct = ((value || 0) / this.safeDistributionTotal) * 100;
    return `${Math.round(pct)} %`;
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

  private updateLastRefresh(): void {
    this.lastUpdatedLabel = new Date().toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
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

  private buildLineChartPoints(data: ChartPointDto[]): SvgPoint[] {
    if (!data || data.length === 0) return [];

    const { width, left, right, top, bottom } = this.lineChartConfig;
    const usableWidth = width - left - right;
    const plotHeight = bottom - top;

    const values = data.map(p => Number(p.value) || 0);
    const rawMin = Math.min(...values);
    const rawMax = Math.max(...values);

    const padding = rawMax === rawMin ? 10 : (rawMax - rawMin) * 0.15;
    const minValue = Math.max(0, rawMin - padding);
    const maxValue = rawMax + padding;
    const range = Math.max(1, maxValue - minValue);

    return data.map((p, index) => {
      const value = Number(p.value) || 0;
      const x =
        data.length === 1
          ? left + usableWidth / 2
          : left + (index * usableWidth) / (data.length - 1);

      const y = bottom - ((value - minValue) / range) * plotHeight;

      return {
        x: Number(x.toFixed(2)),
        y: Number(y.toFixed(2)),
        label: p.label,
        value
      };
    });
  }

  private buildWeeklyBarItems(data: ChartPointDto[]): BarItem[] {
    if (!data || data.length === 0) return [];

    const { width, left, right, top, bottom } = this.barChartConfig;
    const usableWidth = width - left - right;
    const maxBarHeight = bottom - top;
    const maxValue = Math.max(...data.map(p => Number(p.value) || 0), 1);

    const gap = 14;
    const barWidth = Math.max(
      20,
      Math.min(44, (usableWidth - gap * (data.length - 1)) / data.length)
    );
    const contentWidth = data.length * barWidth + (data.length - 1) * gap;
    const startX = left + (usableWidth - contentWidth) / 2;

    return data.map((p, index) => {
      const value = Number(p.value) || 0;
      const height = (value / maxValue) * maxBarHeight;
      const x = startX + index * (barWidth + gap);
      const y = bottom - height;

      return {
        x: Number(x.toFixed(2)),
        y: Number(y.toFixed(2)),
        width: Number(barWidth.toFixed(2)),
        height: Number(height.toFixed(2)),
        label: p.label,
        value,
        labelX: Number((x + barWidth / 2).toFixed(2)),
        valueY: Number((y - 8).toFixed(2))
      };
    });
  }
}