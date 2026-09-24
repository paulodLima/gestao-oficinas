import { Component, OnInit, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { OfficeSidebarComponent } from './layout/office-sidebar.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, OfficeSidebarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  title = 'oficinas-app';
  readonly showOfficeNavigation = signal(false);

  ngOnInit() {
    this.updateOfficeNavigation(this.router.url);
    this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(event => this.updateOfficeNavigation(event.urlAfterRedirects));
  }

  private updateOfficeNavigation(url: string) {
    const path = url.split('?')[0];
    this.showOfficeNavigation.set(['/painel', '/perfil', '/clientes', '/veiculos', '/abrir-ordem', '/inicio']
      .some(prefix => path === prefix || path.startsWith(prefix + '/')));
  }
}
