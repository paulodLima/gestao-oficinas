import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(NotificationService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('lists paginated unread notifications without accepting a tenant', async () => {
    const pending = service.list(2, true);
    const request = http.expectOne('/api/notificacoes?page=2&size=20&naoLidas=true');
    expect(request.request.method).toBe('GET');
    request.flush({ items: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
    expect((await pending).page).toBe(2);
  });
  it('marks read with CSRF protection', async () => {
    const pending = service.read('notice-1');
    http.expectOne('/api/auth/csrf').flush({ token: 'safe-token', headerName: 'X-CSRF-TOKEN' });
    await Promise.resolve(); await Promise.resolve();
    const request = http.expectOne('/api/notificacoes/notice-1');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ lida: true });
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('safe-token');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await pending;
  });
  it('propagates retry refusal and preserves the event identity', async () => {
    const pending = service.retry('notice-1');
    const rejected = expectAsync(pending).toBeRejected();
    http.expectOne('/api/auth/csrf').flush({ token: 'safe-token', headerName: 'X-CSRF-TOKEN' });
    await Promise.resolve(); await Promise.resolve();
    const request = http.expectOne('/api/notificacoes/notice-1/reenvio');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('safe-token');
    expect(request.request.method).toBe('POST');
    request.flush({}, { status: 409, statusText: 'Conflict' });
    await rejected;
  });
});
