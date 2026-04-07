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

  login(): void {
    this.errorMessage = '';

    console.log('email =', this.email);
    console.log('password =', this.password);

    if (!this.email || !this.email.trim()) {
      this.errorMessage = "L'email est obligatoire.";
      return;
    }

    if (!this.password || !this.password.trim()) {
      this.errorMessage = 'Le mot de passe est obligatoire.';
      return;
    }

    this.authService.login(
      this.email.trim(),
      this.password,
      this.rememberMe
    ).subscribe({
      next: (res) => {
        localStorage.setItem('accessToken', res.token);

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
      },
      error: (err) => {
        console.error('Login error:', err);
        this.errorMessage =
          err?.error?.message || 'Login failed. Vérifiez votre email et votre mot de passe.';
      }
    });
  }
}