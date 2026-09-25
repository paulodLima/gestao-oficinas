import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CustomerVehicleService } from './customer-vehicle.service';

describe('CustomerVehicleService — consultas individuais', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }));
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  it('pesquisa CPF no corpo protegido por CSRF, nunca na URL', async () => {
    const result = TestBed.inject(CustomerVehicleService).customers('529.982.247-25');
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf-safe', headerName: 'X-CSRF-TOKEN' });
    await Promise.resolve();
    await Promise.resolve();
    const request = http.expectOne('/api/clientes/pesquisa');
    expect(request.request.method).toBe('POST');
    expect(request.request.urlWithParams).not.toContain('529');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-safe');
    expect(request.request.body).toEqual({ q: '529.982.247-25', page: 0, size: 100 });
    request.flush({ items: [], totalElements: 0 });
    await result;
  });
  it('consulta veículo pelo ID sem depender da paginação', async () => {
    const result = TestBed.inject(CustomerVehicleService).vehicle('vehicle-id');
    const request = TestBed.inject(HttpTestingController).expectOne('/api/veiculos/vehicle-id');
    expect(request.request.method).toBe('GET');
    request.flush({ id: 'vehicle-id', clienteId: 'current-customer' });
    expect((await result).clienteId).toBe('current-customer');
  });
  it('consulta responsável atual pelo ID sem depender da paginação', async () => {
    const result = TestBed.inject(CustomerVehicleService).customer('current-customer');
    const request = TestBed.inject(HttpTestingController).expectOne('/api/clientes/current-customer');
    expect(request.request.method).toBe('GET');
    request.flush({ id: 'current-customer', ativo: true });
    expect((await result).ativo).toBeTrue();
  });
});
