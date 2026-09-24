import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ShopService, PublicProfile } from './shop.service';
import { ShopIdentityComponent } from './shop-identity.component';

@Component({
  selector: 'app-public-shop', imports: [ShopIdentityComponent],
  template: `<main>
    @if (profile(); as shop) { <p class="brand">{{ shop.nome }}</p><app-shop-identity [profile]="shop" /> }
    @else if (error()) { <h1>Perfil indisponível</h1><p role="alert">Não foi possível carregar este perfil. Ele pode não estar publicado.</p><button (click)="load()">Tentar novamente</button> }
    @else { <p role="status">Carregando oficina…</p> }
    </main>`,
  styles: [`:host{display:block;min-height:100dvh;background:var(--app-bg);color:var(--ink)}main{max-width:720px;margin:auto;padding:32px 20px 56px;min-height:100dvh}.brand{display:flex;align-items:center;gap:10px;margin:0 0 30px;color:var(--ink);font-size:15px;font-weight:800}.brand::before{display:grid;width:32px;height:32px;place-items:center;border-radius:8px;background:var(--brand);color:#fff;content:'OF';font-size:11px;letter-spacing:.05em}button{min-height:44px;padding:10px 14px;border:1px solid var(--brand);border-radius:7px;background:var(--brand);color:#fff;font-weight:750;cursor:pointer}`]
})
export class PublicShopComponent implements OnInit {
  private readonly service = inject(ShopService);
  private readonly route = inject(ActivatedRoute);
  readonly profile = signal<PublicProfile | null>(null);
  readonly error = signal(false);
  ngOnInit() { void this.load(); }
  async load() {
    this.error.set(false);
    try { this.profile.set(await this.service.getPublic(this.route.snapshot.paramMap.get('slug') ?? '')); }
    catch { this.error.set(true); }
  }
}
