import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AddressSuggestion, ShopService, ShopProfile, PublicProfile } from './shop.service';
import { ShopIdentityComponent } from './shop-identity.component';
import { ShopThemeService } from './shop-theme.service';

type WeekDay = 'segunda' | 'terca' | 'quarta' | 'quinta' | 'sexta' | 'sabado' | 'domingo';
type DaySchedule = { fechado: boolean; abertura: string; fechamento: string };
type ThemeName = 'verde' | 'azul' | 'amarelo' | 'vermelho' | 'preto' | 'roxo';

@Component({
  selector: 'app-shop-page', imports: [ReactiveFormsModule, RouterLink, ShopIdentityComponent],
  templateUrl: './shop-page.component.html', styleUrl: './shop-page.component.css'
})
export class ShopPageComponent implements OnInit {
  private readonly service = inject(ShopService);
  private readonly theme = inject(ShopThemeService);
  private readonly builder = inject(FormBuilder);
  readonly shop = signal<ShopProfile | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly addressSuggestions = signal<AddressSuggestion[]>([]);
  readonly addressSearching = signal(false);
  readonly addressLookupError = signal('');
  readonly addressSearched = signal(false);
  private addressTimer: ReturnType<typeof setTimeout> | undefined;
  readonly days: Array<{ key: WeekDay; label: string }> = [
    { key: 'segunda', label: 'Segunda-feira' }, { key: 'terca', label: 'Terça-feira' },
    { key: 'quarta', label: 'Quarta-feira' }, { key: 'quinta', label: 'Quinta-feira' },
    { key: 'sexta', label: 'Sexta-feira' }, { key: 'sabado', label: 'Sábado' },
    { key: 'domingo', label: 'Domingo' }
  ];
  readonly schedule = this.builder.nonNullable.group({
    segunda: this.day(false, '08:00', '18:00'), terca: this.day(false, '08:00', '18:00'),
    quarta: this.day(false, '08:00', '18:00'), quinta: this.day(false, '08:00', '18:00'),
    sexta: this.day(false, '08:00', '18:00'), sabado: this.day(false, '08:00', '12:00'),
    domingo: this.day(true, '', '')
  });
  readonly form = this.builder.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    telefone: ['', [Validators.pattern(/^$|^\(\d{2}\) \d{4,5}-\d{4}$/)]],
    emailContato: ['', [Validators.email, Validators.maxLength(254)]],
    endereco: ['', Validators.maxLength(500)], horario: ['', Validators.maxLength(1000)]
  });
  readonly themes: Array<{ id: ThemeName; label: string; corMenu: string; corMenuAtivo: string; corDestaque: string }> = [
    { id: 'verde', label: 'Verde clássico', corMenu: '#52695F', corMenuAtivo: '#E3ECD9', corDestaque: '#4D7063' },
    { id: 'azul', label: 'Azul', corMenu: '#294B68', corMenuAtivo: '#DDEBF5', corDestaque: '#356E9E' },
    { id: 'amarelo', label: 'Amarelo', corMenu: '#69551F', corMenuAtivo: '#FFF0BE', corDestaque: '#B88718' },
    { id: 'vermelho', label: 'Vermelho', corMenu: '#692F36', corMenuAtivo: '#F9E0E2', corDestaque: '#B74C58' },
    { id: 'preto', label: 'Preto', corMenu: '#27282B', corMenuAtivo: '#E5E5E5', corDestaque: '#4B4D52' },
    { id: 'roxo', label: 'Roxo', corMenu: '#493966', corMenuAtivo: '#EAE2F5', corDestaque: '#7653A6' }
  ];
  readonly colors = this.builder.nonNullable.group({ tema: ['verde' as ThemeName] });
  readonly defaultColors = { corMenu: '#52695F', corMenuAtivo: '#E3ECD9', corDestaque: '#4D7063' };
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
    return { ...this.form.getRawValue(), corMenu: shop?.corMenu ?? this.defaultColors.corMenu, corMenuAtivo: shop?.corMenuAtivo ?? this.defaultColors.corMenuAtivo, corDestaque: shop?.corDestaque ?? this.defaultColors.corDestaque, horario: this.scheduleText(), fuso: shop?.fuso ?? 'America/Sao_Paulo', logoUrl: shop?.temLogo ? '/api/oficina/logo?v=' + shop.versao : null };
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
    if (!this.validSchedule()) { this.error.set('Informe abertura e fechamento para cada dia que estiver aberto.'); return; }
    await this.perform(async () => this.apply(await this.service.save({ ...this.form.getRawValue(), horario: JSON.stringify(this.schedule.getRawValue()),
      fuso: shop.fuso || 'America/Sao_Paulo', perfilPublico: true, versao: shop.versao })));
  }
  async saveColors() {
    const shop = this.shop();
    if (!shop || this.busy()) return;
    await this.perform(async () => this.apply(await this.service.save({ ...this.selectedColors(), versao: shop.versao })));
  }
  previewTheme() { this.theme.apply(this.selectedColors()); }
  async restoreColors() {
    const shop = this.shop();
    if (!shop || this.busy()) return;
    this.colors.patchValue({ tema: 'verde' });
    await this.perform(async () => this.apply(await this.service.save({ ...this.defaultColors, versao: shop.versao })));
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
  async uploadCover(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    const shop = this.shop();
    if (!file || !shop || this.busy()) return;
    if (!['image/png', 'image/jpeg'].includes(file.type) || !file.size || file.size > 2 * 1024 * 1024) {
      this.error.set('Escolha uma imagem PNG ou JPEG de até 2 MiB.');
      return;
    }
    await this.perform(async () => this.shop.set(await this.service.saveCover(file, shop.versao)));
  }
  async removeCover() {
    const shop = this.shop();
    if (!shop || this.busy()) return;
    await this.perform(async () => this.shop.set(await this.service.saveCover(null, shop.versao)));
  }
  formatPhone() {
    const digits = this.form.controls.telefone.value.replace(/\D/g, '').slice(0, 11);
    const value = digits.length <= 2 ? digits : digits.length <= 6 ? `(${digits.slice(0, 2)}) ${digits.slice(2)}`
      : digits.length <= 10 ? `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`
      : `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
    this.form.controls.telefone.setValue(value, { emitEvent: false });
  }
  searchAddress() {
    const query = this.form.controls.endereco.value.trim();
    this.addressSuggestions.set([]);
    this.addressLookupError.set('');
    this.addressSearched.set(false);
    if (this.addressTimer) clearTimeout(this.addressTimer);
    if (query.length < 3) { this.addressSearching.set(false); return; }
    this.addressSearching.set(true);
    this.addressTimer = setTimeout(async () => {
      try {
        this.addressSuggestions.set(await this.service.addressSuggestions(query));
        this.addressSearched.set(true);
      }
      catch (error) {
        this.addressSuggestions.set([]);
        this.addressLookupError.set(error instanceof HttpErrorResponse
          ? error.error?.detail ?? 'Não foi possível buscar endereços agora.'
          : 'Não foi possível buscar endereços agora.');
      }
      finally { this.addressSearching.set(false); }
    }, 700);
  }
  chooseAddress(suggestion: AddressSuggestion) {
    this.form.controls.endereco.setValue(suggestion.endereco);
    this.addressSuggestions.set([]);
  }
  dayControl(day: WeekDay) { return this.schedule.controls[day]; }
  private day(fechado: boolean, abertura: string, fechamento: string) {
    return this.builder.nonNullable.group({ fechado, abertura, fechamento });
  }
  private validSchedule() {
    return this.days.every(day => {
      const value = this.dayControl(day.key).getRawValue();
      return value.fechado || (/^\d{2}:\d{2}$/.test(value.abertura) && /^\d{2}:\d{2}$/.test(value.fechamento));
    });
  }
  private scheduleText() {
    return this.days.map(day => {
      const value = this.dayControl(day.key).getRawValue();
      return value.fechado ? `${day.label}: fechado` : `${day.label}: ${value.abertura} às ${value.fechamento}`;
    }).join('\n');
  }
  private apply(shop: ShopProfile) {
    this.shop.set(shop);
    this.form.patchValue(shop);
    this.colors.patchValue({ tema: this.themeName(shop) });
    this.theme.apply(shop);
    this.applySchedule(shop.horario);
    this.form.markAsPristine();
  }
  selectedTheme() {
    return this.themes.find(theme => theme.id === this.colors.controls.tema.value) ?? this.themes[0];
  }
  private selectedColors() {
    const { corMenu, corMenuAtivo, corDestaque } = this.selectedTheme();
    return { corMenu, corMenuAtivo, corDestaque };
  }
  private themeName(shop: ShopProfile): ThemeName {
    return this.themes.find(theme => theme.corMenu === shop.corMenu && theme.corMenuAtivo === shop.corMenuAtivo && theme.corDestaque === shop.corDestaque)?.id ?? 'verde';
  }
  private applySchedule(value: string) {
    try {
      const parsed = JSON.parse(value) as Partial<Record<WeekDay, DaySchedule>>;
      for (const day of this.days) {
        const item = parsed[day.key];
        if (item && typeof item.fechado === 'boolean' && typeof item.abertura === 'string' && typeof item.fechamento === 'string') {
          this.dayControl(day.key).patchValue(item);
        }
      }
    } catch { /* Horários antigos em texto permanecem editáveis na nova grade. */ }
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
