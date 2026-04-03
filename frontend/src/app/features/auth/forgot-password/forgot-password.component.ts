import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  usernameOrEmail = '';
  successMessage = '';
  errorMessage = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.loading = true;

    const value = this.usernameOrEmail.trim();

    this.authService.forgotPassword(value).subscribe({
      next: () => {
        this.successMessage =
          'Si un compte existe, les instructions de réinitialisation ont été envoyées.';

        this.loading = false;

        setTimeout(() => {
          this.router.navigate(['/reset-password'], {
            queryParams: { email: value }
          });
        }, 1200);
      },
      error: (err: any) => {
        this.errorMessage = err?.error?.message || 'Erreur serveur.';
        this.loading = false;
      }
    });
  }
}