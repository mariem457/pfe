import { Component, OnInit, OnDestroy } from '@angular/core';
import { DatePipe, NgFor, NgClass, NgIf } from '@angular/common';
import {
  SmartRoutingDashboardService,
  RoutingExecutionLogDto,
  MandatoryBinInsightDto
} from '../../../../services/smart-routing-dashboard.service';

@Component({
  selector: 'app-smart-routing',
  standalone: true,
  imports: [NgIf, NgFor, NgClass, DatePipe],
  templateUrl: './smart-routing.component.html',
  styleUrls: ['./smart-routing.component.css']
})
export class SmartRoutingComponent implements OnInit, OnDestroy {
  loading = false;
  error = '';
  lastUpdatedLabel = '—';

  lastExecution: RoutingExecutionLogDto | null = null;
  executionHistory: RoutingExecutionLogDto[] = [];
  mandatoryInsights: MandatoryBinInsightDto[] = [];
  feedbackMandatoryBins: MandatoryBinInsightDto[] = [];

  private refreshInterval: any;

  constructor(private smartRoutingDashboardService: SmartRoutingDashboardService) {}

  ngOnInit(): void {
    this.loadData();

    this.refreshInterval = setInterval(() => {
      this.loadData();
    }, 20000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  loadData(): void {
    this.loading = true;
    this.error = '';

    this.smartRoutingDashboardService.getDashboardData().subscribe({
      next: (data) => {
        this.lastExecution = data.lastExecution || null;
        this.executionHistory = data.executionHistory || [];
        this.mandatoryInsights = data.mandatoryInsights || [];
        this.feedbackMandatoryBins = data.feedbackMandatoryBins || [];
        this.loading = false;
        this.updateLastRefresh();
      },
      error: (err) => {
        console.error('GET /api/routing/* failed', err);
        this.error = 'Impossible de charger les données du routage intelligent.';
        this.loading = false;
      }
    });
  }

  get mandatoryBinsCount(): number {
    return this.mandatoryInsights.filter(item => item.mandatory).length;
  }

  get averageFeedbackScore(): number {
    if (!this.mandatoryInsights.length) return 0;
    const total = this.mandatoryInsights.reduce((sum, item) => sum + (item.feedbackScore || 0), 0);
    return total / this.mandatoryInsights.length;
  }

  get droppedBinsLastRun(): number {
    return this.lastExecution?.droppedBinsCount ?? 0;
  }

  get currentRoutingStrategy(): string {
    return this.lastExecution?.strategy ?? 'N/A';
  }

  get highFeedbackScoreBinsCount(): number {
    return this.mandatoryInsights.filter(item => (item.feedbackScore || 0) >= 6).length;
  }

  get recentUnresolvedBins(): MandatoryBinInsightDto[] {
    return [...this.mandatoryInsights]
      .filter(item => item.postponementCount > 0)
      .sort((a, b) => b.postponementCount - a.postponementCount)
      .slice(0, 5);
  }

  getStrategyClass(strategy?: string): string {
    switch (strategy) {
      case 'FULL_OPTIMIZATION':
        return 'sr-strategy sr-strategy--full';
      case 'MANDATORY_ONLY':
        return 'sr-strategy sr-strategy--mandatory';
      case 'REFUEL_ONLY':
        return 'sr-strategy sr-strategy--refuel';
      case 'SKIP':
        return 'sr-strategy sr-strategy--skip';
      default:
        return 'sr-strategy sr-strategy--default';
    }
  }

  formatNumber(value: number | null | undefined, digits = 1): string {
    return Number(value || 0).toFixed(digits);
  }

  trackByBinId(index: number, item: MandatoryBinInsightDto): number {
    return item.binId;
  }

  trackByExecution(index: number, item: RoutingExecutionLogDto): string {
    return `${item.createdAt}-${index}`;
  }

  private updateLastRefresh(): void {
    this.lastUpdatedLabel = new Date().toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }
}