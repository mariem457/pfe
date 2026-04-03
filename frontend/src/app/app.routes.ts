import { Routes } from '@angular/router';
import { LandingPageComponent } from './features/public/landing-page/landing-page.component';
import { LoginPageComponent } from './features/auth/login-page/login-page.component';
import { RapportUserComponent } from './features/user/pages/rapport-user/rapport-user.component';
import { CreerUtilisateurComponent } from './features/admin-principal/pages/creer-utilisateur/creer-utilisateur.component';
import { GestionUtilisateursComponent } from './features/admin-principal/pages/gestion-utilisateurs/gestion-utilisateurs.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';

export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: 'login', component: LoginPageComponent },
  { path: 'rapport-user', component: RapportUserComponent },
  { path: 'admin/users', component: GestionUtilisateursComponent },
  { path: 'admin/users/create', component: CreerUtilisateurComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
{ path: 'reset-password', component: ResetPasswordComponent },

  {
    path: 'maintenance',
    loadChildren: () =>
      import('./features/maintenance/maintenance.module').then(m => m.MaintenanceModule),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['MAINTENANCE'] }
  },

  {
    path: 'admin',
    loadChildren: () =>
      import('./features/admin-principal/admin-principal.module').then(m => m.AdminPrincipalModule),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },

  {
    path: 'municipality',
    loadChildren: () =>
      import('./features/admin/admin.module').then(m => m.AdminModule),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['MUNICIPALITY'] }
  },

  {
    path: 'chauffeur',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] },
    loadChildren: () =>
      import('./features/chauffeur/chauffeur.module').then(m => m.ChauffeurModule),
  },

  { path: '**', redirectTo: '' },
];