import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../../services/auth.service';
import { ThemeService } from '../../../../services/theme.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [MatIconModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  constructor(
    private authService: AuthService,
    private router: Router,
    private themeService: ThemeService
  ) {}

  onLogout(event?: Event): void {
    event?.preventDefault();

    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/']).then(() => {
          this.themeService.initTheme();
        });
      },
      error: () => {
        this.authService.clearSession();
        this.router.navigate(['/']).then(() => {
          this.themeService.initTheme();
        });
      }
    });
  }
}