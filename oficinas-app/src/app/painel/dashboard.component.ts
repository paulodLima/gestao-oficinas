import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DashboardCardComponent } from './dashboard-card.component';
import { DashboardFilters, DashboardService, DashboardSnapshot, groupCards, Situation, STAGES } from './dashboard.service';

@Component({
  selector: 'app-dashboard', imports: [ReactiveFormsModule, RouterLink, DashboardCardComponent],
  templateUrl: './dashboard.component.html', styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private readonly service = inject(DashboardService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly builder = inject(FormBuilder);
  private requestNumber = 0;
  readonly snapshot = signal<DashboardSnapshot | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly view = signal('lista');
  readonly page = signal(0);
  readonly stages = STAGES;
  readonly columns = computed(() => groupCards(this.snapshot()?.ordens.items ?? []));
  readonly filters = this.builder.nonNullable.group({ q: ['', Validators.maxLength(100)], situacao: ['ATIVAS'],
    status: [''], sort: ['ATUALIZACAO'], semAtualizacaoHoras: ['0'], minHorasEtapa: ['0'] });
  private appliedFilters: DashboardFilters = this.filters.getRawValue();
  readonly metrics: { key: keyof DashboardSnapshot['indicadores']; label: string; situation: Situation }[] = [
    { key: 'ativas', label: 'Ativas', situation: 'ATIVAS' }, { key: 'atrasadas', label: 'Atrasadas', situation: 'ATRASADAS' },
    { key: 'aprovacoes', label: 'Aguardando aprovação', situation: 'APROVACAO' },
    { key: 'pecas', label: 'Aguardando peças', situation: 'PECAS' }, { key: 'prontas', label: 'Prontas para retirada', situation: 'PRONTAS' }
  ];
  ngOnInit() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.filters.setValue({ q: params.get('q') ?? '', situacao: params.get('situacao') ?? 'ATIVAS',
        status: params.get('status') ?? '', sort: params.get('sort') ?? 'ATUALIZACAO',
        semAtualizacaoHoras: params.get('semAtualizacaoHoras') ?? '0', minHorasEtapa: params.get('minHorasEtapa') ?? '0' });
      this.page.set(Number(params.get('page') ?? 0));
      this.appliedFilters = this.filters.getRawValue();
      this.view.set(params.get('view') === 'quadro' ? 'quadro' : params.get('view') === 'lista' ? 'lista' :
        window.matchMedia('(min-width: 900px)').matches ? 'quadro' : 'lista');
      void this.fetch();
    });
    this.destroyRef.onDestroy(() => { this.requestNumber++; });
  }
  async fetch() {
    const request = ++this.requestNumber;
    this.loading.set(true); this.error.set('');
    try {
      const result = await this.service.get(this.appliedFilters, this.page());
      if (request === this.requestNumber) this.snapshot.set(result);
    } catch (error) {
      if (request === this.requestNumber) {
        this.snapshot.set(null);
        this.error.set(error instanceof HttpErrorResponse ? error.error?.detail ?? 'Não foi possível carregar o painel.' : 'Não foi possível carregar o painel.');
      }
    } finally { if (request === this.requestNumber) this.loading.set(false); }
  }
  async apply(page = 0) {
    if (this.filters.invalid) { this.error.set('A busca deve ter até 100 caracteres.'); return; }
    const navigated = await this.router.navigate([], { relativeTo: this.route,
      queryParams: { ...this.filters.getRawValue(), page, view: this.view() } });
    if (!navigated) await this.fetch();
  }
  choose(situation: Situation) { this.filters.controls.situacao.setValue(situation); this.filters.controls.status.setValue(''); void this.apply(); }
  clear() {
    this.filters.setValue({ q: '', situacao: 'ATIVAS', status: '', sort: 'ATUALIZACAO', semAtualizacaoHoras: '0', minHorasEtapa: '0' });
    void this.apply();
  }
  changeView(view: string) {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { view }, queryParamsHandling: 'merge' });
  }
  changePage(page: number) {
    void this.router.navigate([], { relativeTo: this.route,
      queryParams: { ...this.appliedFilters, page, view: this.view() } });
  }
  verifiedAt() {
    const value = this.snapshot();
    return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: value.fuso }).format(new Date(value.verificadoEm)) : '';
  }
}
