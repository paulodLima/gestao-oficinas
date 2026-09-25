import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';

interface PortalVehicle { id: string; placa: string; veiculo: string; }
interface PortalService {
  id: string; numero: number; status: string; previsaoEm: string;
  placa: string; veiculo: string; oficina: string;
}
interface PortalPhoto { id: string; etapa: string; legenda: string; createdAt: string; }

@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <main class="portal-shell">
      <header><span class="mark" aria-hidden="true">OF</span><strong>Acompanhamento do serviço</strong></header>
      @if (!authenticated()) {
        <section class="access-card" aria-labelledby="access-title">
          <p class="eyebrow">ÁREA DO CLIENTE</p>
          <h1 id="access-title">{{ sent() ? 'Confirme seu acesso' : 'Acompanhe seu veículo' }}</h1>
          <p class="intro">{{ sent() ? 'Informe o código enviado ao contato cadastrado.' : 'Use a oficina e a placa para solicitar um código de acesso seguro.' }}</p>
          @if (!sent()) {
            <label for="portal-shop">Identificador da oficina</label>
            <input id="portal-shop" [(ngModel)]="slug" placeholder="ex.: oficina-central" autocomplete="organization">
            <label for="portal-plate">Placa do veículo</label>
            <input id="portal-plate" [(ngModel)]="plate" placeholder="ABC1D23" autocapitalize="characters" autocomplete="off">
            <button type="button" [disabled]="busy() || !slug.trim() || !plate.trim()" (click)="request()">{{ busy() ? 'Enviando…' : 'Receber código' }}</button>
          } @else {
            <label for="portal-code">Código de 6 dígitos</label>
            <input id="portal-code" [(ngModel)]="code" inputmode="numeric" maxlength="6" autocomplete="one-time-code">
            <button type="button" [disabled]="busy() || code.length !== 6" (click)="confirm()">{{ busy() ? 'Validando…' : 'Entrar no acompanhamento' }}</button>
            <button type="button" class="button-link" [disabled]="busy()" (click)="restart()">Solicitar outro código</button>
          }
          @if (message()) { <p class="message" role="status">{{ message() }}</p> }
        </section>
      } @else {
        @if (vehicles().length > 1) {
          <section class="vehicle-picker">
            <label for="portal-vehicle">Veículo acompanhado</label>
            <select id="portal-vehicle" [(ngModel)]="selectedVehicleId" (ngModelChange)="loadService($event)">
              @for (vehicle of vehicles(); track vehicle.id) {
                <option [value]="vehicle.id">{{ vehicle.placa }} · {{ vehicle.veiculo }}</option>
              }
            </select>
          </section>
        }
        @if (service(); as current) {
          <section class="service-head">
            <p class="eyebrow">SERVIÇO EM ANDAMENTO · OS {{ current.numero }}</p>
            <h1>{{ current.veiculo }}</h1><p class="plate">{{ current.placa }}</p>
            <div class="status-row"><span class="status">{{ statusLabel(current.status) }}</span><span>Previsão: {{ current.previsaoEm || 'a confirmar' }}</span></div>
          </section>
          <section class="progress" aria-label="Andamento do serviço"><div class="progress-line"></div><article><i></i><strong>Recebido</strong><small>Veículo na oficina</small></article><article><i></i><strong>{{ statusLabel(current.status) }}</strong><small>Etapa atual</small></article><article class="muted"><i></i><strong>Pronto</strong><small>Retirada prevista</small></article></section>
          <section class="gallery"><div class="section-title"><div><p class="eyebrow">REGISTROS DA OFICINA</p><h2>Fotos do serviço</h2></div><span>{{ photos().length }} foto(s)</span></div>
            @if (photos().length) { <div class="photo-grid">@for (photo of photos(); track photo.id) { <figure><img [src]="photoUrl(photo.id)" [alt]="photo.legenda || 'Registro do serviço'"><figcaption>{{ photo.etapa }}</figcaption></figure> }</div> }
            @else { <p class="empty">A oficina ainda não publicou fotos deste serviço.</p> }
          </section>
        } @else {
          <section class="access-card empty-state"><p class="eyebrow">ACOMPANHAMENTO</p><h1>Nenhum serviço em andamento</h1><p class="intro">Quando a oficina abrir um serviço autorizado para este veículo, ele aparecerá aqui.</p></section>
        }
      }
    </main>`,
  styles: `:host{display:block;min-height:100dvh;background:var(--app-bg);color:var(--ink)}.portal-shell{max-width:940px;margin:auto;padding:28px 20px 56px}.portal-shell>header{display:flex;align-items:center;gap:10px;margin-bottom:38px;font-size:14px}.mark{display:grid;width:32px;height:32px;place-items:center;border-radius:8px;background:var(--brand);color:#fff;font-size:11px;font-weight:800}.access-card{max-width:460px;margin:8vh auto;padding:28px;border:1px solid var(--line);border-radius:12px;background:var(--surface)}.eyebrow{margin:0 0 8px;color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.1em}h1{margin:0;color:var(--ink);font-size:clamp(27px,5vw,40px);letter-spacing:-.04em;line-height:1.15}.intro{margin:10px 0 22px;color:var(--muted);line-height:1.6}label{display:block;margin:16px 0 6px;color:#4f5b6d;font-size:12px;font-weight:750}input,select{display:block;width:100%;min-height:46px;padding:10px 12px;border:1px solid var(--line);border-radius:7px;background:#fff;color:var(--ink)}input:focus-visible,select:focus-visible,button:focus-visible{outline:3px solid color-mix(in srgb,var(--brand) 28%,transparent);outline-offset:2px}button{width:100%;min-height:46px;margin-top:20px;border:1px solid var(--brand);border-radius:7px;background:var(--brand);color:#fff;font-weight:750;cursor:pointer;transition:filter .18s ease,transform .18s ease}button:hover:not(:disabled){filter:brightness(.92)}button:active:not(:disabled){transform:translateY(1px)}button:disabled{cursor:not-allowed;opacity:.55}.button-link{min-height:38px;margin-top:8px;border-color:transparent;background:transparent;color:var(--brand)}.message{margin:16px 0 0;padding:10px;border-radius:7px;background:#eef4ff;color:var(--brand);font-size:13px}.vehicle-picker{max-width:420px;margin:0 0 24px}.vehicle-picker label{margin-top:0}.service-head{padding-bottom:24px;border-bottom:1px solid var(--line)}.plate{display:inline-block;margin:10px 0 0;padding:5px 8px;border:1px solid #aebbd0;border-radius:5px;color:#314866;font:750 13px ui-monospace,SFMono-Regular,Menlo,monospace}.status-row{display:flex;gap:12px;align-items:center;margin-top:18px;color:var(--muted);font-size:13px}.status{padding:5px 8px;border-radius:99px;background:var(--brand-soft);color:var(--brand);font-size:11px;font-weight:800}.progress{position:relative;display:grid;grid-template-columns:repeat(3,1fr);gap:10px;padding:30px 0}.progress-line{position:absolute;top:36px;right:12%;left:12%;height:2px;background:#c7d3e7}.progress article{position:relative;z-index:1;display:grid;justify-items:center;gap:4px;text-align:center}.progress i{width:14px;height:14px;border:3px solid var(--brand-soft);border-radius:50%;background:var(--brand)}.progress small{color:var(--muted);font-size:12px}.progress .muted i{border-color:#e2e6ed;background:#fff}.progress .muted strong,.progress .muted small{color:var(--muted)}.gallery{padding:22px;border:1px solid var(--line);border-radius:12px;background:var(--surface)}.section-title{display:flex;justify-content:space-between;gap:12px;align-items:start}.section-title h2{margin:0;font-size:19px}.section-title span{color:var(--muted);font-size:12px}.photo-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;margin-top:18px}.photo-grid figure{margin:0;overflow:hidden;border-radius:8px;background:#f4f7fc}.photo-grid img{display:block;width:100%;aspect-ratio:1;object-fit:cover}.photo-grid figcaption{padding:7px;color:var(--muted);font-size:11px}.empty{margin:18px 0 0;color:var(--muted);font-size:13px}.empty-state{margin-top:4vh}@media(max-width:580px){.portal-shell{padding:20px 16px 40px}.access-card{margin:4vh auto;padding:20px}.progress{gap:4px}.progress strong{font-size:12px}.progress small{font-size:10px}.photo-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.gallery{padding:16px}.status-row{align-items:flex-start;flex-direction:column}}@media(prefers-reduced-motion:reduce){button{transition:none}}`
})
export class PortalAccessComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  slug = '';
  plate = '';
  code = '';
  id = '';
  selectedVehicleId = '';
  readonly sent = signal(false);
  readonly authenticated = signal(false);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly vehicles = signal<PortalVehicle[]>([]);
  readonly service = signal<PortalService | null>(null);
  readonly photos = signal<PortalPhoto[]>([]);

  async ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) return;
    this.busy.set(true);
    try {
      await firstValueFrom(this.http.post('/api/portal/acesso/link', { token }, { headers: await this.headers() }));
      this.authenticated.set(true);
      await this.loadService();
    } catch {
      this.message.set('Este link é inválido, expirou ou foi revogado.');
    } finally {
      this.busy.set(false);
    }
  }

  async request() {
    this.busy.set(true);
    this.message.set('');
    try {
      const result = await firstValueFrom(this.http.post<{ desafioId?: string }>(
        '/api/portal/acesso/codigo', { oficinaSlug: this.slug.trim(), placa: this.plate.trim() },
        { headers: await this.headers() }));
      this.id = result.desafioId ?? '';
      this.sent.set(true);
      this.message.set('Se houver um contato verificado, o código foi enviado.');
    } catch {
      this.message.set('Não foi possível solicitar o código agora. Tente novamente.');
    } finally {
      this.busy.set(false);
    }
  }

  async confirm() {
    this.busy.set(true);
    this.message.set('');
    try {
      await firstValueFrom(this.http.post('/api/portal/acesso/validacao',
        { desafioId: this.id, codigo: this.code }, { headers: await this.headers() }));
      this.authenticated.set(true);
      const vehicles = await firstValueFrom(this.http.get<PortalVehicle[]>('/api/portal/veiculos'));
      this.vehicles.set(vehicles);
      this.selectedVehicleId = vehicles[0]?.id ?? '';
      await this.loadService(this.selectedVehicleId);
    } catch {
      this.authenticated.set(false);
      this.message.set('Código inválido ou expirado.');
    } finally {
      this.busy.set(false);
    }
  }

  async loadService(vehicleId = '') {
    this.selectedVehicleId = vehicleId || this.selectedVehicleId;
    let params = new HttpParams();
    if (this.selectedVehicleId) params = params.set('veiculoId', this.selectedVehicleId);
    const result = await firstValueFrom(this.http.get<{ servico: PortalService | null }>(
      '/api/portal/servico-atual', { params }));
    this.service.set(result.servico);
    this.photos.set(result.servico ? await firstValueFrom(this.http.get<PortalPhoto[]>(
      `/api/portal/ordens-servico/${result.servico.id}/fotos`)) : []);
  }

  restart() {
    this.sent.set(false);
    this.code = '';
    this.id = '';
    this.message.set('');
  }

  photoUrl(id: string) {
    return `/api/portal/ordens-servico/${this.service()?.id}/fotos/${id}/conteudo`;
  }

  statusLabel(status: string) {
    return status.replaceAll('_', ' ').toLocaleLowerCase('pt-BR')
      .replace(/^./, value => value.toLocaleUpperCase('pt-BR'));
  }

  private async headers() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
