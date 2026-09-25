import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdditionalDecisionService, AdditionalRequest } from './additional-decision.service';

describe('AdditionalDecisionService', () => {
  let service: AdditionalDecisionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdditionalDecisionService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('envia decisão com código, versão e chave idempotente', async () => {
    const request = { id: 'a1', versao: 3 } as AdditionalRequest;
    const operation = service.confirm('os1', request, 'challenge', '123456',
      [{ bloco: 'grupo:motor', decisao: 'APROVADO' }], 'Pode executar', 'attempt-1');
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await Promise.resolve();
    await Promise.resolve();
    const confirmation = http.expectOne('/api/portal/ordens-servico/os1/adicionais/a1/decisoes');
    expect(confirmation.request.headers.get('X-CSRF-TOKEN')).toBe('csrf');
    expect(confirmation.request.headers.get('Idempotency-Key')).toBe('attempt-1');
    expect(confirmation.request.body).toEqual({ desafioId: 'challenge', codigo: '123456', versao: 3,
      decisoes: [{ bloco: 'grupo:motor', decisao: 'APROVADO' }], comentario: 'Pode executar' });
    confirmation.flush(request);
    await operation;
  });
});
