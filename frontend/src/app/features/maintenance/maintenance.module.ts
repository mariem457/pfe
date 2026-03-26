import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { MaintenanceRoutingModule } from './maintenance-routing.module';
import { MaintenanceLayoutComponent } from './layout/maintenance-layout/maintenance-layout.component';
import { FooterComponent } from './layout/footer/footer.component';
import { HeadPagesComponent } from './layout/head-pages/head-pages.component';
import { HeaderComponent } from './layout/header/header.component';
import { SidebarComponent } from './layout/sidebar/sidebar.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { BatteryCapturesComponent } from './pages/battery-captures/battery-captures.component';
import { InterventionsComponent } from './pages/interventions/interventions.component';
import { TechnicianAlertsComponent } from './pages/technician-alerts/technician-alerts.component';
import { MatIcon } from "@angular/material/icon";

@NgModule({
  declarations: [
    MaintenanceLayoutComponent,
    FooterComponent,
    HeadPagesComponent,
    HeaderComponent,
    SidebarComponent,
    DashboardComponent,
    BatteryCapturesComponent,
    InterventionsComponent,
    TechnicianAlertsComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    MaintenanceRoutingModule,
    MatIcon
]
})
export class MaintenanceModule {}