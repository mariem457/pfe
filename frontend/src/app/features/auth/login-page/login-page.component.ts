import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css'
})
export class LoginPageComponent {
  email = '';
  password = '';
  rememberMe = true;
  showPassword = false;
  errorMessage = '';

  successMessage = '';
  showSuccessToast = false;

  showErrorToast = false;

  isLoading = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  forgotPassword(): void {
    this.router.navigate(['/forgot-password']);
  }

  showSuccess(message: string): void {
    this.successMessage = message;
    this.showSuccessToast = true;
  }

  hideSuccess(): void {
    this.showSuccessToast = false;
    this.successMessage = '';
  }

  showError(message: string): void {
    this.errorMessage = message;
    this.showErrorToast = true;
  }

  hideError(): void {
    this.showErrorToast = false;
    this.errorMessage = '';
  }

  login(): void {
    this.hideError();
    this.hideSuccess();

    if (!this.email || !this.email.trim()) {
      this.showError("L'email est obligatoire.");
      return;
    }

    if (!this.password || !this.password.trim()) {
      this.showError('Le mot de passe est obligatoire.');
      return;
    }

    this.isLoading = true;

    this.authService.login(
      this.email.trim(),
      this.password,
      this.rememberMe
    ).subscribe({
      next: (res) => {
        this.showSuccess('Utilisateur connecté avec succès');

        const navigateAfterToast = () => {
          this.hideSuccess();

          if (res.mustChangePassword) {
            if (res.role === 'MUNICIPALITY') {
              this.router.navigate(['/municipality/parametres'], {
                queryParams: { forcePasswordChange: 'true' }
              });
              return;
            }

            if (res.role === 'ADMIN') {
              this.router.navigate(['/admin/parametres'], {
                queryParams: { forcePasswordChange: 'true' }
              });
              return;
            }

            if (res.role === 'MAINTENANCE') {
              this.router.navigate(['/maintenance/parametres'], {
                queryParams: { forcePasswordChange: 'true' }
              });
              return;
            }

            if (res.role === 'DRIVER') {
              this.router.navigate(['/chauffeur/parametres'], {
                queryParams: { forcePasswordChange: 'true' }
              });
              return;
            }
          }

          if (res.role === 'DRIVER') {
            this.router.navigate(['/chauffeur']);
          } else if (res.role === 'ADMIN') {
            this.router.navigate(['/admin']);
          } else if (res.role === 'MUNICIPALITY') {
            this.router.navigate(['/municipality']);
          } else if (res.role === 'MAINTENANCE') {
            this.router.navigate(['/maintenance']);
          } else {
            this.router.navigate(['/']);
          }
        };

        setTimeout(() => {
          navigateAfterToast();
        }, 10000);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Login error:', err);

        this.showError(
          err?.error?.message || 'Les identifications sont erronées'
        );

        setTimeout(() => {
          this.hideError();
        }, 5000);
      }
    });
  }
}