import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FooterComponent } from '../footer/footer.component';
import { MatIconModule } from '@angular/material/icon';
import { PublicHeaderComponent } from '../header/header.component';
import { ThemeService } from '../../../services/theme.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [PublicHeaderComponent, FooterComponent, RouterModule, MatIconModule],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.css'
})
export class LandingPageComponent implements OnInit {
  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    this.themeService.initTheme();
  }
}