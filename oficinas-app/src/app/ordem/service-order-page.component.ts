import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Customer, CustomerVehicleService, Vehicle } from '../cadastro/customer-vehicle.service';
import { ServiceOrder, ServiceOrderInput, ServiceOrderService, ServiceOrderStatus } from './service-order.service';

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
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly search = this.builder.nonNullable.control('', Validators.maxLength(100));
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

  ngOnInit() { void this.load(); }
  async load() {
    this.loading.set(true); this.clearMessages();
    try {
      const [orders, customers, vehicles] = await Promise.all([
        this.service.orders(), this.registrations.customers(), this.registrations.vehicles()
      ]);
      this.orders.set(orders.items); this.customers.set(customers.items); this.vehicles.set(vehicles.items);
      this.selected.set(orders.items[0] ?? null);
    } catch (error) { this.showError(error, 'Não foi possível carregar as ordens de serviço.'); }
    finally { this.loading.set(false); }
  }
  newOrder() {
    this.selected.set(null); this.clearMessages();
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
    await this.perform(async () => this.selected.set(await this.service.order(order.id)), 'Detalhes atualizados.');
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
    }, 'Ordem de serviço aberta com sucesso.');
  }
  formatNumber(value: number) { return `OS-${value.toString().padStart(6, '0')}`; }
  formatPlate(value: string) { return value.length === 7 ? value.slice(0, 3) + '-' + value.slice(3) : value; }
  formatDate(value: string | null) {
    return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : 'Não informada';
  }
  statusLabel(value: ServiceOrderStatus) {
    return value.toLowerCase().replaceAll('_', ' ').replace(/^./, letter => letter.toUpperCase());
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
