import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { PortalAccessComponent } from './portal-access.component';

describe('PortalAccessComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PortalAccessComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('solicita código com resposta genérica sem expor cadastro', async () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    component.slug = ' oficina-central ';
    component.plate = ' BRA-1E23 ';
    const operation = component.request();
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await Promise.resolve();
    await Promise.resolve();
    const request = http.expectOne('/api/portal/acesso/codigo');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('csrf');
    expect(request.request.body).toEqual({ oficinaSlug: 'oficina-central', placa: 'BRA-1E23' });
    request.flush({ desafioId: 'challenge-1' });
    await operation;
    expect(component.sent()).toBeTrue();
    expect(component.message()).toContain('Se houver');
  });

  it('carrega somente veículos autorizados e permite selecionar o serviço', async () => {
    const component = TestBed.createComponent(PortalAccessComponent).componentInstance;
    component.id = 'challenge-1';
    component.code = '123456';
    const operation = component.confirm();
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/portal/acesso/validacao').flush(null);
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/portal/veiculos').flush([
      { id: 'v1', placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross' },
      { id: 'v2', placa: 'ABC1234', veiculo: 'Fiat Uno' }
    ]);
    await Promise.resolve();
    await Promise.resolve();
    const current = http.expectOne(request => request.url === '/api/portal/servico-atual');
    expect(current.request.params.get('veiculoId')).toBe('v1');
    current.flush({ servico: { id: 'os1', numero: 1, status: 'EM_DIAGNOSTICO', previsaoEm: '',
      placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross', oficina: 'Central' } });
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/portal/ordens-servico/os1/fotos').flush([]);
    await operation;
    expect(component.authenticated()).toBeTrue();
    expect(component.vehicles().length).toBe(2);
    expect(component.selectedVehicleId).toBe('v1');
  });
});
