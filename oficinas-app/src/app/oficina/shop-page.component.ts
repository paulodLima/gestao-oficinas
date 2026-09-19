import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ShopService, ShopProfile, PublicProfile } from './shop.service';
import { ShopIdentityComponent } from './shop-identity.component';

@Component({
  selector: 'app-shop-page', imports: [ReactiveFormsModule, RouterLink, ShopIdentityComponent],
  templateUrl: './shop-page.component.html', styleUrl: './shop-page.component.css'
})
export class ShopPageComponent implements OnInit {
  private readonly service = inject(ShopService);
  private readonly builder = inject(FormBuilder);
  readonly shop = signal<ShopProfile | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly form = this.builder.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    telefone: ['', [Validators.pattern(/^[+0-9() .-]{8,30}$/)]],
    emailContato: ['', [Validators.email, Validators.maxLength(254)]],
    endereco: ['', Validators.maxLength(500)], horario: ['', Validators.maxLength(1000)],
    fuso: ['America/Sao_Paulo', [Validators.required, Validators.maxLength(80)]], perfilPublico: [false]
  });
  ngOnInit() { void this.load(); }
  async load() {
    this.success.set('');
    this.loading.set(true);
    this.error.set('');
    try { this.apply(await this.service.get()); }
    catch (error) { this.showError(error); }
    finally { this.loading.set(false); }
  }
  preview(): PublicProfile {
    const shop = this.shop();
    return { ...this.form.getRawValue(), logoUrl: shop?.temLogo ? '/api/oficina/logo?v=' + shop.versao : null };
  }
  async save() {
    this.success.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.form.controls.nome.value.trim()) {
      this.error.set('Confira o nome, o telefone, o e-mail e os limites dos campos.');
      return;
    }
    const shop = this.shop();
    if (!shop || this.busy()) return;
    await this.perform(async () => this.apply(await this.service.save({ ...this.form.getRawValue(), versao: shop.versao })));
  }
  async upload(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    const shop = this.shop();
    if (!file || !shop || this.busy()) return;
    this.success.set('');
    if (!['image/png', 'image/jpeg'].includes(file.type) || file.size > 2 * 1024 * 1024 || file.size === 0) {
      this.error.set('Escolha uma imagem PNG ou JPEG de até 2 MiB.');
      return;
    }
    await this.perform(async () => this.shop.set(await this.service.upload(file, shop.versao)));
  }
  async removeLogo() {
    const shop = this.shop();
    if (!shop || this.busy()) return;
    await this.perform(async () => this.shop.set(await this.service.remove(shop.versao)));
  }
  private apply(shop: ShopProfile) {
    this.shop.set(shop);
    this.form.patchValue(shop);
    this.form.markAsPristine();
  }
  private async perform(action: () => Promise<void>) {
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    try { await action(); this.success.set('Alteração salva.'); }
    catch (error) { this.showError(error); }
    finally { this.busy.set(false); }
  }
  private showError(error: unknown) {
    this.error.set(error instanceof HttpErrorResponse
      ? error.error?.detail ?? 'Não foi possível salvar ou carregar. Verifique a conexão e tente novamente.'
      : 'Não foi possível concluir. Tente novamente.');
  }
}
