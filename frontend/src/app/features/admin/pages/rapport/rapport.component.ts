import { Component, AfterViewInit, OnDestroy } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-rapport',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './rapport.component.html',
  styleUrls: ['./rapport.component.css']
})
export class RapportComponent implements AfterViewInit, OnDestroy {

  private collectionChart?: Chart;
  private fuelChart?: Chart;
  private co2Chart?: Chart;

  ngAfterViewInit(): void {
    this.initCharts();
  }

  ngOnDestroy(): void {
    this.collectionChart?.destroy();
    this.fuelChart?.destroy();
    this.co2Chart?.destroy();
  }

  private initCharts(): void {
    // destroy in case route reload
    this.collectionChart?.destroy();
    this.fuelChart?.destroy();
    this.co2Chart?.destroy();

    this.collectionChart = this.createCollectionChart();
    this.fuelChart = this.createFuelChart();
    this.co2Chart = this.createCO2Chart();
  }

  private baseLineOptions(showLegendBottom = false): ChartConfiguration<'line'>['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: showLegendBottom
          ? { position: 'bottom', labels: { color: '#111827', boxWidth: 10, boxHeight: 10 } }
          : { display: false },
        tooltip: {
          backgroundColor: '#ffffff',
          titleColor: '#111827',
          bodyColor: '#111827',
          borderColor: '#eef0f3',
          borderWidth: 1
        }
      },
      scales: {
        x: {
          grid: { color: '#eef0f3' },
          ticks: { color: '#6b7280' }
        },
        y: {
          beginAtZero: true,
          grid: { color: '#eef0f3' },
          ticks: { color: '#6b7280' }
        }
      }
    };
  }

  private baseBarOptions(): ChartConfiguration<'bar'>['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#ffffff',
          titleColor: '#111827',
          bodyColor: '#111827',
          borderColor: '#eef0f3',
          borderWidth: 1
        }
      },
      scales: {
        x: { grid: { color: '#eef0f3' }, ticks: { color: '#6b7280' } },
        y: { beginAtZero: true, grid: { color: '#eef0f3' }, ticks: { color: '#6b7280' } }
      }
    };
  }

  private createCollectionChart(): Chart {
    const canvas = document.getElementById('collectionChart') as HTMLCanvasElement;
    const ctx = canvas.getContext('2d')!;
    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(22,199,132,0.25)');
    gradient.addColorStop(1, 'rgba(22,199,132,0)');

    return new Chart(canvas, {
      type: 'line',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
        datasets: [{
          label: 'Collections',
          data: [4200, 3900, 4500, 4700, 5000, 5400],
          borderColor: '#16c784',
          backgroundColor: gradient,
          fill: true,
          tension: 0.4,
          pointRadius: 3,
          pointBackgroundColor: '#16c784'
        }]
      },
      options: this.baseLineOptions(false)
    });
  }

  private createFuelChart(): Chart {
    const canvas = document.getElementById('fuelChart') as HTMLCanvasElement;

    return new Chart(canvas, {
      type: 'line',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
        datasets: [
          {
            label: 'Fuel Usage (L)',
            data: [2400, 2200, 2100, 1950, 1800, 1650],
            borderColor: '#16c784',
            tension: 0.4,
            pointRadius: 3,
            pointBackgroundColor: '#16c784'
          },
          {
            label: 'Cost ($)',
            data: [4800, 4450, 4300, 4000, 3650, 3350],
            borderColor: '#111827',
            borderDash: [6, 6],
            tension: 0.4,
            pointRadius: 3,
            pointBackgroundColor: '#111827'
          }
        ]
      },
      options: this.baseLineOptions(true)
    });
  }

  private createCO2Chart(): Chart {
    const canvas = document.getElementById('co2Chart') as HTMLCanvasElement;

    return new Chart(canvas, {
      type: 'bar',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
        datasets: [{
          label: 'kg CO2',
          data: [140, 175, 205, 230, 290, 340],
          backgroundColor: '#16c784',
          borderRadius: 10
        }]
      },
      options: this.baseBarOptions()
    });
  }
}
