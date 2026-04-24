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
  email = '';
  successMessage = '';
  errorMessage = '';
  loading = false;
  submitted = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  getEmailValidationMessage(email: string): string {
    const value = email.trim();

    if (!value) {
      return "L'adresse e-mail est obligatoire.";
    }

    if (!value.includes('@')) {
      return "L'adresse e-mail doit contenir le symbole '@'.";
    }

    const parts = value.split('@');

    if (parts.length !== 2) {
      return "L'adresse e-mail ne doit contenir qu'un seul symbole '@'.";
    }

    const localPart = parts[0];
    const domainPart = parts[1];

    if (!localPart) {
      return "La partie avant le symbole '@' est obligatoire.";
    }

    if (!domainPart) {
      return "La partie après le symbole '@' est obligatoire.";
    }

    if (domainPart.startsWith('.')) {
      return "La partie après le symbole '@' ne doit pas commencer par un point.";
    }

    if (domainPart.endsWith('.')) {
      return "La partie après le symbole '@' ne doit pas se terminer par un point.";
    }

    if (domainPart.includes('..')) {
      return "Le domaine ne doit pas contenir deux points consécutifs.";
    }

    if (!domainPart.includes('.')) {
      return "Le domaine doit contenir un point, par exemple : gmail.com.";
    }

    if (/[,:;\s]/.test(domainPart)) {
      return "La partie après le symbole '@' ne doit pas contenir de caractères invalides comme ':', ',', ';' ou des espaces.";
    }

    if (/\s/.test(value)) {
      return "L'adresse e-mail ne doit pas contenir d'espaces.";
    }

    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$/;
    if (!emailRegex.test(value)) {
      return "Le format de l'adresse e-mail est invalide. Exemple valide : nom@domaine.com.";
    }

    return '';
  }

  get emailDetailedError(): string {
    return this.getEmailValidationMessage(this.email);
  }

  get hasEmailError(): boolean {
    return this.submitted && !!this.emailDetailedError;
  }

  submit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    const validationMessage = this.getEmailValidationMessage(this.email);

    if (validationMessage) {
      return;
    }

    const value = this.email.trim();
    this.loading = true;

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