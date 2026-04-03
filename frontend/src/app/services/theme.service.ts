import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly storageKey = 'app-theme';

  initTheme(): void {
    const savedTheme = localStorage.getItem(this.storageKey) || 'light';
    this.applyTheme(savedTheme);
  }

  toggleTheme(): void {
    const currentTheme = localStorage.getItem(this.storageKey) || 'light';
    const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
    this.applyTheme(nextTheme);
  }

  setTheme(theme: 'light' | 'dark'): void {
    this.applyTheme(theme);
  }

  isDarkMode(): boolean {
    return (localStorage.getItem(this.storageKey) || 'light') === 'dark';
  }

  private applyTheme(theme: string): void {
    localStorage.setItem(this.storageKey, theme);

    if (theme === 'dark') {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
  }
}