import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Customer, CustomerInput, CustomerVehicleService, Vehicle, VehicleInput } from './customer-vehicle.service';

@Component({
  selector: 'app-customer-vehicle-page',
  imports: [ReactiveFormsModule],
  templateUrl: './customer-vehicle-page.component.html',
  styleUrl: './customer-vehicle-page.component.css'
})
export class CustomerVehiclePageComponent implements OnInit {
  private readonly service = inject(CustomerVehicleService);
  private readonly route = inject(ActivatedRoute);
  private readonly builder = inject(FormBuilder);
  readonly customers = signal<Customer[]>([]);
  readonly vehicles = signal<Vehicle[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly pane = signal<'clientes' | 'veiculos'>('clientes');
  readonly editingCustomer = signal<Customer | null>(null);
  readonly editingVehicle = signal<Vehicle | null>(null);
  readonly challenge = signal<{ clienteId: string; desafioId: string } | null>(null);
  readonly activeCustomers = computed(() => this.customers().filter(item => item.ativo));
  readonly customerSearch = this.builder.nonNullable.control('', Validators.maxLength(100));
  readonly vehicleSearch = this.builder.nonNullable.control('', Validators.maxLength(100));
  readonly verificationCode = this.builder.nonNullable.control('', [Validators.required, Validators.pattern(/^\d{6}$/)]);
  readonly transferCustomer = this.builder.nonNullable.control('');
  readonly customerForm = this.builder.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    cpf: ['', [Validators.required, Validators.pattern(/^(?:\d{11}|\d{3}\.\d{3}\.\d{3}-\d{2})$/)]],
    telefone: ['', Validators.pattern(/^[+0-9() .-]{8,30}$/)],
    email: ['', [Validators.email, Validators.maxLength(254)]]
  });
  readonly vehicleForm = this.builder.nonNullable.group({
    placa: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{3}[- ]?(?:\d{4}|\d[A-Za-z]\d{2})$/)]],
    marca: ['', [Validators.required, Validators.maxLength(80)]],
    modelo: ['', [Validators.required, Validators.maxLength(120)]],
    ano: ['', Validators.pattern(/^$|^\d{4}$/)],
    cor: ['', Validators.maxLength(50)],
    clienteId: ['', Validators.required]
  });

  ngOnInit() {
    const pane = this.route.snapshot.data['pane'];
    if (pane === 'clientes' || pane === 'veiculos') this.pane.set(pane);
    void this.load();
  }
  async load() {
    this.loading.set(true); this.clearMessages();
    try {
      const [customers, vehicles] = await Promise.all([this.service.customers(), this.service.vehicles()]);
      this.customers.set(customers.items); this.vehicles.set(vehicles.items);
    } catch (error) { this.showError(error, 'Não foi possível carregar os cadastros.'); }
    finally { this.loading.set(false); }
  }
  async searchCustomers() {
    if (this.customerSearch.invalid || this.busy()) return;
    await this.perform(async () => this.customers.set((await this.service.customers(this.customerSearch.value)).items), 'Busca atualizada.');
  }
  async searchVehicles() {
    if (this.vehicleSearch.invalid || this.busy()) return;
    await this.perform(async () => this.vehicles.set((await this.service.vehicles(this.vehicleSearch.value)).items), 'Busca atualizada.');
  }
  newCustomer() {
    this.editingCustomer.set(null); this.customerForm.reset({ nome: '', cpf: '', telefone: '', email: '' });
    this.challenge.set(null); this.clearMessages();
  }
  editCustomer(customer: Customer) {
    this.editingCustomer.set(customer); this.customerForm.reset({ nome: customer.nome, cpf: this.formatCpf(customer.cpf), telefone: customer.telefone, email: customer.email });
    this.challenge.set(null); this.clearMessages();
  }
  async saveCustomer() {
    this.customerForm.markAllAsTouched(); this.clearMessages();
    if (this.customerForm.invalid) { this.error.set('Confira nome, CPF, telefone e e-mail.'); return; }
    const input: CustomerInput = this.customerForm.getRawValue();
    const current = this.editingCustomer();
    await this.perform(async () => {
      const saved = current ? await this.service.updateCustomer(current, input) : await this.service.createCustomer(input);
      this.replaceCustomer(saved); this.editCustomer(saved);
    }, current ? 'Cliente atualizado.' : 'Cliente cadastrado.');
  }
  async requestVerification(customer: Customer) {
    await this.perform(async () => {
      const result = await this.service.requestVerification(customer.id);
      this.challenge.set({ clienteId: customer.id, desafioId: result.desafioId });
      this.verificationCode.reset('');
    }, 'Código enviado para o e-mail cadastrado.');
  }
  async confirmVerification(customer: Customer) {
    const challenge = this.challenge(); this.verificationCode.markAsTouched();
    if (!challenge || challenge.clienteId !== customer.id || this.verificationCode.invalid) {
      this.error.set('Informe o código de seis dígitos enviado por e-mail.'); return;
    }
    await this.perform(async () => {
      const saved = await this.service.confirmVerification(customer.id, challenge.desafioId, this.verificationCode.value);
      this.replaceCustomer(saved); this.editingCustomer.set(saved); this.challenge.set(null);
    }, 'E-mail verificado. O cliente já pode receber códigos de acesso.');
  }
  newVehicle() {
    this.editingVehicle.set(null); this.vehicleForm.reset({ placa: '', marca: '', modelo: '', ano: '', cor: '', clienteId: '' });
    this.transferCustomer.reset(''); this.clearMessages();
  }
  editVehicle(vehicle: Vehicle) {
    this.editingVehicle.set(vehicle);
    this.vehicleForm.reset({ placa: this.formatPlate(vehicle.placa), marca: vehicle.marca, modelo: vehicle.modelo,
      ano: vehicle.ano?.toString() ?? '', cor: vehicle.cor, clienteId: vehicle.clienteId });
    this.transferCustomer.reset(vehicle.clienteId); this.clearMessages();
  }
  async saveVehicle() {
    this.vehicleForm.markAllAsTouched(); this.clearMessages();
    if (this.vehicleForm.invalid) { this.error.set('Confira placa, marca, modelo, ano e responsável.'); return; }
    const value = this.vehicleForm.getRawValue();
    const input: VehicleInput = { ...value, ano: value.ano ? Number(value.ano) : null };
    const current = this.editingVehicle();
    await this.perform(async () => {
      const saved = current
        ? await this.service.updateVehicle(current, { placa: input.placa, marca: input.marca, modelo: input.modelo, ano: input.ano, cor: input.cor })
        : await this.service.createVehicle(input);
      this.replaceVehicle(saved); this.editVehicle(saved);
    }, current ? 'Veículo atualizado.' : 'Veículo cadastrado.');
  }
  async transfer() {
    const vehicle = this.editingVehicle(); const customerId = this.transferCustomer.value;
    if (!vehicle || !customerId || customerId === vehicle.clienteId) {
      this.error.set('Selecione outro cliente para registrar a troca de responsável.'); return;
    }
    await this.perform(async () => {
      const saved = await this.service.transfer(vehicle, customerId);
      this.replaceVehicle(saved); this.editVehicle(saved);
    }, 'Responsável alterado. O vínculo anterior foi preservado no histórico.');
  }
  customerName(id: string) { return this.customers().find(item => item.id === id)?.nome ?? 'Cliente indisponível'; }
  formatCpf(value: string) { return value.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4'); }
  formatPlate(value: string) { return value.length === 7 ? value.slice(0, 3) + '-' + value.slice(3) : value; }
  private replaceCustomer(saved: Customer) {
    this.customers.update(items => [...items.filter(item => item.id !== saved.id), saved].sort((a, b) => a.nome.localeCompare(b.nome)));
  }
  private replaceVehicle(saved: Vehicle) {
    this.vehicles.update(items => [...items.filter(item => item.id !== saved.id), saved].sort((a, b) => a.placa.localeCompare(b.placa)));
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
