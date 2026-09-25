import { DatePipe, Location } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, NavigationEnd, NavigationSkipped, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ReviewService, ReviewSummary, reviewError } from './review.service';

@Component({
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './review-page.component.html',
  styleUrl: './review.css'
})
export class ReviewPageComponent implements OnInit, OnDestroy {
  private readonly api = inject(ReviewService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private navigation?: Subscription;
  private revision = 0;
  private queue = Promise.resolve();
  readonly summary = signal<ReviewSummary | null>(null);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly scores = [1, 2, 3, 4, 5];
  score = 0; comment = ''; consent = false;

  ngOnInit() {
    this.navigation = this.router.events.subscribe(event => {
      if (!(event instanceof NavigationEnd) && !(event instanceof NavigationSkipped)) return;
      const token = new URLSearchParams(this.location.path(true).split('#')[1] || '').get('token');
      if (token !== null) void this.load(token);
    });
    return this.load(new URLSearchParams(this.route.snapshot.fragment || '').get('token'));
  }
  ngOnDestroy() { this.navigation?.unsubscribe(); this.revision++; }
  load(token: string | null = null) {
    const revision = ++this.revision;
    this.location.replaceState('/avaliar');
    this.summary.set(null); this.message.set(''); this.busy.set(true);
    this.score = 0; this.comment = ''; this.consent = false;
    // Serialize grants and submissions: a new link must never submit against the previous OS.
    this.queue = this.queue.then(async () => {
      if (revision !== this.revision) return;
      try {
        if (token !== null) await this.api.exchange(token);
        if (revision !== this.revision) return;
        const result = await this.api.summary();
        if (revision === this.revision) this.summary.set(result);
      } catch (error) {
        if (revision === this.revision) this.message.set(reviewError(error, 'Não foi possível abrir o resumo. Use o convite recebido da oficina.'));
      } finally { if (revision === this.revision) this.busy.set(false); }
    });
    return this.queue;
  }
  submit() {
    if (this.busy() || !this.summary() || this.summary()!.avaliacao) return Promise.resolve();
    if (!Number.isInteger(this.score) || this.score < 1 || this.score > 5) {
      this.message.set('Selecione uma nota de 1 a 5.'); return Promise.resolve();
    }
    const revision = this.revision;
    const input = { contexto: this.summary()!.contexto, nota: this.score, comentario: this.comment.trim() || null, consentimentoPublicacao: this.consent };
    this.busy.set(true); this.message.set('');
    this.queue = this.queue.then(async () => {
      if (revision !== this.revision) return;
      try {
        const result = await this.api.submit(input);
        if (revision === this.revision) this.summary.update(current => current ? { ...current, avaliacao: result } : null);
      } catch (error) {
        if (revision !== this.revision) return;
        if (error instanceof HttpErrorResponse && (error.status === 401 || error.error?.code === 'AVALIACAO_CONTEXTO_ALTERADO')) this.summary.set(null);
        this.message.set(reviewError(error, 'Não foi possível enviar. Tente novamente; sua resposta não será duplicada.'));
      } finally { if (revision === this.revision) this.busy.set(false); }
    });
    return this.queue;
  }
}
