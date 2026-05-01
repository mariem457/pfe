import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminPrincipalRoutingModule } from './admin-principal-routing.module';

import { FormsModule } from '@angular/forms';

import { GestionUtilisateursComponent } from './pages/gestion-utilisateurs/gestion-utilisateurs.component';
import { GestionPoubellesComponent } from './pages/gestion-poubelles/gestion-poubelles.component';
import { ControleSystemeComponent } from './pages/controle-systeme/controle-systeme.component';
import { SecuriteComponent } from './pages/securite/securite.component';
@NgModule({
  declarations: [
    GestionUtilisateursComponent,
    GestionPoubellesComponent,
    ControleSystemeComponent,
    SecuriteComponent
  ],
  imports: [
    CommonModule,
    AdminPrincipalRoutingModule,
    FormsModule
  ]
})
export class AdminPrincipalModule {}