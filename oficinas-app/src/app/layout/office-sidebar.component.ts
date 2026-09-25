import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';
import { ShopProfile, ShopService } from '../oficina/shop.service';

@Component({
  selector: 'app-office-sidebar',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar" aria-label="Navegação principal">
      <a class="identity" routerLink="/painel" aria-label="Ir para o painel">
        @if (office()?.temLogo) {
          <img src="/api/oficina/logo" [alt]="'Logo de ' + office()!.nome">
        } @else {
          <span class="logo-fallback" aria-hidden="true">{{ initials() }}</span>
        }
        <strong>{{ office()?.nome || 'Minha oficina' }}</strong>
      </a>

      <nav>
        <a routerLink="/avaliacoes" routerLinkActive="active" aria-label="Avaliações dos clientes">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8-6.2-3.2L5.8 21 7 14.2 2 9.3l6.9-1Z"/></svg><span>Avaliações</span>
        </a>
        <a routerLink="/notificacoes" routerLinkActive="active" aria-label="Central de avisos">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2a6 6 0 0 0-6 6v5l-2 3v2h16v-2l-2-3V8a6 6 0 0 0-6-6Zm0 20a3 3 0 0 0 3-3H9a3 3 0 0 0 3 3Z"/></svg><span>Avisos</span>
        </a>
        <a routerLink="/painel" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }" aria-label="Painel">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 13h6V4H4v9Zm0 7h6v-5H4v5Zm10 0h6v-9h-6v9Zm0-16v5h6V4h-6Z"/></svg><span>Painel</span>
        </a>
        <a routerLink="/perfil" routerLinkActive="active" aria-label="Perfil">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-4.42 0-8 2.02-8 4.5V20h16v-1.5c0-2.48-3.58-4.5-8-4.5Z"/></svg><span>Perfil</span>
        </a>
        <a routerLink="/clientes" routerLinkActive="active" aria-label="Clientes">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M16 11a4 4 0 1 0-3.99-4A4 4 0 0 0 16 11Zm-8 0a3 3 0 1 0-3-3 3 3 0 0 0 3 3Zm8 2c-2.67 0-8 1.34-8 4v3h16v-3c0-2.66-5.33-4-8-4ZM8 13c-.34 0-.72.02-1.12.05C5.25 13.38 2 14.26 2 16v3h4v-2c0-1.17.5-2.16 1.34-2.97A6.33 6.33 0 0 0 8 13Z"/></svg><span>Clientes</span>
        </a>
        <a routerLink="/veiculos" routerLinkActive="active" aria-label="Veículos">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 11 1.5-4.5h11L19 11v7h-2v-2H7v2H5v-7Zm2.2-2.5L6.7 10h10.6l-.5-1.5H7.2ZM8 14a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Zm8 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z"/></svg><span>Veículos</span>
        </a>
      </nav>

      <div class="sidebar-footer">
        <a class="open-order" routerLink="/abrir-ordem" aria-label="Abrir ordem de serviço">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2Z"/></svg>
          <span>Abrir ordem de serviço</span>
        </a>
        <button class="logout" type="button" aria-label="Sair da conta" (click)="logout()" [disabled]="busy()">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 4H4v16h6v-2H6V6h4V4Zm5 3-1.4 1.4 2.6 2.6H9v2h7.2l-2.6 2.6L15 17l5-5-5-5Z"/></svg>
          <span>{{ busy() ? 'Saindo…' : 'Sair da conta' }}</span>
        </button>
        @if (error()) { <p class="logout-error" role="alert">{{ error() }}</p> }
      </div>
    </aside>
  `,
  styles: `
    .logout{display:flex;align-items:center;gap:10px;width:100%;min-width:44px;min-height:44px;margin-top:8px;padding:8px 10px;border:1px solid var(--line);border-radius:8px;background:var(--surface);color:var(--ink);cursor:pointer;font-size:13px}
    .logout svg{width:20px;height:20px;flex:none;fill:currentColor}.logout-error{color:var(--danger);font-size:12px}
    @media(max-width:900px){:host .sidebar-footer{display:flex;gap:6px}.logout{width:44px;margin:0}.logout span{display:none}.logout-error{position:fixed;top:74px;right:10px;max-width:280px;padding:10px;background:var(--surface)}}
    :host { display: block; }
    .sidebar { position: fixed; inset: 0 auto 0 0; z-index: 20; width: var(--sidebar-width); display: flex; flex-direction: column; padding: 24px 14px 16px; background: var(--surface); border-right: 1px solid var(--line); }
    .identity { display: flex; align-items: center; gap: 10px; min-height: 52px; padding: 4px 8px 24px; color: var(--ink); text-decoration: none; border-bottom: 1px solid var(--line); }
    .identity img, .logo-fallback { width: 36px; height: 36px; flex: 0 0 36px; object-fit: cover; border-radius: 10px; }
    .logo-fallback { display: grid; place-items: center; background: var(--brand-soft); color: var(--brand); font-size: 13px; font-weight: 800; letter-spacing: .02em; }
    .identity strong { font-size: 14px; font-weight: 750; line-height: 1.3; overflow-wrap: anywhere; }
    nav { display: grid; gap: 3px; padding: 18px 0; }
    nav a, .open-order { display: flex; align-items: center; gap: 11px; min-height: 44px; padding: 0 10px; border-radius: 8px; color: var(--muted); text-decoration: none; font-size: 14px; font-weight: 600; transition: background .16s ease, color .16s ease; }
    nav svg, .open-order svg { width: 18px; height: 18px; flex: 0 0 18px; fill: currentColor; }
    nav a:hover { background: var(--surface-muted); color: var(--ink); }
    nav a.active { background: var(--brand-soft); color: var(--brand); }
    .sidebar-footer { margin-top: auto; padding-top: 12px; border-top: 1px solid var(--line); }
    .open-order { min-height: 50px; background: var(--brand); color: #fff; padding: 8px 12px; font-size: 13px; line-height: 1.25; }
    .open-order:hover { background: var(--brand-strong); }
    @media (max-width: 900px) { .sidebar { width: 100%; height: 74px; padding: 10px 14px; flex-direction: row; align-items: center; overflow-x: auto; } .identity { padding: 0 14px 0 0; border: 0; min-width: max-content; } .identity img, .logo-fallback { width: 32px; height: 32px; flex-basis: 32px; } .identity strong { max-width: 130px; } nav { display: flex; padding: 0; gap: 2px; } nav a { min-width: 44px; padding: 0 10px; } nav a span { display: none; } .sidebar-footer { margin: 0 0 0 6px; padding: 0; border: 0; } .open-order { min-height: 44px; min-width: 44px; padding: 0 10px; } .open-order span { display: none; } }
  `
})
export class OfficeSidebarComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly busy = signal(false);
  readonly error = signal('');
  private readonly shopService = inject(ShopService);
  readonly office = signal<ShopProfile | null>(null);

  ngOnInit() { void this.load(); }

  async logout() {
    if (this.busy()) return;
    this.busy.set(true); this.error.set('');
    try { await this.auth.logout(); await this.router.navigateByUrl('/entrar'); }
    catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.auth.owner.set(null); await this.router.navigateByUrl('/entrar');
      } else this.error.set('Não foi possível sair. Confira a conexão e tente novamente.');
    } finally { this.busy.set(false); }
  }

  initials() {
    return (this.office()?.nome || 'OF').split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }

  private async load() {
    try { this.office.set(await this.shopService.get()); } catch { this.office.set(null); }
  }
}
