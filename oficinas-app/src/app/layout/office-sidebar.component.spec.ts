import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { OfficeSidebarComponent } from './office-sidebar.component';
import { AuthService } from '../auth/auth.service';
import { ShopService } from '../oficina/shop.service';

describe('Saída da navegação da oficina', () => {
  const auth = { logout: jasmine.createSpy(), owner: signal(null) };
  beforeEach(() => {
    auth.logout.calls.reset(); auth.logout.and.resolveTo();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth },
      { provide: ShopService, useValue: { get: () => Promise.resolve(null) } }] });
    spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
  });
  it('encerra sessão e leva para o login', async () => {
    const component = TestBed.createComponent(OfficeSidebarComponent).componentInstance;
    await component.logout();
    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/entrar');
  });
  it('sessão já revogada também permite sair', async () => {
    auth.logout.and.rejectWith(new HttpErrorResponse({ status: 401 }));
    const component = TestBed.createComponent(OfficeSidebarComponent).componentInstance;
    await component.logout(); expect(component.error()).toBe('');
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/entrar');
  });
  it('falha de rede não afirma que a sessão foi encerrada', async () => {
    auth.logout.and.rejectWith(new HttpErrorResponse({ status: 0 }));
    const component = TestBed.createComponent(OfficeSidebarComponent).componentInstance;
    await component.logout(); expect(component.error()).toContain('Não foi possível sair');
    expect(component.busy()).toBeFalse();
    expect(TestBed.inject(Router).navigateByUrl).not.toHaveBeenCalled();
  });
});
