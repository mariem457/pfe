import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { MaintenanceLayoutComponent } from './layout/maintenance-layout/maintenance-layout.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { BatteryCapturesComponent } from './pages/battery-captures/battery-captures.component';
import { InterventionsComponent } from './pages/interventions/interventions.component';
import { TechnicianAlertsComponent } from './pages/technician-alerts/technician-alerts.component';

const routes: Routes = [
  {
    path: '',
    component: MaintenanceLayoutComponent,
    children: [
      { path: '', component: DashboardComponent },
      { path: 'battery-captures', component: BatteryCapturesComponent },
      { path: 'interventions', component: InterventionsComponent },
      { path: 'technician-alerts', component: TechnicianAlertsComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MaintenanceRoutingModule {}