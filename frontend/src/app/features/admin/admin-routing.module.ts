import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { BinsComponent } from './pages/bins/bins.component';
import { TrucksComponent } from './pages/trucks/trucks.component';

import { ParametreComponent } from './pages/parametre/parametre.component';

import { PublicReportsComponent } from './pages/public-reports/public-reports.component';
import { MissionsComponent } from './pages/missions/missions.component';


const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent, data: { title: 'Overview' } },
      { path: 'bins', component: BinsComponent, data: { title: 'Smart Bins' } },
      { path: 'trucks', component: TrucksComponent, data: { title: 'Trucks' } },
      { path: 'missions', component: MissionsComponent, data: { title: 'Missions' } },

      { path: 'parametres', component: ParametreComponent, data: { title: 'Paramètres' } },
      
     
      { path: 'public-reports', component: PublicReportsComponent, data: { title: 'Rapports Publics' } }
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}