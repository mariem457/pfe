import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

type KpiIcon = 'map' | 'trend' | 'bolt' | 'spark';

interface KpiItem {
  icon: KpiIcon;
  label: string;
  value: string;
  delta: string;
}

interface HeatRow {
  zone: string;
  morning: number;
  afternoon: number;
  evening: number;
  night: number;
}

@Component({
  selector: 'app-optimisation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './optimisation.component.html',
  styleUrls: ['./optimisation.component.css'],
})
export class OptimisationComponent {

  kpis: KpiItem[] = [
    { icon: 'map',   label: 'Itinéraires optimisés',     value: '847',     delta: '+12 %' },
    { icon: 'trend', label: 'Carburant économisé',      value: '2 450 L',  delta: '+18 %' },
    { icon: 'bolt',  label: 'CO₂ réduit',               value: '6,4 t',    delta: '+15 %' },
    { icon: 'spark', label: 'Temps économisé',          value: '340 h',    delta: '+22 %' },
  ];

  // line chart (démo)
  days = ['Lun','Mar','Mer','Jeu','Ven','Sam','Dim'];
  predicted = [140, 155, 165, 150, 178, 168, 142];
  actual    = [135, 152, 160, 148, 180,   2,   2]; // démo: baisse forte samedi/dimanche

  // radar (démo)
  radar = {
    labels: [
      'Efficacité des itinéraires',
      'Économies de carburant',
      'Optimisation du temps',
      'Couverture',
      'Précision des prédictions'
    ],
    values: [90, 75, 82, 88, 80],
    score: 90
  };

  heatmap: HeatRow[] = [
    { zone: 'Centre-ville',   morning: 85, afternoon: 92, evening: 78, night: 45 },
    { zone: 'Résidentiel',    morning: 65, afternoon: 58, evening: 88, night: 35 },
    { zone: 'Commercial',     morning: 90, afternoon: 95, evening: 70, night: 25 },
    { zone: 'Industriel',     morning: 70, afternoon: 75, evening: 68, night: 60 },
  ];

  cellClass(v: number){
    if (v >= 80) return 'is-high';
    if (v >= 60) return 'is-med';
    return 'is-low';
  }

  generateRoute(){
    // action placeholder
    alert('Génération de l’itinéraire optimisé (démo)...');
  }

  exportHeatmap(){
    const headers = ['Zone','Matin','Après-midi','Soir','Nuit'];
    const lines = this.heatmap.map(r =>
      [r.zone, `${r.morning}%`, `${r.afternoon}%`, `${r.evening}%`, `${r.night}%`]
        .map(x => `"${String(x).replace(/"/g,'""')}"`).join(',')
    );
    const csv = [headers.join(','), ...lines].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'carte-thermique-dechets.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  // Helpers SVG pour le line chart
  yMin = 0;
  yMax = 180;

  toPoints(values: number[], w=520, h=260, pad=24){
    const xStep = (w - pad*2) / (values.length - 1);
    const yRange = this.yMax - this.yMin;
    return values.map((v,i)=>{
      const x = pad + i*xStep;
      const y = pad + (h - pad*2) * (1 - (v - this.yMin)/yRange);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  // Points du polygone radar
  radarPoints(values: number[], w=320, h=260){
    const cx = w/2, cy = h/2 + 10;
    const r = 95;
    const n = values.length;
    return values.map((v,i)=>{
      const ang = (-90 + i*(360/n)) * Math.PI/180;
      const rr = r*(v/100);
      const x = cx + rr*Math.cos(ang);
      const y = cy + rr*Math.sin(ang);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  radarGrid(level: number, w=320, h=260){
    const cx = w/2, cy = h/2 + 10;
    const r = 95*(level/100);
    const n = this.radar.values.length;
    return Array.from({length:n}).map((_,i)=>{
      const ang = (-90 + i*(360/n)) * Math.PI/180;
      const x = cx + r*Math.cos(ang);
      const y = cy + r*Math.sin(ang);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }
}
