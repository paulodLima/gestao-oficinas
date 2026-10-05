import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Customer, CustomerInput, CustomerVehicleService, Vehicle, VehicleInput } from './customer-vehicle.service';
import { STANDARD_VEHICLE_COLORS, VEHICLE_BRANDS, VehicleBrandCatalog } from './vehicle-catalog';

@Component({
  selector: 'app-customer-vehicle-page',
  imports: [ReactiveFormsModule],
  templateUrl: './customer-vehicle-page.component.html',
  styleUrl: './customer-vehicle-page.component.css'
})
export class CustomerVehiclePageComponent implements OnInit {
  private readonly service = inject(CustomerVehicleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly builder = inject(FormBuilder);
  readonly customers = signal<Customer[]>([]);
  readonly vehicles = signal<Vehicle[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly pane = signal<'clientes' | 'veiculos'>('clientes');
  readonly workspaceMode = signal<'search' | 'form'>('search');
  readonly hasSearched = signal(false);
  readonly editingCustomer = signal<Customer | null>(null);
  readonly editingVehicle = signal<Vehicle | null>(null);
  readonly selectedVehicleBrand = signal<VehicleBrandCatalog | null>(null);
  readonly brandMenuOpen = signal(false);
  readonly brandFilter = signal('');
  readonly manualVehicleBrand = signal(false);
  readonly manualVehicleModel = signal(false);
  readonly brandSearchState = signal<'idle' | 'found' | 'not-found'>('idle');
  readonly challenge = signal<{ clienteId: string; desafioId: string } | null>(null);
  readonly activeCustomers = computed(() => this.customers().filter(item => item.ativo));
  readonly availableModels = computed(() => this.selectedVehicleBrand()?.models ?? []);
  readonly filteredVehicleBrands = computed(() => {
    const filter = this.normalize(this.brandFilter());
    return filter ? VEHICLE_BRANDS.filter(brand => this.normalize(brand.name).includes(filter)) : VEHICLE_BRANDS;
  });
  readonly vehicleBrands = VEHICLE_BRANDS;
  readonly standardVehicleColors = STANDARD_VEHICLE_COLORS;
  readonly customerSearch = this.builder.nonNullable.control('', Validators.maxLength(100));
  readonly vehicleSearch = this.builder.nonNullable.control('', Validators.maxLength(100));
  readonly customVehicleModel = this.builder.nonNullable.control('', [Validators.required, Validators.maxLength(120)]);
  readonly verificationCode = this.builder.nonNullable.control('', [Validators.required, Validators.pattern(/^\d{6}$/)]);
  readonly transferCustomer = this.builder.nonNullable.control('');
  readonly customerForm = this.builder.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    cpf: ['', [Validators.required, Validators.pattern(/^(?:\d{11}|\d{3}\.\d{3}\.\d{3}-\d{2})$/)]],
    telefone: ['', Validators.pattern(/^$|^\(\d{2}\) \d{4,5}-\d{4}$/)],
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
      const customerId = this.route.snapshot.queryParamMap.get('clienteId');
      if (this.pane() === 'veiculos' && this.route.snapshot.queryParamMap.get('novo') === '1') {
        this.newVehicle();
        if (customerId) {
          const customer = await this.service.customer(customerId);
          if (!customer.ativo) { this.error.set('O cliente está inativo. Confira seu cadastro.'); return; }
          this.replaceCustomer(customer);
          this.vehicleForm.controls.clienteId.setValue(customer.id);
        }
      }
    } catch (error) { this.showError(error, 'Não foi possível carregar os cadastros.'); }
    finally { this.loading.set(false); }
  }
  async searchCustomers() {
    if (this.customerSearch.invalid || this.busy()) return;
    if (!this.customerSearch.value.trim()) { this.error.set('Digite um dado do cliente para pesquisar.'); return; }
    this.hasSearched.set(true);
    await this.perform(async () => this.customers.set((await this.service.customers(this.customerSearch.value)).items), 'Busca atualizada.');
  }
  async searchVehicles() {
    if (this.vehicleSearch.invalid || this.busy()) return;
    if (!this.vehicleSearch.value.trim()) { this.error.set('Digite placa, veículo ou cliente para pesquisar.'); return; }
    this.hasSearched.set(true);
    await this.perform(async () => this.vehicles.set((await this.service.vehicles(this.vehicleSearch.value)).items), 'Busca atualizada.');
  }
  newCustomer() {
    this.workspaceMode.set('form');
    this.editingCustomer.set(null); this.customerForm.reset({ nome: '', cpf: '', telefone: '', email: '' });
    this.challenge.set(null); this.clearMessages();
  }
  editCustomer(customer: Customer) {
    this.workspaceMode.set('form');
    this.editingCustomer.set(customer); this.customerForm.reset({ nome: customer.nome, cpf: this.formatCpf(customer.cpf), telefone: customer.telefone, email: customer.email });
    this.challenge.set(null); this.clearMessages();
  }
  async saveCustomer(continueToVehicle = false) {
    if (this.busy()) return;
    this.customerForm.markAllAsTouched(); this.clearMessages();
    if (this.customerForm.invalid) { this.error.set('Confira nome, CPF, telefone e e-mail.'); return; }
    const input: CustomerInput = this.customerForm.getRawValue();
    const current = this.editingCustomer();
    await this.perform(async () => {
      const saved = current ? await this.service.updateCustomer(current, input) : await this.service.createCustomer(input);
      this.replaceCustomer(saved); this.editCustomer(saved);
      if (continueToVehicle) await this.router.navigate(['/veiculos'], { queryParams: { novo: '1', clienteId: saved.id } });
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
    this.workspaceMode.set('form');
    this.editingVehicle.set(null); this.vehicleForm.reset({ placa: '', marca: '', modelo: '', ano: '', cor: '', clienteId: '' });
    this.resetVehicleCatalog();
    this.transferCustomer.reset(''); this.clearMessages();
  }
  editVehicle(vehicle: Vehicle) {
    this.workspaceMode.set('form');
    this.editingVehicle.set(vehicle);
    this.vehicleForm.reset({ placa: this.formatPlate(vehicle.placa), marca: vehicle.marca, modelo: vehicle.modelo,
      ano: vehicle.ano?.toString() ?? '', cor: vehicle.cor, clienteId: vehicle.clienteId });
    this.findModelsForBrand(false);
    this.transferCustomer.reset(vehicle.clienteId); this.clearMessages();
  }
  toggleBrandMenu() {
    this.brandMenuOpen.update(open => !open);
    if (this.brandMenuOpen()) this.brandFilter.set('');
  }
  selectVehicleBrand(brand: VehicleBrandCatalog) {
    this.vehicleForm.controls.marca.setValue(brand.name);
    this.selectedVehicleBrand.set(brand); this.manualVehicleBrand.set(false); this.brandMenuOpen.set(false);
    this.brandSearchState.set('found'); this.clearSelectedModel();
  }
  startManualBrand() {
    this.vehicleForm.controls.marca.setValue(''); this.selectedVehicleBrand.set(null); this.brandMenuOpen.set(false);
    this.manualVehicleBrand.set(true); this.brandSearchState.set('idle'); this.clearSelectedModel();
  }
  returnToBrandCatalog() {
    this.vehicleForm.controls.marca.setValue(''); this.selectedVehicleBrand.set(null);
    this.manualVehicleBrand.set(false); this.brandSearchState.set('idle'); this.clearSelectedModel();
  }
  onBrandInput() {
    if (this.selectedVehicleBrand() && this.normalize(this.selectedVehicleBrand()!.name) !== this.normalize(this.vehicleForm.controls.marca.value)) {
      this.selectedVehicleBrand.set(null); this.brandSearchState.set('idle');
    }
  }
  findModelsForBrand(clearModel = true) {
    const brandName = this.vehicleForm.controls.marca.value.trim();
    const brand = VEHICLE_BRANDS.find(item => this.normalize(item.name) === this.normalize(brandName));
    if (!brand) {
      this.selectedVehicleBrand.set(null); this.manualVehicleBrand.set(Boolean(brandName));
      this.brandSearchState.set(brandName ? 'not-found' : 'idle');
      return;
    }
    this.selectedVehicleBrand.set(brand); this.manualVehicleBrand.set(false); this.brandSearchState.set('found');
    this.vehicleForm.controls.marca.setValue(brand.name);
    const modelIsCatalogued = brand.models.some(model => this.normalize(model) === this.normalize(this.vehicleForm.controls.modelo.value));
    if (clearModel) {
      this.clearSelectedModel();
    } else if (!modelIsCatalogued && this.vehicleForm.controls.modelo.value) {
      this.manualVehicleModel.set(true); this.customVehicleModel.setValue(this.vehicleForm.controls.modelo.value);
    } else {
      this.manualVehicleModel.set(false); this.customVehicleModel.setValue('');
    }
  }
  chooseOtherModel() {
    this.vehicleForm.controls.modelo.setValue(''); this.customVehicleModel.setValue(''); this.manualVehicleModel.set(true);
  }
  onVehicleModelSelection() {
    if (this.vehicleForm.controls.modelo.value === '__outro__') this.chooseOtherModel();
    else this.chooseCataloguedModel();
  }
  chooseCataloguedModel() {
    this.manualVehicleModel.set(false); this.customVehicleModel.setValue('');
  }
  syncCustomModel() { this.vehicleForm.controls.modelo.setValue(this.customVehicleModel.value); }
  isStandardVehicleColor(color: string) { return STANDARD_VEHICLE_COLORS.includes(color as typeof STANDARD_VEHICLE_COLORS[number]); }
  async saveVehicle(continueToOrder = false) {
    if (this.busy()) return;
    this.vehicleForm.markAllAsTouched(); this.customVehicleModel.markAsTouched(); this.clearMessages();
    if (this.vehicleForm.invalid || (this.manualVehicleModel() && this.customVehicleModel.invalid)) { this.error.set('Confira placa, marca, modelo, ano e responsável.'); return; }
    const value = this.vehicleForm.getRawValue();
    const input: VehicleInput = { ...value, ano: value.ano ? Number(value.ano) : null };
    const current = this.editingVehicle();
    await this.perform(async () => {
      const saved = current
        ? await this.service.updateVehicle(current, { placa: input.placa, marca: input.marca, modelo: input.modelo, ano: input.ano, cor: input.cor })
        : await this.service.createVehicle(input);
      this.replaceVehicle(saved); this.editVehicle(saved);
      if (continueToOrder) await this.router.navigate(['/abrir-ordem'], { queryParams: { novo: '1', veiculoId: saved.id } });
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
  backToSearch() { this.workspaceMode.set('search'); this.clearMessages(); }
  formatCpf(value: string) { return value.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4'); }
  formatCpfInput() {
    const digits = this.customerForm.controls.cpf.value.replace(/\D/g, '').slice(0, 11);
    this.customerForm.controls.cpf.setValue(this.formatCpf(digits), { emitEvent: false });
  }
  formatCustomerPhone() {
    const digits = this.customerForm.controls.telefone.value.replace(/\D/g, '').slice(0, 11);
    const value = digits.length <= 2 ? digits : digits.length <= 6 ? `(${digits.slice(0, 2)}) ${digits.slice(2)}`
      : digits.length <= 10 ? `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`
      : `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
    this.customerForm.controls.telefone.setValue(value, { emitEvent: false });
  }
  formatPlate(value: string) { return value.length === 7 ? value.slice(0, 3) + '-' + value.slice(3) : value; }
  private normalize(value: string) { return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase(); }
  private clearSelectedModel() {
    this.vehicleForm.controls.modelo.setValue(''); this.customVehicleModel.setValue(''); this.manualVehicleModel.set(false);
  }
  private resetVehicleCatalog() {
    this.selectedVehicleBrand.set(null); this.brandMenuOpen.set(false); this.brandFilter.set('');
    this.manualVehicleBrand.set(false); this.brandSearchState.set('idle'); this.clearSelectedModel();
  }
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
