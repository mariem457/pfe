import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css'
})
export class LoginPageComponent {

  usernameOrEmail = '';
  password = '';
  rememberMe = true;
  errorMessage = '';

  private API = 'http://localhost:8083/api';

  constructor(private router: Router, private http: HttpClient) {}

  login() {
    this.errorMessage = '';

    this.http.post<any>(`${this.API}/auth/login`, {
      usernameOrEmail: this.usernameOrEmail.trim(),
      password: this.password
    }).subscribe({
      next: (res) => {
        const storage = this.rememberMe ? localStorage : sessionStorage;

        storage.setItem('token', res.token);
        storage.setItem('role', res.role);
        storage.setItem('userId', String(res.userId));

        if (res.role === 'DRIVER') {
          this.router.navigate(['/chauffeur']);
        } 
        else if (res.role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } 
        else if (res.role === 'MUNICIPALITY') {
          this.router.navigate(['/municipality']);
        } 
        else if (res.role === 'MAINTENANCE') {
          this.router.navigate(['/maintenance']);
        } 
        else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.errorMessage =
          err?.error?.message || 'Login failed. Vérifiez vos identifiants.';
      }
    });
  }
}