import { DatePipe } from '@angular/common';
import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { ReviewService, reviewError } from './review.service';

@Component({ selector: 'app-review-invitation', imports: [DatePipe], styleUrl: './review.css',
  template: `<section class="panel"><p class="eyebrow">PÓS-ENTREGA</p><h2>Convite para avaliação</h2><p>Acesso separado do acompanhamento, válido por sete dias após a entrega. Compartilhe somente com o cliente responsável.</p>
    <div class="actions"><button type="button" [disabled]="busy()" (click)="create()">Obter link de avaliação</button><button class="danger" type="button" [disabled]="busy()" (click)="confirming.set(true)">Revogar convite</button></div>
    @if (confirming()) { <div class="notice"><p>Revogar impede novos acessos e envios, inclusive em sessões abertas. O convite não poderá ser reemitido para esta OS. A avaliação já recebida será preservada.</p><div class="actions"><button type="button" [disabled]="busy()" (click)="revoke()">Confirmar revogação</button><button type="button" class="secondary" [disabled]="busy()" (click)="confirming.set(false)">Voltar</button></div></div> }
    @if (url()) { <div class="link-box"><label class="field" for="review-link">Link restrito de avaliação</label><input id="review-link" type="text" readonly [value]="url()" (focus)="$any($event.target).select()"><p>Expira em {{ expires() | date:'dd/MM/yyyy HH:mm':'-0300' }}. Copiar não envia uma mensagem.</p><button class="secondary" type="button" (click)="copy()">Copiar link de avaliação</button></div> }
    @if (message()) { <p class="notice" role="status">{{ message() }}</p> }
  </section>`
})
export class ReviewInvitationComponent implements OnChanges {
  @Input({ required: true }) orderId!: string;
  private readonly api = inject(ReviewService);
  private revision = 0;
  readonly url = signal(''); readonly expires = signal(''); readonly busy = signal(false);
  readonly message = signal(''); readonly confirming = signal(false);
  ngOnChanges() { this.revision++; this.url.set(''); this.expires.set(''); this.message.set(''); this.busy.set(false); this.confirming.set(false); }
  async create() {
    if (this.busy()) return;
    const revision = this.revision; this.busy.set(true); this.message.set(''); this.url.set('');
    try {
      const result = await this.api.invitation(this.orderId);
      if (revision !== this.revision) return;
      this.url.set(new URL('/avaliar', window.location.origin).href + '#token=' + encodeURIComponent(result.token)); this.expires.set(result.expiraEm);
    } catch (error) { if (revision === this.revision) this.message.set(reviewError(error, 'Não foi possível obter o convite.')); }
    finally { if (revision === this.revision) this.busy.set(false); }
  }
  async revoke() {
    if (this.busy() || !this.confirming()) return;
    const revision = this.revision; this.busy.set(true);
    try { await this.api.revoke(this.orderId); if (revision === this.revision) { this.url.set(''); this.confirming.set(false); this.message.set('Convite revogado.'); } }
    catch (error) { if (revision === this.revision) this.message.set(reviewError(error, 'Não foi possível revogar o convite.')); }
    finally { if (revision === this.revision) this.busy.set(false); }
  }
  async copy() {
    const revision = this.revision;
    try { await navigator.clipboard.writeText(this.url()); if (revision === this.revision) this.message.set('Link copiado. Compartilhe somente com o responsável.'); }
    catch { if (revision === this.revision) this.message.set('Selecione e copie o link no campo acima.'); }
  }
}
