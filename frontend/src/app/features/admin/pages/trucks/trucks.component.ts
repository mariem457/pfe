import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FleetMapComponent } from './fleet-map/fleet-map.component';

type TruckStatus = 'Actif' | 'Inactif' | 'Hors ligne';

interface TruckCard {
  id: string;
  driver: string;
  location: string;
  status: TruckStatus;
  progress: number;      // 0..100
  collected: number;
  remaining: number;
  fuel: number;          // 0..100
  etaMins: number;
}

@Component({
  selector: 'app-trucks',
  standalone: true,
  imports: [CommonModule, FleetMapComponent],
  templateUrl: './trucks.component.html',
  styleUrls: ['./trucks.component.css']
})
export class TrucksComponent {

  kpis = [
    { icon: 'show_chart',        label: 'Camions actifs',         value: '24' },
    { icon: 'place',            label: 'Total des itinéraires',   value: '32' },
    { icon: 'schedule',         label: 'Progression moyenne',     value: '67 %' },
    { icon: 'local_gas_station',label: 'État du carburant',       value: 'Bon' },
  ];

  trucks: TruckCard[] = [
    {
      id: 'TRK-001',
      driver: 'John Smith',
      location: 'Rue Principale & 5e Avenue',
      status: 'Actif',
      progress: 65,
      collected: 18,
      remaining: 9,
      fuel: 78,
      etaMins: 45
    },
    {
      id: 'TRK-002',
      driver: 'Sarah Johnson',
      location: 'Parc Central',
      status: 'Actif',
      progress: 42,
      collected: 11,
      remaining: 16,
      fuel: 64,
      etaMins: 60
    },
    {
      id: 'TRK-003',
      driver: 'Ahmed Ali',
      location: 'Place de l’Hôtel de Ville',
      status: 'Actif',
      progress: 83,
      collected: 24,
      remaining: 5,
      fuel: 52,
      etaMins: 25
    },
  ];
}
