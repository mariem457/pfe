import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ChauffeurLayoutComponent } from './layout/chauffeur-layout/chauffeur-layout.component';
import { ChauffeurDashboardComponent } from './pages/dashboard/chauffeur-dashboard.component';
import { ProfilComponent } from './pages/profil/profil.component';
import { ParametresComponent } from './pages/parametres/parametres.component';
import { SupportComponent } from './pages/support/support.component';
const routes: Routes = [
  {
    path: '',
    component: ChauffeurLayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: ChauffeurDashboardComponent },
      { path: 'profil', component: ProfilComponent },
      { path: 'parametres', component: ParametresComponent },
      { path: 'support', component: SupportComponent },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ChauffeurRoutingModule {}