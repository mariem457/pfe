import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent implements OnInit {
  isDark = false;

  ngOnInit(): void {
    this.isDark = localStorage.getItem('theme') === 'dark';
    document.body.classList.toggle('dark-mode', this.isDark);
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    localStorage.setItem('theme', this.isDark ? 'dark' : 'light');
    document.body.classList.toggle('dark-mode', this.isDark);
  }
}