import { Component } from '@angular/core';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter, map } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-head-pages',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './head-pages.component.html',
  styleUrls: ['./head-pages.component.css']
})
export class HeadPagesComponent {

  title = '';

  constructor(private router: Router, private route: ActivatedRoute) {

    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        map(() => {
          let currentRoute = this.route;
          while (currentRoute.firstChild) {
            currentRoute = currentRoute.firstChild;
          }
          return currentRoute.snapshot.data['title'];
        })
      )
      .subscribe(title => this.title = title);
  }
}
