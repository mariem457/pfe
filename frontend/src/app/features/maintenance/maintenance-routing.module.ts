import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { MaintenanceLayoutComponent } from './layout/maintenance-layout/maintenance-layout.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { EtatBatteriesComponent } from './pages/etat-batteries/etat-batteries.component';
import { EtatCapteursComponent } from './pages/etat-capteurs/etat-capteurs.component';
import { TechnicianAlertsComponent } from './pages/technician-alerts/technician-alerts.component';

const routes: Routes = [
  {
    path: '',
    component: MaintenanceLayoutComponent,
    children: [
      {
  path: 'technician-alerts',
  component: TechnicianAlertsComponent,
  data: { title: 'Alertes techniques' }
},
      {
        path: '',
        component: DashboardComponent,
        data: { title: 'Dashboard Maintenance' }
      },
      {
        path: 'batteries',
        component: EtatBatteriesComponent,
        data: { title: 'État des batteries' }
      },
      {
        path: 'capteurs',
        component: EtatCapteursComponent,
        data: { title: 'État des capteurs' }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MaintenanceRoutingModule {}