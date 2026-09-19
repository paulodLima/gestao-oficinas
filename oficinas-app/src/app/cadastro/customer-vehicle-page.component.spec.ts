import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CustomerVehiclePageComponent } from './customer-vehicle-page.component';
import { Customer, CustomerVehicleService, Vehicle } from './customer-vehicle.service';

describe('Clientes e veículos', () => {
  const customer: Customer = { id: 'c1', nome: 'Ana Souza', cpf: '52998224725', telefone: '(61) 99999-0000',
    email: 'ana@example.test', emailVerificadoEm: null, ativo: true, versao: 0 };
  const other: Customer = { ...customer, id: 'c2', nome: 'Bruno Lima', cpf: '16899535009', email: 'bruno@example.test' };
  const vehicle: Vehicle = { id: 'v1', placa: 'BRA1E23', marca: 'Volkswagen', modelo: 'T-Cross', ano: 2024,
    cor: 'Cinza', clienteId: 'c1', clienteNome: 'Ana Souza', vinculoDesde: '2026-09-19T10:00:00Z', versao: 0 };
  let service: jasmine.SpyObj<CustomerVehicleService>;

  beforeEach(() => {
    service = jasmine.createSpyObj<CustomerVehicleService>('CustomerVehicleService', [
      'customers', 'vehicles', 'createCustomer', 'updateCustomer', 'requestVerification', 'confirmVerification',
      'createVehicle', 'updateVehicle', 'transfer'
    ]);
    service.customers.and.resolveTo({ items: [customer, other], page: 0, size: 100, totalElements: 2, totalPages: 1 });
    service.vehicles.and.resolveTo({ items: [vehicle], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    TestBed.configureTestingModule({ imports: [CustomerVehiclePageComponent], providers: [provideRouter([]), { provide: CustomerVehicleService, useValue: service }] });
  });

  it('carrega os dois diretórios e valida CPF e placa antes de enviar', async () => {
    const component = TestBed.createComponent(CustomerVehiclePageComponent).componentInstance;
    await component.load();
    expect(component.customers().length).toBe(2);
    expect(component.vehicles().length).toBe(1);
    component.customerForm.setValue({ nome: 'Inválido', cpf: '111', telefone: '', email: '' });
    await component.saveCustomer();
    expect(service.createCustomer).not.toHaveBeenCalled();
    component.vehicleForm.setValue({ placa: 'ABC12D3', marca: 'Marca', modelo: 'Modelo', ano: '2024', cor: '', clienteId: 'c1' });
    await component.saveVehicle();
    expect(service.createVehicle).not.toHaveBeenCalled();
  });

  it('cadastra cliente somente após a resposta da API', async () => {
    const component = TestBed.createComponent(CustomerVehiclePageComponent).componentInstance;
    await component.load(); component.newCustomer();
    component.customerForm.setValue({ nome: 'Carla Dias', cpf: '529.982.247-25', telefone: '(61) 98888-0000', email: 'carla@example.test' });
    const saved = { ...customer, id: 'c3', nome: 'Carla Dias' };
    service.createCustomer.and.resolveTo(saved);
    await component.saveCustomer();
    expect(service.createCustomer).toHaveBeenCalledWith(jasmine.objectContaining({ cpf: '529.982.247-25' }));
    expect(component.editingCustomer()?.id).toBe('c3');
    expect(component.success()).toContain('cadastrado');
  });

  it('registra a troca de responsável com a versão corrente', async () => {
    const component = TestBed.createComponent(CustomerVehiclePageComponent).componentInstance;
    await component.load(); component.editVehicle(vehicle); component.transferCustomer.setValue('c2');
    service.transfer.and.resolveTo({ ...vehicle, clienteId: 'c2', clienteNome: 'Bruno Lima', versao: 1 });
    await component.transfer();
    expect(service.transfer).toHaveBeenCalledWith(vehicle, 'c2');
    expect(component.editingVehicle()?.clienteNome).toBe('Bruno Lima');
    expect(component.success()).toContain('histórico');
  });

  it('preserva formulário e remove sucesso quando a rede falha', async () => {
    const component = TestBed.createComponent(CustomerVehiclePageComponent).componentInstance;
    await component.load(); component.editCustomer(customer);
    component.customerForm.controls.nome.setValue('Ana Atualizada');
    service.updateCustomer.and.rejectWith(new Error('offline'));
    await component.saveCustomer();
    expect(component.customerForm.controls.nome.value).toBe('Ana Atualizada');
    expect(component.success()).toBe('');
    expect(component.error()).toBeTruthy();
  });
});
