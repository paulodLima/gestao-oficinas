import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <main class="portal-shell">
      <header><span class="mark">OF</span><strong>Acompanhamento do serviço</strong></header>
      @if (!service()) {
        <section class="access-card">
          <p class="eyebrow">ÁREA DO CLIENTE</p>
          <h1>{{ sent() ? 'Confirme seu acesso' : 'Acompanhe seu veículo' }}</h1>
          <p class="intro">{{ sent() ? 'Informe o código enviado ao contato cadastrado.' : 'Use a oficina e a placa para solicitar um código de acesso seguro.' }}</p>
          @if (!sent()) {
            <label for="portal-shop">Identificador da oficina</label><input id="portal-shop" [(ngModel)]="slug" placeholder="ex.: oficina-central" autocomplete="organization">
            <label for="portal-plate">Placa do veículo</label><input id="portal-plate" [(ngModel)]="plate" placeholder="ABC1D23" autocapitalize="characters" autocomplete="off">
            <button (click)="request()">Receber código</button>
          } @else {
            <label for="portal-code">Código de 6 dígitos</label><input id="portal-code" [(ngModel)]="code" inputmode="numeric" maxlength="6" autocomplete="one-time-code">
            <button (click)="confirm()">Entrar no acompanhamento</button>
          }
          @if (message()) { <p class="message" role="status">{{ message() }}</p> }
        </section>
      } @else {
        <section class="service-head">
          <p class="eyebrow">SERVIÇO EM ANDAMENTO</p><h1>{{ service().veiculo }}</h1><p class="plate">{{ service().placa }}</p>
          <div class="status-row"><span class="status">{{ service().status }}</span><span>Previsão: {{ service().previsaoEm || 'a confirmar' }}</span></div>
        </section>
        <section class="progress" aria-label="Andamento do serviço"><div class="progress-line"></div><article><i></i><strong>Recebido</strong><small>Veículo na oficina</small></article><article><i></i><strong>{{ service().status }}</strong><small>Etapa atual</small></article><article class="muted"><i></i><strong>Pronto</strong><small>Retirada prevista</small></article></section>
        <section class="gallery"><div class="section-title"><div><p class="eyebrow">REGISTROS DA OFICINA</p><h2>Fotos do serviço</h2></div><span>{{ photos().length }} foto(s)</span></div>
          @if (photos().length) { <div class="photo-grid">@for (photo of photos(); track photo.id) { <figure><img [src]="photoUrl(photo.id)" [alt]="photo.legenda || 'Registro do serviço'"><figcaption>{{ photo.etapa }}</figcaption></figure> }</div> }
          @else { <p class="empty">A oficina ainda não publicou fotos deste serviço.</p> }
        </section>
      }
    </main>`,
  styles: `:host{display:block;min-height:100dvh;background:var(--app-bg);color:var(--ink)}.portal-shell{max-width:940px;margin:auto;padding:28px 20px 56px}.portal-shell>header{display:flex;align-items:center;gap:10px;margin-bottom:38px;font-size:14px}.mark{display:grid;width:32px;height:32px;place-items:center;border-radius:8px;background:var(--brand);color:#fff;font-size:11px;font-weight:800}.access-card{max-width:460px;margin:8vh auto;padding:28px;border:1px solid var(--line);border-radius:12px;background:var(--surface)}.eyebrow{margin:0 0 8px;color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.1em}h1{margin:0;color:var(--ink);font-size:clamp(27px,5vw,40px);letter-spacing:-.04em;line-height:1.15}.intro{margin:10px 0 22px;color:var(--muted);line-height:1.6}label{display:block;margin:16px 0 6px;color:#4f5b6d;font-size:12px;font-weight:750}input{display:block;width:100%;min-height:46px;padding:10px 12px;border:1px solid var(--line);border-radius:7px;background:#fff;color:var(--ink)}button{width:100%;min-height:46px;margin-top:20px;border:1px solid var(--brand);border-radius:7px;background:var(--brand);color:#fff;font-weight:750;cursor:pointer}.message{margin:16px 0 0;padding:10px;border-radius:7px;background:#eef4ff;color:var(--brand);font-size:13px}.service-head{padding-bottom:24px;border-bottom:1px solid var(--line)}.plate{display:inline-block;margin:10px 0 0;padding:5px 8px;border:1px solid #aebbd0;border-radius:5px;color:#314866;font:750 13px ui-monospace,SFMono-Regular,Menlo,monospace}.status-row{display:flex;gap:12px;align-items:center;margin-top:18px;color:var(--muted);font-size:13px}.status{padding:5px 8px;border-radius:99px;background:var(--brand-soft);color:var(--brand);font-size:11px;font-weight:800}.progress{position:relative;display:grid;grid-template-columns:repeat(3,1fr);gap:10px;padding:30px 0}.progress-line{position:absolute;top:36px;right:12%;left:12%;height:2px;background:#c7d3e7}.progress article{position:relative;z-index:1;display:grid;justify-items:center;gap:4px;text-align:center}.progress i{width:14px;height:14px;border:3px solid var(--brand-soft);border-radius:50%;background:var(--brand)}.progress small{color:var(--muted);font-size:12px}.progress .muted i{border-color:#e2e6ed;background:#fff}.progress .muted strong,.progress .muted small{color:var(--muted)}.gallery{padding:22px;border:1px solid var(--line);border-radius:12px;background:var(--surface)}.section-title{display:flex;justify-content:space-between;gap:12px;align-items:start}.section-title h2{margin:0;font-size:19px}.section-title span{color:var(--muted);font-size:12px}.photo-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;margin-top:18px}.photo-grid figure{margin:0;overflow:hidden;border-radius:8px;background:#f4f7fc}.photo-grid img{display:block;width:100%;aspect-ratio:1;object-fit:cover}.photo-grid figcaption{padding:7px;color:var(--muted);font-size:11px}.empty{margin:18px 0 0;color:var(--muted);font-size:13px}@media(max-width:580px){.portal-shell{padding:20px 16px 40px}.access-card{margin:4vh auto;padding:20px}.progress{gap:4px}.progress strong{font-size:12px}.progress small{font-size:10px}.photo-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.gallery{padding:16px}}`
})
export class PortalAccessComponent {
  private readonly http = inject(HttpClient);
  slug = ''; plate = ''; code = ''; id = '';
  readonly sent = signal(false); readonly message = signal(''); readonly service = signal<any>(null); readonly photos = signal<any[]>([]);
  async request() { const result = await firstValueFrom(this.http.post<{ desafioId?: string }>('/api/portal/acesso/codigo', { oficinaSlug: this.slug, placa: this.plate })); this.id = result.desafioId ?? ''; this.sent.set(true); this.message.set('Se houver um contato verificado, o código foi enviado.'); }
  async confirm() { try { await firstValueFrom(this.http.post('/api/portal/acesso/validacao', { desafioId: this.id, codigo: this.code })); const result = await firstValueFrom(this.http.get<{ servico: any }>('/api/portal/servico-atual')); this.service.set(result.servico); if (result.servico) this.photos.set(await firstValueFrom(this.http.get<any[]>(`/api/portal/ordens-servico/${result.servico.id}/fotos`))); this.message.set(result.servico ? '' : 'Nenhum serviço em andamento.'); } catch { this.message.set('Código inválido ou expirado.'); } }
  photoUrl(id: string) { return `/api/portal/ordens-servico/${this.service().id}/fotos/${id}/conteudo`; }
}
