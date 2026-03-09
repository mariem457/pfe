import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

type ReportStatus = 'Pending' | 'Assigned' | 'Resolved';
type Priority = 'High' | 'Medium' | 'Low';

interface ReportItem {
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

  hasImage?: boolean;
}

@Component({
  selector: 'app-public-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './public-reports.component.html',
  styleUrls: ['./public-reports.component.css']
})
export class PublicReportsComponent {

  query = '';
  statusFilter: 'All' | ReportStatus = 'All';

  // ===== Textes FR (UI) =====
  statusLabel(s: ReportStatus){
    return s === 'Pending' ? 'En attente'
         : s === 'Assigned' ? 'Affecté'
         : 'Résolu';
  }

  priorityLabel(p: Priority){
    return p === 'High' ? 'Élevée'
         : p === 'Medium' ? 'Moyenne'
         : 'Faible';
  }

  reports: ReportItem[] = [
    {
      code: 'IWT-A7K9M2B4',
      status: 'Pending',
      priority: 'High',
      timeAgo: 'il y a 2 heures',
      location: 'Rue Principale & 5e Avenue',
      description: 'Grand tas de sacs-poubelle laissé sur le trottoir, semble provenir d’un restaurant',
      lat: 40.7589,
      lng: -73.9851,
      hasImage: false
    },
    {
      code: 'IWT-B3N8P1L6',
      status: 'Assigned',
      priority: 'Medium',
      timeAgo: 'il y a 5 heures',
      location: 'Entrée Est du Parc Central',
      description: 'Bac public débordant près de la zone de jeux',
      lat: 40.7829,
      lng: -73.9654,
      assignedTo: 'TRK-002',
      hasImage: false
    },
    {
      code: 'IWT-C2M5K9D7',
      status: 'Resolved',
      priority: 'High',
      timeAgo: 'il y a 1 jour',
      location: 'Front du port – Quai 17',
      description: 'Bac cassé qui laisse fuir des déchets dans la rue',
      lat: 40.7061,
      lng: -74.0032,
      resolvedAgo: 'il y a 6 heures',
      hasImage: false
    },
    {
      code: 'IWT-D8L3J6H2',
      status: 'Pending',
      priority: 'High',
      timeAgo: 'il y a 3 heures',
      location: 'Place de l’Hôtel de Ville',
      description: 'Plusieurs sacs de gravats de chantier déposés illégalement',
      lat: 40.7128,
      lng: -74.0060,
      hasImage: false
    },
    {
      code: 'IWT-E5P9N4M1',
      status: 'Assigned',
      priority: 'Low',
      timeAgo: 'il y a 8 heures',
      location: 'Quartier commerçant – Broadway',
      description: 'Cartons qui bloquent le passage',
      lat: 40.7580,
      lng: -73.9855,
      assignedTo: 'TRK-001',
      hasImage: false
    },
    {
      code: 'IWT-F7K2L8N3',
      status: 'Resolved',
      priority: 'Medium',
      timeAgo: 'il y a 2 jours',
      location: 'Zone résidentielle – Rue Oak',
      description: 'Collecte manquée : les bacs sont toujours pleins depuis hier',
      lat: 40.7489,
      lng: -73.9680,
      resolvedAgo: 'il y a 1 jour',
      hasImage: false
    },
  ];

  get stats(){
    const total = this.reports.length;
    const pending = this.reports.filter(r => r.status === 'Pending').length;
    const assigned = this.reports.filter(r => r.status === 'Assigned').length;
    const resolved = this.reports.filter(r => r.status === 'Resolved').length;
    return { total, pending, assigned, resolved };
  }

  get filteredReports(){
    const q = this.query.trim().toLowerCase();

    return this.reports.filter(r => {
      const matchQuery =
        !q ||
        r.code.toLowerCase().includes(q) ||
        r.location.toLowerCase().includes(q) ||
        r.description.toLowerCase().includes(q);

      const matchStatus =
        this.statusFilter === 'All' ? true : r.status === this.statusFilter;

      return matchQuery && matchStatus;
    });
  }

  statusClass(s: ReportStatus){
    return s === 'Pending' ? 'is-pending' : s === 'Assigned' ? 'is-assigned' : 'is-resolved';
  }

  priorityClass(p: Priority){
    return p === 'High' ? 'is-high' : p === 'Medium' ? 'is-med' : 'is-low';
  }

  assignToTruck(r: ReportItem){
    alert(`Affecter ${r.code} à un camion (démo).`);
  }

  viewOnMap(r: ReportItem){
    alert(`Ouvrir la carte à ${r.lat}, ${r.lng} (démo).`);
  }
}
