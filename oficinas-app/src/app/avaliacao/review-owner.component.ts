import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ReviewPage, ReviewService, reviewError } from './review.service';

@Component({
  standalone: true, imports: [DatePipe, FormsModule, RouterLink], styleUrl: './review.css',
  template: `<main class="review-shell"><p class="eyebrow">PÓS-ATENDIMENTO</p><h1>Avaliações</h1><p>Ouça seus clientes. Todas as respostas ficam restritas à sua oficina.</p>
    @if (message()) { <p class="notice" role="status">{{ message() }}</p> }
    <section class="panel"><h2>Convite opcional para o Google</h2><p class="muted">O mesmo link aparece para todas as notas, antes e depois da avaliação. Não enviamos a resposta do cliente ao Google.</p>
      @if (configReady()) { <form (ngSubmit)="save()"><label class="field" for="google-url">Link de avaliação do Perfil da Empresa (opcional)</label><input id="google-url" type="url" name="google" [(ngModel)]="googleUrl" maxlength="500" placeholder="https://g.page/r/IDENTIFICADOR/review" aria-describedby="google-help">
      <p id="google-help" class="muted">Aceitamos g.page/r/…/review ou search.google.com/local/writereview?placeid=… . Deixe em branco para desativar.</p><button [disabled]="saving()" type="submit">{{ saving() ? 'Salvando…' : 'Salvar link do Google' }}</button></form> }
      @else { <button type="button" (click)="loadConfiguration()">Carregar configuração</button> }
    </section>
    <section class="panel reviews" aria-labelledby="reviews-title"><h2 id="reviews-title">Respostas recebidas</h2>
      @if (loading()) { <p role="status">Carregando avaliações…</p> }
      @if (result(); as page) { <p class="muted">{{ page.totalElements }} avaliação(ões) · página {{ page.page + 1 }}</p>
        @for (item of page.items; track item.id) { <article><header><a [routerLink]="['/abrir-ordem']" [queryParams]="{ id: item.ordemServicoId }">OS {{ item.numero }}</a><strong>{{ item.nota }} / 5</strong><time>{{ item.createdAt | date:'dd/MM/yyyy HH:mm':'-0300' }}</time></header>
          @if (item.comentario) { <blockquote>{{ item.comentario }}</blockquote> } @else { <p class="muted">Sem comentário.</p> }
          <p class="private">{{ item.consentimentoPublicacao ? 'Consentimento para eventual publicação registrado. Não publicada automaticamente.' : 'Privada · publicação não autorizada.' }}</p></article>
        } @empty { <p>Ainda não há avaliações. O convite fica disponível após a entrega da OS.</p> }
        <div class="actions"><button class="secondary" [disabled]="loading() || page.page === 0" (click)="load(page.page - 1)">Anterior</button><button class="secondary" [disabled]="loading() || page.page + 1 >= page.totalPages" (click)="load(page.page + 1)">Próxima</button></div>
      } @else if (!loading()) { <button type="button" (click)="load()">Tentar novamente</button> }
    </section></main>`
})
export class ReviewOwnerComponent implements OnInit {
  private readonly api = inject(ReviewService);
  readonly result = signal<ReviewPage | null>(null);
  readonly loading = signal(false); readonly saving = signal(false); readonly configReady = signal(false);
  readonly message = signal(''); googleUrl = '';
  ngOnInit() { return Promise.all([this.load(), this.loadConfiguration()]); }
  async load(page = 0) {
    if (this.loading()) return;
    this.loading.set(true);
    try { this.result.set(await this.api.list(page)); }
    catch (error) { this.message.set(reviewError(error, 'Não foi possível carregar as avaliações.')); }
    finally { this.loading.set(false); }
  }
  async loadConfiguration() {
    try { this.googleUrl = (await this.api.configuration()).googleUrl || ''; this.configReady.set(true); }
    catch { this.message.set('Não foi possível carregar a configuração do Google.'); }
  }
  async save() {
    if (this.saving()) return;
    this.saving.set(true); this.message.set('');
    try { this.googleUrl = (await this.api.configure(this.googleUrl)).googleUrl || ''; this.message.set('Link do Google atualizado.'); }
    catch (error) { this.message.set(reviewError(error, 'Não foi possível salvar o link.')); }
    finally { this.saving.set(false); }
  }
}
