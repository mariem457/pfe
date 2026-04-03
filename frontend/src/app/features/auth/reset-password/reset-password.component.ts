import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  identifier = '';
  code = '';
  newPassword = '';
  confirmPassword = '';
  showPassword = false;
  errorMessage = '';
  successMessage = '';
  loading = false;
  codeVerified = false;

  passwordChecks = {
    minLength: false,
    uppercase: false,
    lowercase: false,
    digit: false,
    special: false
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    this.identifier = this.route.snapshot.queryParamMap.get('email') || '';
    this.updatePasswordChecks();
  }

  get isCodeMode(): boolean {
    return !this.token;
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  updatePasswordChecks(): void {
    const password = this.newPassword || '';

    this.passwordChecks.minLength = password.length >= 8;
    this.passwordChecks.uppercase = /[A-Z]/.test(password);
    this.passwordChecks.lowercase = /[a-z]/.test(password);
    this.passwordChecks.digit = /\d/.test(password);
    this.passwordChecks.special = /[@$!%*?&._\-#]/.test(password);
  }

  isStrongPassword(): boolean {
    return Object.values(this.passwordChecks).every(Boolean);
  }

  generateStrongPassword(): void {
    const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
    const lower = 'abcdefghijkmnopqrstuvwxyz';
    const digits = '23456789';
    const special = '@$!%*?&._-#';

    const getRandomChar = (chars: string) =>
      chars[Math.floor(Math.random() * chars.length)];

    const requiredChars = [
      getRandomChar(upper),
      getRandomChar(lower),
      getRandomChar(digits),
      getRandomChar(special)
    ];

    const allChars = upper + lower + digits + special;

    while (requiredChars.length < 12) {
      requiredChars.push(getRandomChar(allChars));
    }

    for (let i = requiredChars.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [requiredChars[i], requiredChars[j]] = [requiredChars[j], requiredChars[i]];
    }

    this.newPassword = requiredChars.join('');
    this.confirmPassword = this.newPassword;
    this.updatePasswordChecks();
    this.errorMessage = '';
  }

  verifyCode(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.identifier.trim()) {
      this.errorMessage = "L'email ou le nom d'utilisateur est requis.";
      return;
    }

    if (!this.code.trim()) {
      this.errorMessage = 'Le code est requis.';
      return;
    }

    this.loading = true;

    this.authService.verifyResetCode(this.identifier.trim(), this.code.trim()).subscribe({
      next: () => {
        this.loading = false;
        this.codeVerified = true;
        this.successMessage =
          'Code vérifié. Vous pouvez maintenant définir un nouveau mot de passe.';
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage =
          err?.error?.fieldErrors?.code ||
          err?.error?.fieldErrors?.identifier ||
          err?.error?.message ||
          'Code invalide ou expiré.';
      }
    });
  }

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.updatePasswordChecks();

    if (!this.isStrongPassword()) {
      this.errorMessage =
        'Le mot de passe doit contenir au moins 8 caractères, avec majuscule, minuscule, chiffre et caractère spécial.';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.loading = true;

    if (this.token) {
      this.authService.resetPassword(this.token, this.newPassword).subscribe({
        next: () => {
          this.loading = false;
          this.successMessage = 'Mot de passe modifié avec succès.';
          setTimeout(() => this.router.navigate(['/login']), 1500);
        },
        error: (err: any) => {
          this.loading = false;
          this.errorMessage =
            err?.error?.fieldErrors?.newPassword ||
            err?.error?.message ||
            'Lien invalide ou expiré.';
        }
      });
      return;
    }

    this.authService.resetPasswordByCode(
      this.identifier.trim(),
      this.code.trim(),
      this.newPassword
    ).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Mot de passe modifié avec succès.';
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage =
          err?.error?.fieldErrors?.code ||
          err?.error?.fieldErrors?.identifier ||
          err?.error?.fieldErrors?.newPassword ||
          err?.error?.message ||
          'Erreur lors de la réinitialisation.';
      }
    });
  }
}