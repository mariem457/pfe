import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminPrincipalDashboardComponent } from './pages/admin-principal-dashboard/admin-principal-dashboard.component';

const routes: Routes = [
  { path: '', component: AdminPrincipalDashboardComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminPrincipalRoutingModule {}