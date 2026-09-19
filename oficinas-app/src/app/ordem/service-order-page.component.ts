import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Customer, CustomerVehicleService, Vehicle } from '../cadastro/customer-vehicle.service';
import { ServiceOrder, ServiceOrderEvent, ServiceOrderInput, ServiceOrderService,
  ServiceOrderStatus } from './service-order.service';

const ACTIVE_STATUSES: ServiceOrderStatus[] = ['RECEBIDO', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
  'AGUARDANDO_PECAS', 'EM_MANUTENCAO', 'FUNILARIA', 'PINTURA', 'EM_MONTAGEM', 'EM_TESTES',
  'PRONTO_PARA_RETIRADA'];
const STATUS_SEQUENCE: Record<ServiceOrderStatus, number> = {
  RECEBIDO: 0, EM_DIAGNOSTICO: 10, AGUARDANDO_APROVACAO: 20, AGUARDANDO_PECAS: 30,
  EM_MANUTENCAO: 40, FUNILARIA: 42, PINTURA: 44, EM_MONTAGEM: 50, EM_TESTES: 60,
  PRONTO_PARA_RETIRADA: 70, ENTREGUE: 100, CANCELADO: 100
};

@Component({
  selector: 'app-service-order-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './service-order-page.component.html',
  styleUrl: './service-order-page.component.css'
})
export class ServiceOrderPageComponent implements OnInit {
  private readonly service = inject(ServiceOrderService);
  private readonly registrations = inject(CustomerVehicleService);
  private readonly builder = inject(FormBuilder);
  readonly orders = signal<ServiceOrder[]>([]);
  readonly customers = signal<Customer[]>([]);
  readonly vehicles = signal<Vehicle[]>([]);
  readonly selected = signal<ServiceOrder | null>(null);
  readonly timeline = signal<ServiceOrderEvent[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly search = this.builder.nonNullable.control('', Validators.maxLength(100));
  readonly statusOptions = ACTIVE_STATUSES;
  readonly activeCount = computed(() => this.orders().filter(item => !['ENTREGUE', 'CANCELADO'].includes(item.status)).length);
  readonly availableVehicles = computed(() => {
    const customerId = this.form.controls.clienteId.value;
    return customerId ? this.vehicles().filter(item => item.clienteId === customerId) : this.vehicles();
  });
  readonly form = this.builder.nonNullable.group({
    clienteId: ['', Validators.required],
    veiculoId: ['', Validators.required],
    relatoInicial: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    entradaEm: [this.localDateTime(new Date()), Validators.required],
    kmEntrada: ['', [Validators.required, Validators.pattern(/^\d{1,7}$/)]],
    previsaoEm: ['']
  });
  readonly statusForm = this.builder.nonNullable.group({
    status: ['' as ServiceOrderStatus | '', Validators.required],
    motivo: ['', Validators.maxLength(1000)],
    textoPublico: ['', Validators.maxLength(2000)],
    textoInterno: ['', Validators.maxLength(2000)]
  });
  readonly updateForm = this.builder.nonNullable.group({
    textoPublico: ['', Validators.maxLength(2000)],
    textoInterno: ['', Validators.maxLength(2000)],
    publicada: [false]
  });

  ngOnInit() { void this.load(); }
  async load() {
    this.loading.set(true); this.clearMessages();
    try {
      const [orders, customers, vehicles] = await Promise.all([
        this.service.orders(), this.registrations.customers(), this.registrations.vehicles()
      ]);
      this.orders.set(orders.items); this.customers.set(customers.items); this.vehicles.set(vehicles.items);
      const first = orders.items[0] ?? null;
      this.selected.set(first);
      if (first) await this.loadTimeline(first.id);
    } catch (error) { this.showError(error, 'Não foi possível carregar as ordens de serviço.'); }
    finally { this.loading.set(false); }
  }
  newOrder() {
    this.selected.set(null); this.timeline.set([]); this.clearMessages();
    this.form.reset({ clienteId: '', veiculoId: '', relatoInicial: '',
      entradaEm: this.localDateTime(new Date()), kmEntrada: '', previsaoEm: '' });
  }
  syncVehicle() {
    const vehicle = this.vehicles().find(item => item.id === this.form.controls.veiculoId.value);
    if (vehicle?.clienteId !== this.form.controls.clienteId.value) this.form.controls.veiculoId.reset('');
  }
  async searchOrders() {
    if (this.search.invalid || this.busy()) return;
    await this.perform(async () => {
      const result = await this.service.orders(this.search.value);
      this.orders.set(result.items); this.selected.set(result.items[0] ?? null);
    }, 'Busca atualizada.');
  }
  async selectOrder(order: ServiceOrder) {
    await this.perform(async () => {
      const detail = await this.service.order(order.id);
      this.selected.set(detail); this.resetWorkflowForms(); await this.loadTimeline(detail.id);
    }, 'Detalhes atualizados.');
  }
  async createOrder() {
    this.form.markAllAsTouched(); this.clearMessages();
    if (this.form.invalid) { this.error.set('Confira cliente, veículo, relato, entrada e quilometragem.'); return; }
    const value = this.form.getRawValue();
    const input: ServiceOrderInput = {
      clienteId: value.clienteId, veiculoId: value.veiculoId, relatoInicial: value.relatoInicial,
      entradaEm: new Date(value.entradaEm).toISOString(), kmEntrada: Number(value.kmEntrada),
      previsaoEm: value.previsaoEm ? new Date(value.previsaoEm).toISOString() : null
    };
    await this.perform(async () => {
      const opened = await this.service.create(input);
      this.orders.update(items => [opened, ...items]); this.selected.set(opened);
      this.resetWorkflowForms(); await this.loadTimeline(opened.id);
    }, 'Ordem de serviço aberta com sucesso.');
  }
  async changeStatus() {
    const order = this.selected();
    this.statusForm.markAllAsTouched(); this.clearMessages();
    if (!order || this.statusForm.invalid) { this.error.set('Selecione a nova etapa.'); return; }
    if (this.requiresReason() && !this.statusForm.controls.motivo.value.trim()) {
      this.error.set('Informe o motivo para retornar ou colocar uma etapa em espera.'); return;
    }
    const value = this.statusForm.getRawValue();
    await this.perform(async () => {
      const updated = await this.service.changeStatus(order.id, { ...value,
        status: value.status as ServiceOrderStatus, expectedVersion: order.versao });
      this.replaceOrder(updated); this.statusForm.reset({ status: '', motivo: '', textoPublico: '', textoInterno: '' });
      await this.loadTimeline(order.id);
    }, 'Etapa atualizada e registrada na linha do tempo.');
  }
  async publishUpdate() {
    const order = this.selected();
    this.updateForm.markAllAsTouched(); this.clearMessages();
    const value = this.updateForm.getRawValue();
    if (!order || this.updateForm.invalid || (!value.textoPublico.trim() && !value.textoInterno.trim())) {
      this.error.set('Escreva um texto público ou uma observação interna.'); return;
    }
    if (value.publicada && !value.textoPublico.trim()) {
      this.error.set('Escreva o texto público antes de publicar para o cliente.'); return;
    }
    await this.perform(async () => {
      await this.service.publish(order.id, { ...value, expectedVersion: order.versao });
      const updated = await this.service.order(order.id);
      this.replaceOrder(updated); this.updateForm.reset({ textoPublico: '', textoInterno: '', publicada: false });
      await this.loadTimeline(order.id);
    }, value.publicada ? 'Atualização publicada na linha do tempo.' : 'Observação interna registrada.');
  }
  requiresReason() {
    const current = this.selected()?.status;
    const target = this.statusForm.controls.status.value as ServiceOrderStatus | '';
    if (!current || !target) return false;
    const returning = STATUS_SEQUENCE[target] < STATUS_SEQUENCE[current];
    const waiting = ['AGUARDANDO_APROVACAO', 'AGUARDANDO_PECAS'].includes(target);
    const executing = !['RECEBIDO', 'AGUARDANDO_APROVACAO', 'AGUARDANDO_PECAS',
      'PRONTO_PARA_RETIRADA', 'ENTREGUE', 'CANCELADO'].includes(current);
    return returning || waiting && executing;
  }
  eventTitle(event: ServiceOrderEvent) {
    return event.tipo === 'STATUS' ? (event.statusAnterior ?
      `${this.statusLabel(event.statusAnterior)} → ${this.statusLabel(event.statusNovo!)}` :
      `Ordem recebida · ${this.statusLabel(event.statusNovo!)}`) : 'Atualização registrada';
  }
  formatNumber(value: number) { return `OS-${value.toString().padStart(6, '0')}`; }
  formatPlate(value: string) { return value.length === 7 ? value.slice(0, 3) + '-' + value.slice(3) : value; }
  formatDate(value: string | null) {
    return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : 'Não informada';
  }
  statusLabel(value: ServiceOrderStatus) {
    return value.toLowerCase().replaceAll('_', ' ').replace(/^./, letter => letter.toUpperCase());
  }
  private async loadTimeline(id: string) { this.timeline.set(await this.service.timeline(id)); }
  private replaceOrder(order: ServiceOrder) {
    this.selected.set(order);
    this.orders.update(items => items.map(item => item.id === order.id ? order : item));
  }
  private resetWorkflowForms() {
    this.statusForm.reset({ status: '', motivo: '', textoPublico: '', textoInterno: '' });
    this.updateForm.reset({ textoPublico: '', textoInterno: '', publicada: false });
  }
  private localDateTime(value: Date) {
    const local = new Date(value.getTime() - value.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 16);
  }
  private async perform(action: () => Promise<void>, message: string) {
    if (this.busy()) return;
    this.busy.set(true); this.clearMessages();
    try { await action(); this.success.set(message); }
    catch (error) { this.showError(error, 'Não foi possível concluir. Confira a conexão e tente novamente.'); }
    finally { this.busy.set(false); }
  }
  private clearMessages() { this.error.set(''); this.success.set(''); }
  private showError(error: unknown, fallback: string) {
    this.error.set(error instanceof HttpErrorResponse ? error.error?.detail ?? fallback : fallback);
  }
}
