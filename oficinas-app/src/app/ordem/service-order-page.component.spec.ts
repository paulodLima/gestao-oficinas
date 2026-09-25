import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { Customer, CustomerVehicleService, Vehicle } from '../cadastro/customer-vehicle.service';
import { ServiceOrder, ServiceOrderEvent, ServiceOrderForecast, ServiceOrderService } from './service-order.service';
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
    kmEntrada: 48210, status: 'RECEBIDO', previsaoEm: null, atrasada: false,
    aguardandoRetirada: false, versao: 0, createdAt: '2026-09-19T10:00:00Z',
    updatedAt: '2026-09-19T10:00:00Z' };
  const event: ServiceOrderEvent = { id: 'e1', tipo: 'STATUS', statusAnterior: null,
    statusNovo: 'RECEBIDO', motivo: null, textoPublico: null, textoInterno: null,
    publicada: false, autorId: 'p1', autorNome: 'Dono', createdAt: '2026-09-19T10:00:00Z' };
  const forecast: ServiceOrderForecast = { id: 'f1', previsaoAnterior: null,
    previsaoNova: '2026-09-20T18:00:00Z', motivoPublico: 'Aguardando peça',
    proximaAcao: 'Confirmar entrega.', autorId: 'p1', autorNome: 'Dono',
    createdAt: '2026-09-19T11:00:00Z' };
  let service: jasmine.SpyObj<ServiceOrderService>;
  let registrations: jasmine.SpyObj<CustomerVehicleService>;

  beforeEach(() => {
    service = jasmine.createSpyObj<ServiceOrderService>('ServiceOrderService',
      ['orders', 'order', 'create', 'timeline', 'changeStatus', 'publish', 'forecasts', 'updateForecast',
        'photos', 'inspection', 'saveInspection', 'confirmInspection', 'correctInspection']);
    registrations = jasmine.createSpyObj<CustomerVehicleService>('CustomerVehicleService', ['customers', 'vehicles', 'customer', 'vehicle']);
    registrations.customer.and.resolveTo(customer);
    registrations.vehicle.and.resolveTo(vehicle);
    service.orders.and.resolveTo({ items: [order], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    service.order.and.resolveTo(order);
    service.timeline.and.resolveTo([event]);
    service.forecasts.and.resolveTo([forecast]);
    service.photos.and.resolveTo([]);
    service.inspection.and.resolveTo([]);
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
    expect(component.timeline()).toEqual([event]);
    expect(component.forecasts()).toEqual([forecast]);
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

  it('permite pular etapa e atualiza a versão exibida', async () => {
    const updated = { ...order, status: 'EM_TESTES' as const, versao: 1 };
    service.changeStatus.and.resolveTo(updated);
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.statusForm.controls.status.setValue('EM_TESTES');
    await component.changeStatus();
    expect(service.changeStatus).toHaveBeenCalledWith('o1', jasmine.objectContaining({
      status: 'EM_TESTES', expectedVersion: 0
    }));
    expect(component.selected()).toEqual(updated);
  });

  it('exige motivo no retorno antes de chamar a API', async () => {
    const current = { ...order, status: 'EM_TESTES' as const, versao: 2 };
    service.orders.and.resolveTo({ items: [current], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.statusForm.controls.status.setValue('EM_DIAGNOSTICO');
    expect(component.requiresReason()).toBeTrue();
    await component.changeStatus();
    expect(service.changeStatus).not.toHaveBeenCalled();
    expect(component.error()).toContain('motivo');
  });

  it('registra publicação sem trocar a etapa', async () => {
    service.publish.and.resolveTo({ ...event, id: 'e2', tipo: 'ATUALIZACAO',
      textoPublico: 'Diagnóstico iniciado.', publicada: true });
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.updateForm.setValue({
      textoPublico: 'Diagnóstico iniciado.', textoInterno: 'Conferir agregado.', publicada: true
    });
    await component.publishUpdate();
    expect(service.publish).toHaveBeenCalledWith('o1', jasmine.objectContaining({
      textoPublico: 'Diagnóstico iniciado.', textoInterno: 'Conferir agregado.', expectedVersion: 0
    }));
    expect(service.order).toHaveBeenCalledWith('o1');
  });

  it('atualiza a previsão com motivo, próxima ação e versão', async () => {
    const updated = { ...order, previsaoEm: '2026-09-21T18:00:00Z', versao: 1 };
    service.updateForecast.and.resolveTo(updated);
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.forecastForm.setValue({ previsaoEm: '2026-09-21T15:00',
      semPrevisao: false, motivoPublico: 'Atraso do fornecedor', proximaAcao: 'Cobrar nova posição.' });
    await component.updateForecast();
    expect(service.updateForecast).toHaveBeenCalledWith('o1', jasmine.objectContaining({
      motivoPublico: 'Atraso do fornecedor', proximaAcao: 'Cobrar nova posição.', expectedVersion: 0
    }));
    expect(component.selected()).toEqual(updated);
  });

  it('permite registrar ausência de nova previsão', async () => {
    const current = { ...order, previsaoEm: '2026-09-20T18:00:00Z' };
    service.orders.and.resolveTo({ items: [current], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    service.updateForecast.and.resolveTo({ ...current, previsaoEm: null, versao: 1 });
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.forecastForm.setValue({ previsaoEm: '', semPrevisao: true,
      motivoPublico: 'Peça incompatível', proximaAcao: 'Localizar fornecedor alternativo.' });
    await component.updateForecast();
    expect(service.updateForecast).toHaveBeenCalledWith('o1', jasmine.objectContaining({ previsao: null }));
  });

  it('diferencia atraso de espera para retirada', () => {
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    expect(component.deadlineLabel({ ...order, atrasada: true })).toContain('ultrapassada');
    expect(component.deadlineLabel({ ...order, aguardandoRetirada: true })).toContain('retirada');
  });

  it('retorno usa o responsável atual sem copiar dados do atendimento anterior', async () => {
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load();
    const closed = { ...order, status: 'ENTREGUE' as const };
    component.selected.set(closed);
    registrations.customer.and.resolveTo({ ...customer, id: 'c2' });
    registrations.vehicle.and.resolveTo({ ...vehicle, clienteId: 'c2' });
    await component.returnVisit(closed);
    expect(component.selected()).toBeNull();
    expect(component.form.controls.clienteId.value).toBe('c2');
    expect(component.form.controls.veiculoId.value).toBe('v1');
    expect(component.form.controls.relatoInicial.value).toBe('');
    expect(component.form.controls.kmEntrada.value).toBe('');
    expect(service.create).not.toHaveBeenCalled();
    expect(registrations.customer).toHaveBeenCalledWith('c2');
  });

  it('não preenche vínculo antigo quando veículo não está mais disponível', async () => {
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); const closed = { ...order, status: 'CANCELADO' as const }; component.selected.set(closed);
    registrations.vehicle.and.rejectWith(new HttpErrorResponse({ status: 404 }));
    await component.returnVisit(closed);
    expect(component.form.controls.clienteId.value).toBe('');
    expect(component.form.controls.veiculoId.value).toBe('');
    expect(component.selected()).toEqual(closed);
    expect(component.error()).toContain('indisponível');
    expect(component.success()).toBe('');
  });

  for (const vehicleOutsidePage of [true, false]) {
    it(`prepara retorno fora da primeira página de clientes (veículo fora: ${vehicleOutsidePage})`, async () => {
      const firstCustomers = Array.from({ length: 100 }, (_, index) => ({ ...customer, id: `other-c-${index}` }));
      const firstVehicles = Array.from({ length: 100 }, (_, index) => ({ ...vehicle, id: `other-v-${index}` }));
      registrations.customers.and.resolveTo({ items: firstCustomers, page: 0, size: 100, totalElements: 101, totalPages: 2 });
      registrations.vehicles.and.resolveTo({ items: vehicleOutsidePage ? firstVehicles : [vehicle], page: 0, size: 100, totalElements: 101, totalPages: 2 });
      const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
      await component.load(); const closed = { ...order, status: 'ENTREGUE' as const }; component.selected.set(closed);
      await component.returnVisit(closed);
      expect(registrations.vehicle).toHaveBeenCalledOnceWith('v1');
      expect(registrations.customer).toHaveBeenCalledOnceWith('c1');
      expect(component.form.controls.veiculoId.value).toBe('v1');
      expect(component.form.controls.clienteId.value).toBe('c1');
      expect(component.customers()).toContain(customer);
      expect(component.availableVehicles()).toContain(vehicle);
      expect(component.form.controls.relatoInicial.value).toBe('');
      expect(component.form.controls.kmEntrada.value).toBe('');
      expect(service.create).not.toHaveBeenCalled();
    });
  }

  it('não prepara retorno para responsável inativo', async () => {
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); const closed = { ...order, status: 'ENTREGUE' as const }; component.selected.set(closed);
    registrations.customer.and.resolveTo({ ...customer, ativo: false });
    await component.returnVisit(closed);
    expect(component.selected()).toEqual(closed);
    expect(component.error()).toContain('inativo');
    expect(service.create).not.toHaveBeenCalled();
  });

  it('recebe encerramento preservando diretório e sem sobrescrever outra seleção', async () => {
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.selected.set({ ...order, id: 'outra' });
    await component.orderClosed({ ...order, status: 'ENTREGUE' });
    expect(component.selected()?.id).toBe('outra');
    expect(component.orders()[0].status).toBe('ENTREGUE');
    expect(component.deadlineLabel(component.orders()[0])).toBe('Encerrada · histórico interno');
  });

  it('preserva o formulário no navegador quando a confirmação falha', async () => {
    service.saveInspection.and.rejectWith(new Error('offline'));
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load();
    component.inspectionForm.patchValue({ quilometragem: '48211', combustivel: 'METADE',
      avarias: 'Risco no para-choque' });
    await component.confirmInspection();
    const saved = JSON.parse(sessionStorage.getItem('vistoria:o1')!);
    expect(saved.quilometragem).toBe(48211);
    expect(saved.avarias).toContain('Risco');
    expect(service.confirmInspection).not.toHaveBeenCalled();
    sessionStorage.removeItem('vistoria:o1');
  });

  it('registra correção em nova versão com motivo', async () => {
    service.inspection.and.resolveTo([{ id: 'i1', numeroVersao: 1, estado: 'CONFIRMADA',
      checklist: { quilometragem: 48210, combustivel: 'METADE', objetos: '', avarias: '',
        observacoes: '', fotos: {} }, motivoCorrecao: null, createdAt: order.createdAt, updatedAt: order.updatedAt }]);
    service.correctInspection.and.resolveTo({ id: 'i2', numeroVersao: 2, estado: 'CONFIRMADA',
      checklist: { quilometragem: 48211, combustivel: 'METADE', objetos: '', avarias: '',
        observacoes: '', fotos: {} }, motivoCorrecao: 'Conferência no painel',
      createdAt: order.createdAt, updatedAt: order.updatedAt });
    const component = TestBed.createComponent(ServiceOrderPageComponent).componentInstance;
    await component.load(); component.beginCorrection();
    component.inspectionForm.controls.quilometragem.setValue('48211');
    component.correctionReason.setValue('Conferência no painel');
    await component.correctInspection();
    expect(service.correctInspection).toHaveBeenCalledWith('o1', 0, 'Conferência no painel',
      jasmine.objectContaining({ quilometragem: 48211 }));
  });
});
