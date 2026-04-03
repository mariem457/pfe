import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LandingPageComponent } from './features/public/landing-page/landing-page.component';
import { LoginPageComponent } from './features/auth/login-page/login-page.component';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, LandingPageComponent, LoginPageComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'front_end';

  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    this.themeService.initTheme();
  }
}