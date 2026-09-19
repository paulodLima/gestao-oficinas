import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('obtém CSRF antes de enviar credenciais e guarda somente o perfil', async () => {
    const operation = service.login('dono@example.test', 'senha-de-teste');
    http.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    await Promise.resolve();
    const login = http.expectOne('/api/auth/login');
    expect(login.request.headers.get('X-CSRF-TOKEN')).toBe('csrf');
    const owner = { id: '1', nome: 'Dono', email: 'dono@example.test', oficina: { id: '2', nome: 'Oficina' } };
    login.flush(owner);
    await operation;
    expect(service.owner()).toEqual(owner);
  });
  it('não envia credenciais se o bootstrap CSRF falhar', async () => {
    const operation = service.login('dono@example.test', 'senha-de-teste');
    const assertion = expectAsync(operation).toBeRejected();
    http.expectOne('/api/auth/csrf').flush({}, { status: 503, statusText: 'Unavailable' });
    await assertion;
    http.expectNone('/api/auth/login');
    expect(service.owner()).toBeNull();
  });
});
