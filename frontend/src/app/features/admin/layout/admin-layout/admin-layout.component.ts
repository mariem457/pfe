import { Component } from '@angular/core';

import { SidebarComponent } from '../sidebar/sidebar.component';

import { RouterOutlet } from '@angular/router';
import { HeadPagesComponent } from '../head-pages/head-pages.component';
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [ RouterOutlet,SidebarComponent, HeadPagesComponent],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.css'
})
export class AdminLayoutComponent {}
