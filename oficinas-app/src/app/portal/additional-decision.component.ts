import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AdditionalBlock, AdditionalDecision, AdditionalDecisionService,
  AdditionalRequest, AdditionalVersion
} from './additional-decision.service';

@Component({
  selector: 'app-additional-decision',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (loading()) {
      <section class="additional-card subtle" aria-live="polite">Carregando solicitações adicionais…</section>
    } @else if (requests().length) {
      <section class="additional-card" aria-labelledby="additional-title">
        <header><div><p class="eyebrow">APROVAÇÃO DO CLIENTE</p><h2 id="additional-title">Serviços adicionais</h2></div>
          <span>{{ pendingCount() }} pendente(s)</span></header>
        <p class="lead">Revise valores, impacto no prazo e evidências antes de confirmar. Grupos dependentes são decididos juntos.</p>

        @for (request of requests(); track request.id) {
          @if (currentVersion(request); as version) {
            <article class="request">
              <div class="request-head"><div><strong>Solicitação #{{ version.numero }}</strong><small>{{ version.problema }}</small></div>
                <b>{{ money(version.total) }}</b></div>
              @if (version.totalAprovado > 0) { <p class="approved-total">Total aprovado: <b>{{ money(version.totalAprovado) }}</b></p> }
              @if (version.estado === 'SUBSTITUIDA') { <p class="replaced">Versão substituída — mantida somente para histórico.</p> }
              <div class="context"><p><span>Justificativa</span>{{ version.justificativa }}</p>
                <p><span>Impacto no prazo</span>{{ version.impactoPrazo }}</p>
                <p><span>Nova previsão</span>{{ date(version.previsaoProposta) }}</p></div>

              @for (block of version.blocos; track block.id) {
                <div class="block" [class.decided]="block.decisao">
                  <div class="block-title"><div><strong>{{ block.grupoDependencia ? 'Grupo ' + block.grupoDependencia : 'Item independente' }}</strong>
                    @if (block.grupoDependencia) { <small>Itens inseparáveis</small> }</div><b>{{ money(block.total) }}</b></div>
                  <ul>@for (item of block.itens; track item.id) {
                    <li><span>{{ item.descricao }}<small>{{ item.quantidade }} × {{ money(item.valorUnitario) }}</small></span><b>{{ money(item.total) }}</b></li>
                  }</ul>
                  @if (block.decisao) {
                    <p class="decision-result" [class.rejected]="block.decisao === 'RECUSADO'">
                      {{ block.decisao === 'APROVADO' ? 'Aprovado' : 'Recusado' }} em {{ date(block.decididaEm) }}
                    </p>
                  } @else if (version.estado === 'ENVIADA' && (request.estado === 'ENVIADA' || request.estado === 'PARCIALMENTE_DECIDIDA')) {
                    <div class="choices" [attr.aria-label]="'Decisão para ' + block.id">
                      <button type="button" [class.selected]="choice(request.id, block.id) === 'APROVADO'"
                        (click)="choose(request.id, block.id, 'APROVADO')">Aprovar</button>
                      <button type="button" class="reject" [class.selected]="choice(request.id, block.id) === 'RECUSADO'"
                        (click)="choose(request.id, block.id, 'RECUSADO')">Recusar</button>
                    </div>
                  }
                </div>
              }

              @if (version.estado === 'ENVIADA' && hasChoices(request.id)) {
                @if (activeRequest() !== request.id) {
                  <button type="button" class="primary" [disabled]="busy()" (click)="requestCode(request)">Confirmar decisões selecionadas</button>
                } @else {
                  <div class="confirmation">
                    <p>Enviamos um código ao e-mail verificado do responsável.</p>
                    <label [for]="'decision-code-' + request.id">Código de 6 dígitos</label>
                    <input [id]="'decision-code-' + request.id" [(ngModel)]="code" inputmode="numeric" maxlength="6" autocomplete="one-time-code">
                    <label [for]="'decision-comment-' + request.id">Comentário (opcional)</label>
                    <textarea [id]="'decision-comment-' + request.id" [(ngModel)]="comment" maxlength="1000" rows="3"></textarea>
                    <button type="button" class="primary" [disabled]="busy() || code.length !== 6" (click)="confirm(request)">
                      {{ busy() ? 'Registrando…' : 'Registrar decisão definitiva' }}
                    </button>
                  </div>
                }
              }
            </article>
          }
        }
        @if (message()) { <p class="message" role="status">{{ message() }}</p> }
      </section>
    }
  `,
  styles: `
    :host{display:block}.additional-card{margin-top:22px;padding:24px;border:1px solid var(--line);border-radius:14px;background:var(--surface)}.additional-card.subtle{color:var(--muted);font-size:13px}.additional-card>header{display:flex;justify-content:space-between;gap:12px}.additional-card>header span{color:var(--muted);font-size:12px}.eyebrow{margin:0 0 8px;color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.11em}h2{margin:0;font-size:21px;letter-spacing:-.025em}.lead{max-width:700px;margin:10px 0 20px;color:var(--muted);font-size:13px;line-height:1.55}.request{padding:18px;border:1px solid var(--line);border-radius:12px;background:#fbfcfe}.request+.request{margin-top:14px}.request-head,.block-title{display:flex;justify-content:space-between;gap:15px}.request-head div,.block-title div{display:grid;gap:4px}.request-head small,.block-title small{color:var(--muted);font-size:12px}.approved-total,.replaced{margin:10px 0 0;color:#276447;font-size:12px}.replaced{padding:8px;border-radius:7px;background:#fff3dc;color:#725718}.context{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin:14px 0}.context p{display:grid;gap:4px;margin:0;padding:10px;border-radius:8px;background:#f1f5fa;font-size:12px;line-height:1.45}.context span{text-transform:uppercase;color:var(--muted);font-size:9px;font-weight:800;letter-spacing:.06em}.block{margin-top:10px;padding:14px;border:1px solid var(--line);border-radius:9px;background:#fff}.block.decided{background:#f7faf8}.block ul{display:grid;gap:8px;margin:12px 0;padding:0;list-style:none}.block li{display:flex;justify-content:space-between;gap:12px;color:#465367;font-size:13px}.block li span{display:grid;gap:2px}.block li small{color:var(--muted);font-size:11px}.choices{display:grid;grid-template-columns:1fr 1fr;gap:8px}.choices button,.primary{min-height:44px;border:1px solid #2e7453;border-radius:8px;background:#fff;color:#276447;font-weight:750;cursor:pointer}.choices button.selected{background:#e1f2e9;box-shadow:inset 0 0 0 1px #2e7453}.choices .reject{border-color:#a64a4a;color:#923d3d}.choices .reject.selected{background:#faeaea;box-shadow:inset 0 0 0 1px #a64a4a}.primary{width:100%;margin-top:14px;border-color:var(--brand);background:var(--brand);color:#fff}.primary:disabled{cursor:not-allowed;opacity:.55}.decision-result{margin:10px 0 0;padding:8px;border-radius:7px;background:#e1f2e9;color:#276447;font-size:12px;font-weight:750}.decision-result.rejected{background:#faeaea;color:#923d3d}.confirmation{margin-top:14px;padding:14px;border-radius:9px;background:#eef4ff}.confirmation p{margin:0 0 10px;color:#3d526f;font-size:12px}.confirmation label{display:block;margin:10px 0 5px;color:#4f5b6d;font-size:11px;font-weight:750}.confirmation input,.confirmation textarea{box-sizing:border-box;width:100%;padding:10px 12px;border:1px solid var(--line);border-radius:8px;background:#fff;color:var(--ink)}.message{margin:14px 0 0;padding:10px;border-radius:8px;background:#eef4ff;color:var(--brand);font-size:12px}@media(max-width:680px){.additional-card{padding:18px}.context{grid-template-columns:1fr}.request{padding:14px}.choices{grid-template-columns:1fr}}
  `
})
export class AdditionalDecisionComponent implements OnChanges {
  @Input({ required: true }) orderId = '';
  private readonly service = inject(AdditionalDecisionService);
  readonly requests = signal<AdditionalRequest[]>([]);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly activeRequest = signal('');
  private readonly choices = signal<Record<string, AdditionalDecision>>({});
  private challengeId = '';
  private idempotencyKey = '';
  code = '';
  comment = '';

  ngOnChanges() { if (this.orderId) void this.load(); }

  async load() {
    this.loading.set(true);
    try { this.requests.set(await this.service.list(this.orderId)); }
    catch { this.message.set('Não foi possível carregar os serviços adicionais.'); }
    finally { this.loading.set(false); }
  }

  currentVersion(request: AdditionalRequest): AdditionalVersion | undefined {
    return request.versoes.find(version => version.estado === 'ENVIADA') ?? request.versoes[0];
  }

  pendingCount() {
    return this.requests().flatMap(request => this.currentVersion(request)?.blocos ?? [])
      .filter(block => !block.decisao).length;
  }

  choice(requestId: string, blockId: string) { return this.choices()[`${requestId}:${blockId}`]; }
  choose(requestId: string, blockId: string, decision: AdditionalDecision) {
    this.choices.update(values => ({ ...values, [`${requestId}:${blockId}`]: decision }));
    this.message.set('');
  }
  hasChoices(requestId: string) { return Object.keys(this.choices()).some(key => key.startsWith(`${requestId}:`)); }

  async requestCode(request: AdditionalRequest) {
    this.busy.set(true); this.message.set('');
    try {
      const result = await this.service.requestCode(this.orderId, request.id);
      this.challengeId = result.desafioId;
      this.idempotencyKey = globalThis.crypto?.randomUUID?.()
        ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      this.activeRequest.set(request.id); this.code = ''; this.comment = '';
    } catch (error) { this.message.set(this.errorMessage(error)); }
    finally { this.busy.set(false); }
  }

  async confirm(request: AdditionalRequest) {
    this.busy.set(true); this.message.set('');
    const prefix = `${request.id}:`;
    const decisions = Object.entries(this.choices()).filter(([key]) => key.startsWith(prefix))
      .map(([key, decisao]) => ({ bloco: key.slice(prefix.length), decisao }));
    try {
      await this.service.confirm(this.orderId, request, this.challengeId, this.code, decisions,
        this.comment, this.idempotencyKey);
      this.choices.update(values => Object.fromEntries(Object.entries(values).filter(([key]) => !key.startsWith(prefix))));
      this.activeRequest.set(''); this.code = ''; this.comment = '';
      this.message.set('Decisão registrada. A oficina já pode consultar o resultado.');
      await this.load();
    } catch (error) { this.message.set(this.errorMessage(error)); }
    finally { this.busy.set(false); }
  }

  money(value: number) { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value); }
  date(value: string | null) {
    return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short',
      timeZone: 'America/Sao_Paulo' }).format(new Date(value)) : 'Sem alteração informada';
  }
  private errorMessage(error: unknown) {
    if (error instanceof HttpErrorResponse && error.status === 409) return error.error?.detail ?? 'A solicitação mudou. Atualize e tente novamente.';
    if (error instanceof HttpErrorResponse && error.status === 400) return 'Código inválido, expirado ou decisão inconsistente.';
    return 'Não foi possível concluir agora. Tente novamente.';
  }
}
