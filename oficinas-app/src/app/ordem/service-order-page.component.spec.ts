import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Customer, CustomerVehicleService, Vehicle } from '../cadastro/customer-vehicle.service';
import { ServiceOrder, ServiceOrderService } from './service-order.service';
import { ServiceOrderPageComponent } from './service-order-page.component';

describe('Ordens de serviço', () => {
  const customer: Customer = { id: 'c1', nome: 'Ana Souza', cpf: '52998224725', telefone: '',
    email: '', emailVerificadoEm: null, ativo: true, versao: 0 };
  const vehicle: Vehicle = { id: 'v1', placa: 'BRA1E23', marca: 'Volkswagen', modelo: 'T-Cross',
    ano: 2024, cor: 'Cinza', clienteId: 'c1', clienteNome: 'Ana Souza',
    vinculoDesde: '2026-09-19T10:00:00Z', versao: 0 };
  const order: ServiceOrder = { id: 'o1', numero: 1, clienteId: 'c1', clienteNome: 'Ana Souza',
    veiculoId: 'v1', placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross',
    relatoInicial: 'Ruído na suspensão dianteira.', entradaEm: '2026-09-19T10:00:00Z',
    kmEntrada: 48210, status: 'RECEBIDO', previsaoEm: null, versao: 0, createdAt: '2026-09-19T10:00:00Z' };
  let service: jasmine.SpyObj<ServiceOrderService>;
  let registrations: jasmine.SpyObj<CustomerVehicleService>;

  beforeEach(() => {
    service = jasmine.createSpyObj<ServiceOrderService>('ServiceOrderService', ['orders', 'order', 'create']);
    registrations = jasmine.createSpyObj<CustomerVehicleService>('CustomerVehicleService', ['customers', 'vehicles']);
    service.orders.and.resolveTo({ items: [order], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    service.order.and.resolveTo(order);
    registrations.customers.and.resolveTo({ items: [customer], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    registrations.vehicles.and.resolveTo({ items: [vehicle], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    TestBed.configureTestingModule({ imports: [ServiceOrderPageComponent], providers: [provideRouter([]),
      { provide: ServiceOrderService, useValue: service }, { provide: CustomerVehicleService, useValue: registrations }] });
  });

  it('carrega diretório, cadastros e detalhe inicial', async () => {
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load();
    expect(component.orders()).toEqual([order]);
    expect(component.selected()).toEqual(order);
    expect(component.activeCount()).toBe(1);
  });

  it('impede abertura com relato e quilometragem inválidos', async () => {
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.newOrder();
    component.form.patchValue({ clienteId: 'c1', veiculoId: 'v1', relatoInicial: 'curto', kmEntrada: '-1' });
    await component.createOrder();
    expect(service.create).not.toHaveBeenCalled();
    expect(component.error()).toContain('Confira');
  });

  it('abre a OS somente após confirmação da API', async () => {
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.newOrder();
    component.form.patchValue({ clienteId: 'c1', veiculoId: 'v1',
      relatoInicial: 'Ruído na suspensão dianteira ao passar em desníveis.', kmEntrada: '48210' });
    service.create.and.resolveTo(order);
    await component.createOrder();
    expect(service.create).toHaveBeenCalledWith(jasmine.objectContaining({ clienteId: 'c1', veiculoId: 'v1', kmEntrada: 48210 }));
    expect(component.selected()).toEqual(order);
    expect(component.success()).toContain('aberta');
  });

  it('filtra veículos pelo responsável selecionado', async () => {
    const other = { ...vehicle, id: 'v2', clienteId: 'c2', clienteNome: 'Outro' };
    registrations.vehicles.and.resolveTo({ items: [vehicle, other], page: 0, size: 100, totalElements: 2, totalPages: 1 });
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.newOrder(); component.form.controls.clienteId.setValue('c1');
    expect(component.availableVehicles()).toEqual([vehicle]);
  });
});
