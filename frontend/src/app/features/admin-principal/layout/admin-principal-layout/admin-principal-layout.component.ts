import { Component } from '@angular/core';

import { SidebarComponent } from '../sidebar/sidebar.component';

import { RouterOutlet } from '@angular/router';
import { HeadPagesComponent } from '../head-pages/head-pages.component';
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [ RouterOutlet,SidebarComponent, HeadPagesComponent],
  templateUrl: './admin-principal-layout.component.html',
  styleUrl: './admin-principal-layout.component.css'
})
export class AdminPrincipalLayoutComponent {}
