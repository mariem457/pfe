import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';

import { MaintenanceRoutingModule } from './maintenance-routing.module';

import { MaintenanceLayoutComponent } from './layout/maintenance-layout/maintenance-layout.component';

import { HeaderComponent } from './layout/header/header.component';
import { SidebarComponent } from './layout/sidebar/sidebar.component';

import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { EtatBatteriesComponent } from './pages/etat-batteries/etat-batteries.component';
import { EtatCapteursComponent } from './pages/etat-capteurs/etat-capteurs.component';
import { TechnicianAlertsComponent } from './pages/technician-alerts/technician-alerts.component';

@NgModule({
  declarations: [
    MaintenanceLayoutComponent,
    HeaderComponent,
    SidebarComponent,
    DashboardComponent,
    EtatBatteriesComponent,
    TechnicianAlertsComponent,
    EtatCapteursComponent
  ],
  imports: [
    
    CommonModule,
    RouterModule,
    HttpClientModule,
    MatIconModule,
    MaintenanceRoutingModule
  ]
})
export class MaintenanceModule {}