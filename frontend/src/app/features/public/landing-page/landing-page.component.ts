import { Component } from '@angular/core';
import { HeaderComponent } from '../../admin/layout/header/header.component';
import { RouterModule } from '@angular/router';
import { FooterComponent } from '../../admin/layout/footer/footer.component';
import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [HeaderComponent, FooterComponent, RouterModule, MatIconModule],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.css'
})
export class LandingPageComponent {

}
