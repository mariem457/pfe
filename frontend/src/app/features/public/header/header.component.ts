import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-public-header',
  standalone: true,
  imports: [RouterModule, MatIconModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class PublicHeaderComponent {
  isDark = false;

  toggleTheme(): void {
    this.isDark = !this.isDark;
    document.body.classList.toggle('dark-mode', this.isDark);
    localStorage.setItem('app-theme', this.isDark ? 'dark' : 'light');
  }

  ngOnInit(): void {
    const savedTheme = localStorage.getItem('app-theme');
    this.isDark = savedTheme === 'dark';
    document.body.classList.toggle('dark-mode', this.isDark);
  }
}