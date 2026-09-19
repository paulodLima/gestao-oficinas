import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ShopService, PublicProfile } from './shop.service';
import { ShopIdentityComponent } from './shop-identity.component';

@Component({
  selector: 'app-public-shop', imports: [ShopIdentityComponent],
  template: `<main><p class="brand">/ gestão oficinas</p>
    @if (profile(); as shop) { <app-shop-identity [profile]="shop" /> }
    @else if (error()) { <h1>Perfil indisponível</h1><p role="alert">Não foi possível carregar este perfil. Ele pode não estar publicado.</p><button (click)="load()">Tentar novamente</button> }
    @else { <p role="status">Carregando oficina…</p> }
    </main>`,
  styles: [`main{max-width:640px;margin:auto;padding:32px 18px;min-height:100dvh}.brand{font-size:22px;margin-bottom:36px;color:#253e32}button{min-height:44px;padding:12px 24px}`]
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
