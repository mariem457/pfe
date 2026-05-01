import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminPrincipalLayoutComponent } from './layout/admin-principal-layout/admin-principal-layout.component';
import { GestionUtilisateursComponent } from './pages/gestion-utilisateurs/gestion-utilisateurs.component';
import { GestionPoubellesComponent } from './pages/gestion-poubelles/gestion-poubelles.component';
import { ControleSystemeComponent } from './pages/controle-systeme/controle-systeme.component';
import { SecuriteComponent } from './pages/securite/securite.component';
const routes: Routes = [
  {
    path: '',
    component: AdminPrincipalLayoutComponent,
    children: [
      {
        path: '',
        redirectTo: 'gestion-utilisateurs',
        pathMatch: 'full'
      },
      {
        path: 'gestion-utilisateurs',
        component: GestionUtilisateursComponent
      },
      {
        path: 'gestion-poubelles',
        component: GestionPoubellesComponent
      },
      {
        path: 'controle-systeme',
        component: ControleSystemeComponent
      },
      {
        path: 'securite',
        component: SecuriteComponent
      }
          ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminPrincipalRoutingModule {}