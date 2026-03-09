import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { BinsComponent } from './pages/bins/bins.component';
import { TrucksComponent } from './pages/trucks/trucks.component';
import { OptimisationComponent } from './pages/optimisation/optimisation.component';
import { ParametreComponent } from './pages/parametre/parametre.component';
import { RapportComponent } from './pages/rapport/rapport.component';
import { PublicReportsComponent } from './pages/public-reports/public-reports.component';

const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent, data: { title: 'Overview' } },
      { path: 'bins', component: BinsComponent, data: { title: 'Smart Bins' } },
      { path: 'trucks', component: TrucksComponent, data: { title: 'Trucks' } },
      { path: 'optimisation', component: OptimisationComponent, data: { title: 'Optimisation' } },
      { path: 'parametres', component: ParametreComponent, data: { title: 'Paramètres' } },
      { path: 'rapport', component: RapportComponent, data: { title: 'Rapport' } },
      { path: 'public-reports', component: PublicReportsComponent, data: { title: 'Rapports Publics' } },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}