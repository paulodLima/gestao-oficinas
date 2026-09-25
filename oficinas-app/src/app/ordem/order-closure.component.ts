import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClosureInput, ClosureSummary, ServiceOrder, ServiceOrderService } from './service-order.service';

@Component({
  selector: 'app-order-closure',
  imports: [ReactiveFormsModule],
  templateUrl: './order-closure.component.html',
  styleUrl: './order-closure.component.css'
})
export class OrderClosureComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) order!: ServiceOrder;
  @Output() closed = new EventEmitter<ServiceOrder>();
  @Output() returnVisit = new EventEmitter<ServiceOrder>();
  private readonly service = inject(ServiceOrderService);
  private readonly builder = inject(FormBuilder);
  private revision = 0;
  readonly summary = signal<ClosureSummary | null>(null);
  readonly reviewing = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly form = this.builder.nonNullable.group({
    tipo: ['ENTREGUE' as ClosureInput['tipo']], motivo: ['', Validators.maxLength(1000)],
    confirmado: [false, Validators.requiredTrue], cancelarPendencias: [false]
  });
  active() { return !['ENTREGUE', 'CANCELADO'].includes(this.order.status); }
  ngOnChanges() {
    this.revision++; this.reviewing.set(false); this.busy.set(false); this.error.set(''); this.summary.set(null);
    this.form.reset({ tipo: 'ENTREGUE', motivo: '', confirmado: false, cancelarPendencias: false });
    if (!this.active()) void this.load(false);
  }
  ngOnDestroy() { this.revision++; }
  async review() { await this.load(true); }
  back() { this.reviewing.set(false); this.form.controls.confirmado.setValue(false); }
  private async load(review: boolean) {
    const revision = this.revision;
    this.busy.set(true); this.error.set('');
    try {
      const summary = await this.service.closureSummary(this.order.id);
      if (revision !== this.revision) return;
      this.summary.set(summary); this.reviewing.set(review);
      this.form.controls.confirmado.setValue(false);
      this.form.controls.cancelarPendencias.setValue(false);
    } catch (error) { if (revision === this.revision) this.showError(error); }
    finally { if (revision === this.revision) this.busy.set(false); }
  }
  async confirm() {
    if (this.busy() || !this.active() || !this.reviewing()) return;
    const summary = this.summary();
    const value = this.form.getRawValue();
    this.form.markAllAsTouched(); this.error.set('');
    if (!summary || this.form.invalid || (value.tipo === 'CANCELADO' && !value.motivo.trim())) {
      this.error.set('Confirme o encerramento e informe o motivo se for um cancelamento.'); return;
    }
    if (summary.pendencias > 0 && !value.cancelarPendencias) {
      this.error.set('Resolva as pendências ou marque seu cancelamento explícito.'); return;
    }
    const revision = this.revision;
    this.busy.set(true);
    try {
      const result = await this.service.closeOrder(this.order.id, { ...value, expectedVersion: summary.versao });
      if (revision === this.revision) { this.reviewing.set(false); this.closed.emit(result); }
    } catch (error) {
      if (revision !== this.revision) return;
      this.showError(error);
      // A conflict must be reviewed again, never silently retried with a fresh version.
      this.reviewing.set(false); this.form.controls.confirmado.setValue(false);
    } finally { if (revision === this.revision) this.busy.set(false); }
  }
  date(value: string) { return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)); }
  private showError(error: unknown) {
    this.error.set(error instanceof HttpErrorResponse ? error.error?.detail ?? 'Não foi possível consultar ou encerrar a OS.' : 'Não foi possível consultar ou encerrar a OS.');
  }
}
