import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FleetMapComponent } from './fleet-map/fleet-map.component';
import { TruckDashboardService, TruckDashboardResponse, TruckItem } from '../../../../services/truck-dashboard.service';

type TruckStatus = 'Actif' | 'Inactif';

interface TruckCard {
  id: string;
  driver: string;
  location: string;
  status: TruckStatus;
  progress: number;
  collected: number;
  remaining: number;
  fuel: number;
  etaMins: number;
}

@Component({
  selector: 'app-trucks',
  standalone: true,
  imports: [CommonModule, FleetMapComponent],
  templateUrl: './trucks.component.html',
  styleUrls: ['./trucks.component.css']
})
export class TrucksComponent implements OnInit {

  constructor(private dashboardService: TruckDashboardService) {}

  kpis = [
    { icon: 'show_chart', label: 'Camions actifs', value: '0' },
    { icon: 'place', label: 'Total des itinéraires', value: '0' },
    { icon: 'schedule', label: 'Progression moyenne', value: '0 %' },
    { icon: 'local_gas_station', label: 'État du carburant', value: '—' },
  ];

  trucks: TruckCard[] = [];

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard() {

    this.dashboardService.getDashboard().subscribe({

      next: (data: TruckDashboardResponse) => {

        this.kpis = [
          {
            icon: 'show_chart',
            label: 'Camions actifs',
            value: data.activeTrucks.toString()
          },
          {
            icon: 'place',
            label: 'Total des itinéraires',
            value: data.totalRoutes.toString()
          },
          {
            icon: 'schedule',
            label: 'Progression moyenne',
            value: data.averageProgress + ' %'
          },
          {
            icon: 'local_gas_station',
            label: 'État du carburant',
            value: data.fuelStatus
          }
        ];

        this.trucks = data.trucks.map((t: TruckItem) => ({
          id: t.truckCode,
          driver: t.driverName,
          location: t.locationLabel,
          status: t.active ? 'Actif' : 'Inactif',
          progress: t.progress ?? 0,
          collected: t.collectedBins ?? 0,
          remaining: t.remainingBins ?? 0,
          fuel: t.fuelLevel ?? 0,
          etaMins: t.etaMinutes ?? 0
        }));

      },

      error: (err) => {
        console.error('Dashboard trucks error', err);
      }

    });

  }

}