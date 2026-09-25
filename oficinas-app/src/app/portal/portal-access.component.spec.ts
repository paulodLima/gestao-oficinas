import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, NavigationEnd, Router, convertToParamMap } from '@angular/router';
import { Subject } from 'rxjs';
import { PortalAccessComponent } from './portal-access.component';
import { Location } from '@angular/common';

describe('PortalAccessComponent', () => {
  let http: HttpTestingController;
  let navigation: Subject<NavigationEnd>;

  beforeEach(async () => {
    navigation = new Subject<NavigationEnd>();
    await TestBed.configureTestingModule({
      imports: [PortalAccessComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { events: navigation } },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('troca token explicitamente vazio sem restaurar autorização antiga', async () => {
    TestBed.inject(ActivatedRoute).snapshot.fragment = 'token=';
    Object.defineProperty(TestBed.inject(ActivatedRoute).snapshot, 'queryParamMap', {
      value: convertToParamMap({ token: 'must-not-be-used' })
    });
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    const operation = component.ngOnInit();
    await tickRequests();
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await tickRequests();
    const exchange = http.expectOne('/api/portal/acesso/link');
    expect(exchange.request.body).toEqual({ token: '' });
    exchange.flush({}, { status: 400, statusText: 'Bad Request' });
    await operation;
    http.expectNone('/api/portal/veiculos');
    expect(component.authenticated()).toBeFalse();
  });

  for (const legacy of [false, true]) {
    it(`troca token ${legacy ? 'legado' : 'do fragmento'} removendo-o antes da chamada`, async () => {
      TestBed.inject(ActivatedRoute).snapshot.fragment = legacy ? null : 'token=synthetic-token';
      Object.defineProperty(TestBed.inject(ActivatedRoute).snapshot, 'queryParamMap', {
        value: convertToParamMap(legacy ? { token: 'synthetic-token' } : {})
      });
      const clean = spyOn(TestBed.inject(Location), 'replaceState');
      const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
      const operation = component.ngOnInit();
      expect(clean).toHaveBeenCalledWith('/acompanhar');
      await tickRequests();
      http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
      await tickRequests();
      const exchange = http.expectOne('/api/portal/acesso/link');
      expect(exchange.request.body).toEqual({ token: 'synthetic-token' });
      exchange.flush({ detail: 'expired' }, { status: 400, statusText: 'Bad Request' });
      await operation;
      expect(component.authenticated()).toBeFalse();
      expect(component.message()).toContain('expirou ou foi revogado');
    });
  }

  it('restaura sessão HTTP sem guardar ou reenviar token', async () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    const operation = component.ngOnInit();
    http.expectOne('/api/portal/veiculos').flush([]);
    await tickRequests();
    http.expectOne('/api/portal/servico-atual').flush({ oficina: { nome: 'Oficina' }, servico: null });
    await operation;
    expect(component.authenticated()).toBeTrue();
    expect(component.office()?.nome).toBe('Oficina');
  });

  it('mantém formulário de acesso para visitante sem sessão', async () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    const operation = component.ngOnInit();
    http.expectOne('/api/portal/veiculos').flush({}, { status: 401, statusText: 'Unauthorized' });
    await operation;
    expect(component.authenticated()).toBeFalse();
    expect(component.message()).toBe('');
  });

  it('descarta carga atrasada anterior ao receber outro link na mesma aba', async () => {
    const location = TestBed.inject(Location);
    spyOn(location, 'replaceState');
    spyOn(location, 'path').and.returnValue('/acompanhar#token=next-link');
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    const opening = component.ngOnInit();
    http.expectOne('/api/portal/veiculos').flush([]);
    await tickRequests();
    const previous = http.expectOne('/api/portal/servico-atual');
    navigation.next(new NavigationEnd(2, '/acompanhar#token=next-link', '/acompanhar#token=next-link'));
    expect(component.authenticated()).toBeFalse();
    expect(component.service()).toBeNull();
    await tickRequests();
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await tickRequests();
    http.expectOne('/api/portal/acesso/link').flush(null);
    await tickRequests();
    http.expectOne('/api/portal/servico-atual').flush({ oficina: { nome: 'Nova oficina' }, servico: null });
    await tickRequests();
    previous.flush({ oficina: { nome: 'Antiga oficina' }, servico: { id: 'old-order' } });
    await opening;
    expect(component.office()?.nome).toBe('Nova oficina');
    expect(component.service()).toBeNull();
  });

  it('serializa trocas de token e ignora autenticação anterior atrasada', async () => {
    const location = TestBed.inject(Location);
    spyOn(location, 'replaceState');
    spyOn(location, 'path').and.returnValue('/acompanhar#token=second-link');
    TestBed.inject(ActivatedRoute).snapshot.fragment = 'token=first-link';
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    const opening = component.ngOnInit();
    await tickRequests();
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await tickRequests();
    const first = http.expectOne('/api/portal/acesso/link');
    navigation.next(new NavigationEnd(2, '/acompanhar#token=second-link', '/acompanhar#token=second-link'));
    await tickRequests();
    http.expectNone('/api/auth/csrf');
    first.flush(null);
    await opening;
    await tickRequests();
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await tickRequests();
    const second = http.expectOne('/api/portal/acesso/link');
    expect(second.request.body).toEqual({ token: 'second-link' });
    second.flush(null);
    await tickRequests();
    http.expectOne('/api/portal/servico-atual').flush({ oficina: { nome: 'Nova oficina' }, servico: null });
    await tickRequests();
    expect(component.office()?.nome).toBe('Nova oficina');
  });

  it('solicita código com resposta genérica sem expor cadastro', async () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    component.slug = ' oficina-central ';
    component.plate = ' BRA-1E23 ';
    const operation = component.request();
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await tickRequests();
    const request = http.expectOne('/api/portal/acesso/codigo');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('csrf');
    expect(request.request.body).toEqual({ oficinaSlug: 'oficina-central', placa: 'BRA-1E23' });
    request.flush({ desafioId: 'challenge-1' });
    await operation;
    expect(component.sent()).toBeTrue();
    expect(component.message()).toContain('Se houver');
  });

  it('carrega serviço, fotos e linha do tempo do veículo autorizado', async () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    component.id = 'challenge-1';
    component.code = '123456';
    const operation = component.confirm();
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await tickRequests();
    http.expectOne('/api/portal/acesso/validacao').flush(null);
    await tickRequests();
    http.expectOne('/api/portal/veiculos').flush([
      { id: 'v1', placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross' },
      { id: 'v2', placa: 'ABC1234', veiculo: 'Fiat Uno' }
    ]);
    await tickRequests();
    const current = http.expectOne(request => request.url === '/api/portal/servico-atual');
    expect(current.request.params.get('veiculoId')).toBe('v1');
    current.flush({ oficina: { nome: 'Central', telefone: '6133334444', email: 'oi@central.test' }, servico: {
      id: 'os1', numero: 1, status: 'EM_DIAGNOSTICO', previsaoEm: null,
      placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross', pendencia: null,
      motivoPrevisao: null, proximaAcao: null, ultimaAtualizacao: '2026-09-25T12:00:00Z'
    } });
    await tickRequests();
    http.expectOne('/api/portal/ordens-servico/os1/fotos').flush([]);
    http.expectOne('/api/portal/ordens-servico/os1/atualizacoes').flush([]);
    await operation;
    expect(component.authenticated()).toBeTrue();
    expect(component.office()?.nome).toBe('Central');
    expect(component.selectedVehicleId).toBe('v1');
  });

  it('ordena fotos, preserva etapas anteriores e filtra sem alterar a coleção', () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    component.photos.set([
      { id: 'nova', etapa: 'EM_MANUTENCAO', legenda: 'Motor', createdAt: '2026-09-25T12:00:00Z' },
      { id: 'antiga', etapa: 'RECEBIDO', legenda: 'Entrada', createdAt: '2026-09-24T12:00:00Z' },
      { id: 'diagnostico', etapa: 'EM_DIAGNOSTICO', legenda: 'Diagnóstico', createdAt: '2026-09-24T18:00:00Z' }
    ]);

    expect(component.filteredPhotos().map(photo => photo.id)).toEqual(['antiga', 'diagnostico', 'nova']);
    component.setStage('RECEBIDO');
    expect(component.filteredPhotos().map(photo => photo.id)).toEqual(['antiga']);
    expect(component.photos().length).toBe(3);
  });

  it('navega circularmente na visualização ampliada', () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    component.photos.set([
      { id: 'a', etapa: 'RECEBIDO', legenda: null, createdAt: '2026-09-24T12:00:00Z' },
      { id: 'b', etapa: 'EM_DIAGNOSTICO', legenda: null, createdAt: '2026-09-25T12:00:00Z' }
    ]);
    component.openPhoto('a');
    component.nextPhoto();
    expect(component.activePhoto()?.id).toBe('b');
    component.nextPhoto();
    expect(component.activePhoto()?.id).toBe('a');
    component.previousPhoto();
    expect(component.activePhoto()?.id).toBe('b');
  });

  it('encerra o estado autenticado quando a API informa sessão expirada', async () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    component.authenticated.set(true);
    const operation = component.loadService('v1');
    http.expectOne(request => request.url === '/api/portal/servico-atual').flush(
      { codigo: 'ACESSO_EXPIRADO' }, { status: 401, statusText: 'Unauthorized' });
    await operation;
    expect(component.authenticated()).toBeFalse();
    expect(component.message()).toContain('acesso expirou');
  });

  async function tickRequests() {
    await Promise.resolve();
    await Promise.resolve();
  }
});
