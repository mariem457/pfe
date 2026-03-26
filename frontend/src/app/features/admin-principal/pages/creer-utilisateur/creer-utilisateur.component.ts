import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserService } from '../../../../services/user.service';

type RoleCreation = 'DRIVER' | 'ADMIN' | 'MUNICIPALITY';

@Component({
  selector: 'app-creer-utilisateur',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './creer-utilisateur.component.html',
  styleUrls: ['./creer-utilisateur.component.css']
})
export class CreerUtilisateurComponent {
  loading = false;
  successMessage = '';
  errorMessage = '';

  form = {
    fullName: '',
    username: '',
    email: '',
    phone: '',
    role: 'DRIVER' as RoleCreation,
    vehicleCode: '',
    isEnabled: true
  };

  constructor(
    private userService: UserService,
    private router: Router
  ) {}

  get isDriver(): boolean {
    return this.form.role === 'DRIVER';
  }

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.form.fullName.trim()) {
      this.errorMessage = 'Le nom complet est obligatoire';
      return;
    }

    if (!this.form.username.trim()) {
      this.errorMessage = 'Le nom d’utilisateur est obligatoire';
      return;
    }

    if (!this.form.email.trim()) {
      this.errorMessage = 'L’email est obligatoire';
      return;
    }

    if (this.isDriver) {
      if (!this.form.phone.trim()) {
        this.errorMessage = 'Le téléphone est obligatoire pour un chauffeur';
        return;
      }

      if (!this.form.vehicleCode.trim()) {
        this.errorMessage = 'Le code véhicule est obligatoire pour un chauffeur';
        return;
      }
    }

    this.loading = true;

    if (this.isDriver) {
      const payload = {
        fullName: this.form.fullName.trim(),
        username: this.form.username.trim(),
        email: this.form.email.trim(),
        phone: this.form.phone.trim(),
        vehicleCode: this.form.vehicleCode.trim()
      };

      this.userService.createDriver(payload).subscribe({
        next: () => {
          this.loading = false;
          this.successMessage = 'Chauffeur créé avec succès. Les identifiants ont été envoyés par SMS.';
          setTimeout(() => {
            this.router.navigate(['/admin/users']);
          }, 1200);
        },
        error: (err: any) => {
          console.error('Erreur création chauffeur', err);
          this.loading = false;
          this.errorMessage = err?.error?.message || 'Impossible de créer le chauffeur';
        }
      });

      return;
    }

    const payload = {
      fullName: this.form.fullName.trim(),
      username: this.form.username.trim(),
      email: this.form.email.trim(),
      phone: this.form.phone.trim(),
      role: this.form.role,
      isEnabled: this.form.isEnabled
    };

    this.userService.createUser(payload).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Utilisateur créé avec succès.';
        setTimeout(() => {
          this.router.navigate(['/admin/users']);
        }, 1200);
      },
      error: (err: any) => {
        console.error('Erreur création utilisateur', err);
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Impossible de créer l’utilisateur';
      }
    });
  }

  annuler(): void {
    this.router.navigate(['/admin/users']);
  }
}