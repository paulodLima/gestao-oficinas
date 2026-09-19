import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'entrar', pathMatch: 'full' },
  { path: 'entrar', loadComponent: () => import('./auth/auth-page.component').then(m => m.AuthPageComponent), data: { mode: 'login' } },
  { path: 'cadastro', loadComponent: () => import('./auth/auth-page.component').then(m => m.AuthPageComponent), data: { mode: 'register' } },
  { path: 'recuperar-senha', loadComponent: () => import('./auth/auth-page.component').then(m => m.AuthPageComponent), data: { mode: 'recover' } },
  { path: 'redefinir-senha', loadComponent: () => import('./auth/auth-page.component').then(m => m.AuthPageComponent), data: { mode: 'reset' } },
  { path: 'inicio', canActivate: [authGuard], loadComponent: () => import('./auth/home.component').then(m => m.HomeComponent) },
  { path: '**', redirectTo: 'entrar' }
];
