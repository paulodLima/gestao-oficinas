import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CustomerVehicleService } from './customer-vehicle.service';

describe('CustomerVehicleService — consultas individuais', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }));
  afterEach(() => TestBed.inject(HttpTestingController).verify());
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
