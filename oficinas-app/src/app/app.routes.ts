import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: 'notificacoes', canActivate: [authGuard], loadComponent: () => import('./notificacoes/notification-page.component').then(m => m.NotificationPageComponent) },
  { path: 'painel', canActivate: [authGuard], loadComponent: () => import('./painel/dashboard.component').then(m => m.DashboardComponent) },
  { path: 'perfil', canActivate: [authGuard], loadComponent: () => import('./oficina/shop-page.component').then(m => m.ShopPageComponent) },
  { path: 'clientes', canActivate: [authGuard], loadComponent: () => import('./cadastro/customer-vehicle-page.component').then(m => m.CustomerVehiclePageComponent), data: { pane: 'clientes' } },
  { path: 'veiculos', canActivate: [authGuard], loadComponent: () => import('./cadastro/customer-vehicle-page.component').then(m => m.CustomerVehiclePageComponent), data: { pane: 'veiculos' } },
  { path: 'abrir-ordem', canActivate: [authGuard], loadComponent: () => import('./ordem/service-order-page.component').then(m => m.ServiceOrderPageComponent) },
  { path: 'configuracoes/oficina', redirectTo: 'perfil', pathMatch: 'full' },
  { path: 'clientes-veiculos', redirectTo: 'clientes', pathMatch: 'full' },
  { path: 'ordens-servico', redirectTo: 'abrir-ordem', pathMatch: 'full' },
  { path: 'oficina/:slug', loadComponent: () => import('./oficina/public-shop.component').then(m => m.PublicShopComponent) },
  { path: 'acompanhar', loadComponent: () => import('./portal/portal-access.component').then(m => m.PortalAccessComponent) },
  { path: '', redirectTo: 'entrar', pathMatch: 'full' },
  { path: 'entrar', loadComponent: () => import('./auth/auth-page.component').then(m => m.AuthPageComponent), data: { mode: 'login' } },
  { path: 'cadastro', loadComponent: () => import('./auth/auth-page.component').then(m => m.AuthPageComponent), data: { mode: 'register' } },
  { path: 'recuperar-senha', loadComponent: () => import('./auth/auth-page.component').then(m => m.AuthPageComponent), data: { mode: 'recover' } },
  { path: 'redefinir-senha', loadComponent: () => import('./auth/auth-page.component').then(m => m.AuthPageComponent), data: { mode: 'reset' } },
  { path: 'inicio', redirectTo: 'painel', pathMatch: 'full' },
  { path: '**', redirectTo: 'entrar' }
];
