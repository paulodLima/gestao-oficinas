import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReviewService } from './review.service';

describe('ReviewService', () => {
  let api: ReviewService; let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); api = TestBed.inject(ReviewService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('envia credencial somente no corpo com CSRF', async () => {
    const pending = api.exchange('synthetic'); http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' }); await Promise.resolve();
    const request = http.expectOne('/api/portal/avaliacoes/acesso'); expect(request.request.body).toEqual({ token: 'synthetic' }); expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('csrf');
    request.flush(null); await pending;
  });
  it('pagina resultados restritos à oficina', async () => {
    const pending = api.list(2); const request = http.expectOne('/api/avaliacoes?page=2&size=20'); request.flush({ items: [], page: 2, totalPages: 3, totalElements: 41 });
    expect((await pending).page).toBe(2);
  });
});
