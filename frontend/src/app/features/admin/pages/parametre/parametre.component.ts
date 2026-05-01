import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  SettingsService,
  SettingsProfileResponse,
  UpdateSettingsProfileRequest,
  ChangePasswordRequest
} from '../../../../services/settings.service';
import { AuthService } from '../../../../services/auth.service';

@Component({
  selector: 'app-parametre',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './parametre.component.html',
  styleUrl: './parametre.component.css'
})
export class ParametreComponent implements OnInit {

  loadingProfile = false;
  savingProfile = false;
  savingPassword = false;

  successMessage = '';
  errorMessage = '';
  passwordSuccessMessage = '';
  passwordErrorMessage = '';

  forcePasswordChange = false;

  profile: SettingsProfileResponse = {
    firstName: '',
    lastName: '',
    email: '',
    function: '',
    organization: ''
  };

  notifications = {
    fullBinAlerts: true,
    routeOptimizationUpdates: true
  };

  preferences = {
    langue: 'Français',
    fuseauHoraire: 'Europe/Paris'
  };

  security = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  constructor(
    private settingsService: SettingsService,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProfile();

    this.route.queryParams.subscribe(params => {
      this.forcePasswordChange = params['forcePasswordChange'] === 'true'
        || this.authService.mustChangePassword;
    });
  }

  loadProfile(): void {
    this.loadingProfile = true;
    this.errorMessage = '';

    this.settingsService.getProfile().subscribe({
      next: (data) => {
        this.profile = data;
        this.loadingProfile = false;
      },
      error: (err) => {
        console.error('GET /api/settings/profile failed', err);
        this.errorMessage = 'Impossible de charger le profil.';
        this.loadingProfile = false;
      }
    });
  }

  saveProfile(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.profile.firstName || !this.profile.lastName || !this.profile.email) {
      this.errorMessage = 'Veuillez remplir les champs obligatoires du profil.';
      return;
    }

    this.savingProfile = true;

    const payload: UpdateSettingsProfileRequest = {
      firstName: this.profile.firstName,
      lastName: this.profile.lastName,
      email: this.profile.email,
      function: this.profile.function,
      organization: this.profile.organization
    };

    this.settingsService.updateProfile(payload).subscribe({
      next: (data) => {
        this.profile = data;
        this.successMessage = 'Les modifications du profil ont été enregistrées avec succès.';
        this.savingProfile = false;
      },
      error: (err) => {
        console.error('PUT /api/settings/profile failed', err);
        this.errorMessage = 'Impossible de mettre à jour le profil.';
        this.savingProfile = false;
      }
    });
  }

  updatePassword(): void {
    this.passwordSuccessMessage = '';
    this.passwordErrorMessage = '';

    if (!this.security.currentPassword || !this.security.newPassword || !this.security.confirmPassword) {
      this.passwordErrorMessage = 'Veuillez remplir tous les champs du mot de passe.';
      return;
    }

    if (this.security.newPassword !== this.security.confirmPassword) {
      this.passwordErrorMessage = 'La confirmation du mot de passe ne correspond pas.';
      return;
    }

    const password = this.security.newPassword;

    if (password.length < 8) {
      this.passwordErrorMessage = 'Le nouveau mot de passe doit contenir au moins 8 caractères.';
      return;
    }

    const strong =
      /[A-Z]/.test(password) &&
      /[a-z]/.test(password) &&
      /[0-9]/.test(password) &&
      /[^A-Za-z0-9]/.test(password);

    if (!strong) {
      this.passwordErrorMessage =
        'Le mot de passe doit contenir une majuscule, une minuscule, un chiffre et un caractère spécial.';
      return;
    }

    this.savingPassword = true;

    const payload: ChangePasswordRequest = {
      currentPassword: this.security.currentPassword,
      newPassword: this.security.newPassword,
      confirmPassword: this.security.confirmPassword
    };

    this.settingsService.changePassword(payload).subscribe({
      next: () => {
        this.passwordSuccessMessage = 'Le mot de passe a été mis à jour avec succès.';

        localStorage.setItem('mustChangePassword', 'false');
        sessionStorage.setItem('mustChangePassword', 'false');

        this.security = {
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        };

        this.savingPassword = false;
        this.forcePasswordChange = false;

        setTimeout(() => {
          const role = this.authService.role;

          if (role === 'ADMIN') {
            this.router.navigate(['/admin']);
          } else if (role === 'MUNICIPALITY') {
            this.router.navigate(['/municipality']);
          } else if (role === 'MAINTENANCE') {
            this.router.navigate(['/maintenance']);
          } else if (role === 'DRIVER') {
            this.router.navigate(['/chauffeur']);
          } else {
            this.router.navigate(['/']);
          }
        }, 1000);
      },
      error: (err) => {
        console.error('PUT /api/settings/password failed', err);
        this.passwordErrorMessage =
          err?.error?.message || 'Impossible de mettre à jour le mot de passe.';
        this.savingPassword = false;
      }
    });
  }
}