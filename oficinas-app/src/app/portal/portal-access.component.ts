import { Component, HostListener, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, NavigationEnd, NavigationSkipped, Router } from '@angular/router';
import { Subscription, firstValueFrom } from 'rxjs';
import { AdditionalDecisionComponent } from './additional-decision.component';
import { Location } from '@angular/common';

interface PortalVehicle { id: string; placa: string; veiculo: string; }
interface PortalOffice { nome: string; telefone: string; email: string; }
interface PortalService {
  id: string; numero: number; status: string; previsaoEm: string | null;
  placa: string; veiculo: string; pendencia: string | null;
  motivoPrevisao: string | null; proximaAcao: string | null; ultimaAtualizacao: string;
}
interface PortalPhoto { id: string; etapa: string; legenda: string | null; createdAt: string; }
interface PortalUpdate {
  id: string; tipo: string; statusAnterior: string | null; statusNovo: string | null;
  texto: string; createdAt: string;
}

@Component({
  standalone: true,
  imports: [FormsModule, AdditionalDecisionComponent],
  template: `
    <main class="portal-shell">
      <header class="portal-brand">
        <span class="mark" aria-hidden="true">OF</span>
        <div><strong>{{ office()?.nome || 'Gestão Oficinas' }}</strong><small>Acompanhamento do serviço</small></div>
      </header>

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
        @if (message()) { <p class="portal-message" role="alert">{{ message() }}</p> }

        @if (loadingPortal()) {
          <section class="loading" aria-live="polite"><span></span>Carregando acompanhamento…</section>
        } @else {
          @if (service(); as current) {
          <section class="service-hero">
            <div class="service-title">
              <div><p class="eyebrow">SERVIÇO EM ANDAMENTO · OS {{ current.numero }}</p><h1>{{ current.veiculo }}</h1><p class="plate">{{ current.placa }}</p></div>
              <span class="status">{{ statusLabel(current.status) }}</span>
            </div>
            <dl class="service-facts">
              <div><dt>Previsão estimada</dt><dd>{{ formatDate(current.previsaoEm) }}</dd></div>
              <div><dt>Pendência</dt><dd>{{ current.pendencia || 'Nenhuma pendência informada' }}</dd></div>
              <div><dt>Última atualização</dt><dd>{{ formatDate(current.ultimaAtualizacao) }}</dd></div>
            </dl>
            @if (current.motivoPrevisao || current.proximaAcao) {
              <div class="forecast-note">
                @if (current.motivoPrevisao) { <p><strong>Motivo:</strong> {{ current.motivoPrevisao }}</p> }
                @if (current.proximaAcao) { <p><strong>Próxima ação:</strong> {{ current.proximaAcao }}</p> }
              </div>
            }
          </section>

          <app-additional-decision [orderId]="current.id" />

          <section class="gallery" aria-labelledby="gallery-title">
            <div class="section-title"><div><p class="eyebrow">REGISTROS DA OFICINA</p><h2 id="gallery-title">Galeria do serviço</h2></div><span>{{ filteredPhotos().length }} de {{ photos().length }}</span></div>
            @if (stages().length > 1) {
              <div class="filters" aria-label="Filtrar fotos por etapa">
                <button type="button" [class.active]="selectedStage() === 'TODAS'" (click)="setStage('TODAS')">Todas</button>
                @for (stage of stages(); track stage) {
                  <button type="button" [class.active]="selectedStage() === stage" (click)="setStage(stage)">{{ statusLabel(stage) }}</button>
                }
              </div>
            }
            @if (filteredPhotos().length) {
              <div class="photo-grid">
                @for (photo of filteredPhotos(); track photo.id) {
                  <button type="button" class="photo-card" (click)="openPhoto(photo.id)" [attr.aria-label]="'Ampliar ' + (photo.legenda || 'foto do serviço')">
                    <img [src]="photoUrl(photo.id)" [alt]="photo.legenda || 'Registro do serviço'" loading="lazy">
                    <span><b>{{ statusLabel(photo.etapa) }}</b><small>{{ photo.legenda || 'Sem legenda' }} · {{ formatDate(photo.createdAt) }}</small></span>
                  </button>
                }
              </div>
            } @else { <p class="empty">A oficina ainda não publicou fotos para este filtro.</p> }
          </section>

          <section class="timeline" aria-labelledby="timeline-title">
            <div class="section-title"><div><p class="eyebrow">HISTÓRICO PÚBLICO</p><h2 id="timeline-title">Linha do tempo</h2></div><span>{{ updates().length }} atualização(ões)</span></div>
            @if (updates().length) {
              <ol>
                @for (update of updates(); track update.id) {
                  <li><i aria-hidden="true"></i><article><time [attr.datetime]="update.createdAt">{{ formatDate(update.createdAt) }}</time><strong>{{ update.statusNovo ? statusLabel(update.statusNovo) : 'Atualização da oficina' }}</strong><p>{{ update.texto }}</p></article></li>
                }
              </ol>
            } @else { <p class="empty">Ainda não há atualizações públicas neste serviço.</p> }
          </section>
        } @else {
          <section class="access-card empty-state">
            <p class="eyebrow">ACOMPANHAMENTO</p><h1>Nenhum serviço em andamento</h1>
            <p class="intro">Quando a oficina abrir um serviço autorizado para este veículo, ele aparecerá aqui.</p>
            @if (office(); as currentOffice) {
              <div class="contact"><strong>Fale com {{ currentOffice.nome }}</strong>
                @if (currentOffice.telefone) { <a [href]="'tel:' + currentOffice.telefone">{{ currentOffice.telefone }}</a> }
                @if (currentOffice.email) { <a [href]="'mailto:' + currentOffice.email">{{ currentOffice.email }}</a> }
              </div>
            }
          </section>
          }
        }
      }
    </main>

    @if (activePhoto(); as photo) {
      <div class="lightbox" role="dialog" aria-modal="true" aria-label="Foto ampliada do serviço" (click)="closePhoto()" (touchstart)="onTouchStart($event)" (touchend)="onTouchEnd($event)">
        <button type="button" class="close" aria-label="Fechar foto ampliada" (click)="closePhoto(); $event.stopPropagation()">×</button>
        <button type="button" class="nav previous" aria-label="Foto anterior" (click)="previousPhoto(); $event.stopPropagation()">‹</button>
        <figure (click)="$event.stopPropagation()"><img [src]="photoUrl(photo.id, true)" [alt]="photo.legenda || 'Registro ampliado do serviço'"><figcaption><b>{{ statusLabel(photo.etapa) }}</b><span>{{ photo.legenda || 'Sem legenda' }}</span><small>{{ formatDate(photo.createdAt) }}</small></figcaption></figure>
        <button type="button" class="nav next" aria-label="Próxima foto" (click)="nextPhoto(); $event.stopPropagation()">›</button>
      </div>
    }
  `,
  styles: `
    :host .access-card .button-link { min-height:44px; }
    .portal-message{margin:0 0 20px;padding:11px;border-radius:8px;background:#eef4ff;color:var(--brand);font-size:13px}
    :host{display:block;min-height:100dvh;background:var(--app-bg);color:var(--ink)}*{box-sizing:border-box}.portal-shell{max-width:1040px;margin:auto;padding:28px 20px 64px}.portal-brand{display:flex;align-items:center;gap:11px;margin-bottom:38px}.portal-brand div{display:grid;gap:2px}.portal-brand strong{font-size:14px}.portal-brand small{color:var(--muted);font-size:11px}.mark{display:grid;width:34px;height:34px;place-items:center;border-radius:9px;background:var(--brand);color:#fff;font-size:11px;font-weight:800}.access-card{max-width:480px;margin:8vh auto;padding:30px;border:1px solid var(--line);border-radius:14px;background:var(--surface);box-shadow:0 18px 60px rgba(37,55,82,.08)}.eyebrow{margin:0 0 8px;color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.11em}h1{margin:0;color:var(--ink);font-size:clamp(28px,5vw,44px);letter-spacing:-.045em;line-height:1.08}h2{margin:0;font-size:21px;letter-spacing:-.025em}.intro{margin:12px 0 22px;color:var(--muted);line-height:1.65}label{display:block;margin:16px 0 6px;color:#4f5b6d;font-size:12px;font-weight:750}input,select{display:block;width:100%;min-height:46px;padding:10px 12px;border:1px solid var(--line);border-radius:8px;background:#fff;color:var(--ink)}button,a{touch-action:manipulation}input:focus-visible,select:focus-visible,button:focus-visible,a:focus-visible{outline:3px solid color-mix(in srgb,var(--brand) 28%,transparent);outline-offset:2px}.access-card button{width:100%;min-height:46px;margin-top:20px;border:1px solid var(--brand);border-radius:8px;background:var(--brand);color:#fff;font-weight:750;cursor:pointer}.access-card button:disabled{cursor:not-allowed;opacity:.55}.access-card .button-link{min-height:40px;margin-top:8px;border-color:transparent;background:transparent;color:var(--brand)}.message{margin:16px 0 0;padding:11px;border-radius:8px;background:#eef4ff;color:var(--brand);font-size:13px}.vehicle-picker{max-width:420px;margin:0 0 24px}.vehicle-picker label{margin-top:0}.loading{display:flex;justify-content:center;align-items:center;gap:10px;min-height:220px;color:var(--muted)}.loading span{width:18px;height:18px;border:2px solid var(--line);border-top-color:var(--brand);border-radius:50%;animation:spin .8s linear infinite}.service-hero{overflow:hidden;border:1px solid var(--line);border-radius:16px;background:linear-gradient(145deg,#fff 55%,var(--brand-soft));box-shadow:0 14px 45px rgba(35,58,91,.07)}.service-title{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;padding:28px}.plate{display:inline-block;margin:11px 0 0;padding:5px 9px;border:1px solid #aebbd0;border-radius:5px;color:#314866;font:750 13px ui-monospace,SFMono-Regular,Menlo,monospace}.status{flex:none;padding:7px 10px;border-radius:99px;background:var(--brand);color:#fff;font-size:11px;font-weight:800}.service-facts{display:grid;grid-template-columns:repeat(3,1fr);margin:0;border-top:1px solid var(--line);background:rgba(255,255,255,.7)}.service-facts div{min-width:0;padding:18px 22px;border-right:1px solid var(--line)}.service-facts div:last-child{border:0}.service-facts dt{margin-bottom:6px;color:var(--muted);font-size:11px;font-weight:750;text-transform:uppercase;letter-spacing:.06em}.service-facts dd{margin:0;font-size:13px;font-weight:700;line-height:1.45}.forecast-note{display:grid;gap:5px;padding:16px 22px;border-top:1px solid var(--line);background:#fff8e8;color:#6b5624;font-size:13px}.forecast-note p{margin:0}.gallery,.timeline{margin-top:22px;padding:24px;border:1px solid var(--line);border-radius:14px;background:var(--surface)}.section-title{display:flex;justify-content:space-between;gap:12px;align-items:start}.section-title>span{color:var(--muted);font-size:12px}.filters{display:flex;gap:7px;overflow-x:auto;margin:18px -4px 0;padding:4px}.filters button{flex:none;min-height:44px;padding:8px 13px;border:1px solid var(--line);border-radius:99px;background:#fff;color:var(--muted);font-weight:700;cursor:pointer}.filters button.active{border-color:var(--brand);background:var(--brand-soft);color:var(--brand)}.photo-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-top:18px}.photo-card{overflow:hidden;min-height:44px;padding:0;border:1px solid var(--line);border-radius:10px;background:#fff;color:var(--ink);text-align:left;cursor:zoom-in}.photo-card img{display:block;width:100%;aspect-ratio:4/3;object-fit:cover;background:#edf1f7}.photo-card span{display:grid;gap:3px;padding:10px}.photo-card b{font-size:11px}.photo-card small{overflow:hidden;color:var(--muted);font-size:11px;line-height:1.45;text-overflow:ellipsis;white-space:nowrap}.empty{margin:20px 0 0;color:var(--muted);font-size:13px}.timeline ol{margin:22px 0 0;padding:0;list-style:none}.timeline li{position:relative;display:grid;grid-template-columns:18px 1fr;gap:13px;padding-bottom:22px}.timeline li:not(:last-child)::before{position:absolute;top:16px;bottom:0;left:6px;width:2px;background:var(--line);content:''}.timeline i{z-index:1;width:14px;height:14px;margin-top:3px;border:3px solid var(--brand-soft);border-radius:50%;background:var(--brand)}.timeline article{display:grid;gap:4px}.timeline time{color:var(--muted);font-size:11px}.timeline strong{font-size:14px}.timeline p{margin:0;color:#4f5b6d;font-size:13px;line-height:1.55}.empty-state{margin-top:4vh}.contact{display:grid;gap:7px;padding-top:18px;border-top:1px solid var(--line)}.contact a{width:max-content;color:var(--brand);font-size:13px}.lightbox{position:fixed;z-index:1000;inset:0;display:grid;grid-template-columns:56px minmax(0,900px) 56px;place-content:center;align-items:center;gap:12px;padding:70px 20px 24px;background:rgba(9,17,29,.94)}.lightbox figure{margin:0;overflow:hidden;border-radius:12px;background:#0f1928;box-shadow:0 24px 80px #000}.lightbox img{display:block;width:100%;max-height:72dvh;object-fit:contain}.lightbox figcaption{display:grid;grid-template-columns:auto 1fr auto;gap:12px;padding:13px 16px;color:#fff;font-size:13px}.lightbox figcaption span{color:#d7dfeb}.lightbox figcaption small{color:#aebbd0}.lightbox button{display:grid;width:48px;height:48px;place-items:center;border:1px solid rgba(255,255,255,.25);border-radius:50%;background:rgba(255,255,255,.1);color:#fff;font-size:32px;cursor:pointer}.lightbox .close{position:absolute;top:18px;right:20px}.lightbox .next{grid-column:3}.lightbox figure{grid-column:2;grid-row:1}.lightbox .previous{grid-column:1;grid-row:1}@keyframes spin{to{transform:rotate(360deg)}}
    @media(max-width:680px){.portal-shell{padding:20px 14px 44px}.portal-brand{margin-bottom:26px}.access-card{margin:4vh auto;padding:22px}.service-title{display:grid;padding:22px}.service-facts{grid-template-columns:1fr}.service-facts div{padding:14px 18px;border-right:0;border-bottom:1px solid var(--line)}.gallery,.timeline{padding:18px}.photo-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.lightbox{grid-template-columns:48px 1fr 48px;gap:4px;padding:66px 5px 18px}.lightbox button{width:44px;height:44px}.lightbox figcaption{grid-template-columns:1fr}.lightbox figcaption small{grid-row:3}}
    @media(max-width:360px){.photo-grid{grid-template-columns:1fr}.section-title{display:grid}.lightbox{grid-template-columns:44px 1fr 44px}}
    @media(prefers-reduced-motion:reduce){.loading span{animation:none}}
  `
})
export class PortalAccessComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private navigation?: Subscription;
  private contextRevision = 0;
  private serviceRevision = 0;
  private accessQueue = Promise.resolve();
  private touchStartX = 0;
  slug = '';
  plate = '';
  code = '';
  id = '';
  selectedVehicleId = '';
  readonly sent = signal(false);
  readonly authenticated = signal(false);
  readonly busy = signal(false);
  readonly loadingPortal = signal(false);
  readonly message = signal('');
  readonly vehicles = signal<PortalVehicle[]>([]);
  readonly office = signal<PortalOffice | null>(null);
  readonly service = signal<PortalService | null>(null);
  readonly photos = signal<PortalPhoto[]>([]);
  readonly updates = signal<PortalUpdate[]>([]);
  readonly selectedStage = signal('TODAS');
  readonly activePhotoId = signal<string | null>(null);
  readonly orderedPhotos = computed(() => [...this.photos()].sort((a, b) =>
    a.createdAt.localeCompare(b.createdAt) || a.id.localeCompare(b.id)));
  readonly stages = computed(() => [...new Set(this.orderedPhotos().map(photo => photo.etapa))]);
  readonly filteredPhotos = computed(() => this.selectedStage() === 'TODAS'
    ? this.orderedPhotos()
    : this.orderedPhotos().filter(photo => photo.etapa === this.selectedStage()));
  readonly activePhoto = computed(() => this.filteredPhotos().find(photo => photo.id === this.activePhotoId()) ?? null);

  async ngOnInit() {
    this.navigation = this.router.events.subscribe(event => {
      if (!(event instanceof NavigationEnd) && !(event instanceof NavigationSkipped)) return;
      const url = new URL(this.location.path(true), 'https://portal.invalid');
      const token = new URLSearchParams(url.hash.slice(1)).get('token') ?? url.searchParams.get('token');
      if (token !== null) void this.consumeLink(token);
    });
    const fragment = new URLSearchParams(this.route.snapshot.fragment ?? '');
    const token = fragment.get('token') ?? this.route.snapshot.queryParamMap.get('token');
    if (token === null) { await this.restoreSession(); return; }
    await this.consumeLink(token);
  }

  ngOnDestroy() { this.navigation?.unsubscribe(); this.contextRevision++; }

  private consumeLink(token: string) {
    const revision = ++this.contextRevision;
    this.location.replaceState('/acompanhar');
    this.authenticated.set(false);
    this.vehicles.set([]);
    this.office.set(null);
    this.service.set(null);
    this.photos.set([]);
    this.updates.set([]);
    this.closePhoto();
    this.selectedVehicleId = '';
    this.sent.set(false);
    this.code = '';
    this.id = '';
    this.message.set('');
    this.busy.set(true);
    this.loadingPortal.set(false);
    // Serialize exchanges so an older response cannot replace the newer server grant.
    this.accessQueue = this.accessQueue.then(() => this.authenticateLink(token, revision));
    return this.accessQueue;
  }

  private async authenticateLink(token: string, revision: number) {
    if (revision !== this.contextRevision) return;
    try {
      await firstValueFrom(this.http.post('/api/portal/acesso/link', { token }, { headers: await this.headers() }));
      if (revision !== this.contextRevision) return;
      this.authenticated.set(true);
      await this.loadService();
    } catch {
      if (revision === this.contextRevision) this.message.set('Este link é inválido, expirou ou foi revogado.');
    } finally {
      if (revision === this.contextRevision) this.busy.set(false);
    }
  }

  private async restoreSession() {
    const revision = this.contextRevision;
    this.busy.set(true);
    try {
      const vehicles = await firstValueFrom(this.http.get<PortalVehicle[]>('/api/portal/veiculos'));
      if (revision !== this.contextRevision) return;
      this.vehicles.set(vehicles);
      this.authenticated.set(true);
      await this.loadService(vehicles[0]?.id ?? '');
    } catch (error) {
      if (revision !== this.contextRevision) return;
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        this.message.set('Não foi possível recuperar o acompanhamento. Tente novamente.');
      }
    } finally { if (revision === this.contextRevision) this.busy.set(false); }
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
    const context = this.contextRevision;
    const revision = ++this.serviceRevision;
    const current = () => context === this.contextRevision && revision === this.serviceRevision;
    this.loadingPortal.set(true);
    this.message.set('');
    this.service.set(null);
    this.photos.set([]);
    this.updates.set([]);
    this.selectedVehicleId = vehicleId || this.selectedVehicleId;
    this.selectedStage.set('TODAS');
    this.closePhoto();
    try {
      let params = new HttpParams();
      if (this.selectedVehicleId) params = params.set('veiculoId', this.selectedVehicleId);
      const result = await firstValueFrom(this.http.get<{ oficina: PortalOffice; servico: PortalService | null }>(
        '/api/portal/servico-atual', { params }));
      if (!current()) return;
      this.office.set(result.oficina);
      this.service.set(result.servico);
      if (!result.servico) {
        this.photos.set([]);
        this.updates.set([]);
        return;
      }
      const [photos, updates] = await Promise.all([
        firstValueFrom(this.http.get<PortalPhoto[]>(`/api/portal/ordens-servico/${result.servico.id}/fotos`)),
        firstValueFrom(this.http.get<PortalUpdate[]>(`/api/portal/ordens-servico/${result.servico.id}/atualizacoes`))
      ]);
      if (!current()) return;
      this.photos.set(photos);
      this.updates.set([...updates].sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id.localeCompare(b.id)));
    } catch (error) {
      if (!current()) return;
      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.expireAccess();
      } else {
        this.message.set('Não foi possível carregar o acompanhamento agora. Tente novamente.');
      }
    } finally {
      if (current()) this.loadingPortal.set(false);
    }
  }

  setStage(stage: string) { this.selectedStage.set(stage); this.closePhoto(); }
  openPhoto(id: string) { this.activePhotoId.set(id); }
  closePhoto() { this.activePhotoId.set(null); }
  nextPhoto() { this.movePhoto(1); }
  previousPhoto() { this.movePhoto(-1); }

  onTouchStart(event: TouchEvent) { this.touchStartX = event.changedTouches[0]?.clientX ?? 0; }
  onTouchEnd(event: TouchEvent) {
    const distance = (event.changedTouches[0]?.clientX ?? this.touchStartX) - this.touchStartX;
    if (Math.abs(distance) < 40) return;
    distance < 0 ? this.nextPhoto() : this.previousPhoto();
  }

  @HostListener('document:keydown.escape')
  onEscape() { this.closePhoto(); }

  restart() { this.sent.set(false); this.code = ''; this.id = ''; this.message.set(''); }

  photoUrl(id: string, original = false) {
    return `/api/portal/ordens-servico/${this.service()?.id}/fotos/${id}/conteudo${original ? '?tamanho=original' : ''}`;
  }

  statusLabel(status: string) {
    const labels: Record<string, string> = {
      RECEBIDO: 'Recebido', EM_DIAGNOSTICO: 'Em diagnóstico',
      AGUARDANDO_APROVACAO: 'Aguardando aprovação', AGUARDANDO_PECAS: 'Aguardando peças',
      EM_MANUTENCAO: 'Em manutenção', FUNILARIA: 'Funilaria', PINTURA: 'Pintura',
      EM_MONTAGEM: 'Em montagem', EM_TESTES: 'Em testes',
      PRONTO_PARA_RETIRADA: 'Pronto para retirada', ENTREGUE: 'Entregue', CANCELADO: 'Cancelado'
    };
    return labels[status] ?? status.replaceAll('_', ' ');
  }

  formatDate(value: string | null) {
    if (!value) return 'A confirmar';
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo'
    }).format(new Date(value));
  }

  private movePhoto(step: number) {
    const photos = this.filteredPhotos();
    if (!photos.length) return;
    const current = photos.findIndex(photo => photo.id === this.activePhotoId());
    const next = (Math.max(current, 0) + step + photos.length) % photos.length;
    this.activePhotoId.set(photos[next].id);
  }

  private expireAccess() {
    this.authenticated.set(false);
    this.sent.set(false);
    this.service.set(null);
    this.photos.set([]);
    this.updates.set([]);
    this.message.set('Seu acesso expirou. Solicite um novo código para continuar.');
  }

  private async headers() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
