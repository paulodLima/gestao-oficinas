import { TestBed } from '@angular/core/testing';
import { NotificationPageComponent } from './notification-page.component';
import { Notice, NoticePage, NotificationService } from './notification.service';

describe('NotificationPageComponent', () => {
  let service: jasmine.SpyObj<NotificationService>;
  const notice: Notice = { id: '1', ordemServicoId: 'os-1', evento: 'ORDEM_ABERTA', titulo: 'Ordem aberta',
    mensagem: 'OS-1', createdAt: '2026-09-25T12:00:00Z', lida: false, emailEstado: 'FALHOU',
    tentativas: 5, proximaTentativaEm: null, ultimoErro: 'EMAIL_INDISPONIVEL' };
  const page: NoticePage = { items: [notice], page: 0, size: 20, totalElements: 1, totalPages: 1 };
  beforeEach(() => {
    service = jasmine.createSpyObj<NotificationService>('NotificationService', ['list', 'read', 'retry']);
    service.list.and.resolveTo(page); service.read.and.resolveTo(); service.retry.and.resolveTo();
    TestBed.configureTestingModule({ imports: [NotificationPageComponent], providers: [{ provide: NotificationService, useValue: service }] });
  });
  it('shows failures, a retry action and unread state', async () => {
    const fixture = TestBed.createComponent(NotificationPageComponent);
    fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Falha no envio');
    expect(fixture.nativeElement.querySelector('[aria-label="Reenviar e-mail: Ordem aberta"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Não lido');
  });
  it('never reports a requested retry as a delivered email', async () => {
    const fixture = TestBed.createComponent(NotificationPageComponent);
    await fixture.componentInstance.act(notice, true);
    expect(service.retry).toHaveBeenCalledOnceWith('1');
    expect(fixture.componentInstance.feedback()).toContain('ainda não foi enviado');
  });
  it('shows errors and releases the action after failure', async () => {
    service.retry.and.rejectWith(new Error('unavailable'));
    const fixture = TestBed.createComponent(NotificationPageComponent);
    await fixture.componentInstance.act(notice, true);
    expect(fixture.componentInstance.error()).toContain('Não foi possível');
    expect(fixture.componentInstance.feedback()).toBe('');
    expect(fixture.componentInstance.busy()).toBeNull();
  });
  it('ignores stale list responses after changing filters', async () => {
    let resolveOld!: (value: NoticePage) => void;
    service.list.and.returnValues(new Promise(resolve => resolveOld = resolve), Promise.resolve({ ...page, items: [] }));
    const component = TestBed.createComponent(NotificationPageComponent).componentInstance;
    const old = component.load(); component.unread.set(true); await component.load();
    resolveOld(page); await old;
    expect(component.data()?.items).toEqual([]);
  });
  it('returns to the previous page when the last unread notice is read', async () => {
    const component = TestBed.createComponent(NotificationPageComponent).componentInstance;
    component.unread.set(true); component.data.set({ ...page, page: 2 });
    await component.act(notice, false);
    expect(service.list).toHaveBeenCalledWith(1, true);
  });
  it('clears the previous filter after failure and retries from page zero', async () => {
    const component = TestBed.createComponent(NotificationPageComponent).componentInstance;
    component.data.set({ ...page, page: 2, items: [{ ...notice, lida: true }] });
    service.list.and.rejectWith(new Error('unavailable'));
    await component.filter(true);
    expect(component.data()).toBeNull();
    expect(component.error()).toContain('Não foi possível');
    expect(component.unread()).toBeTrue();
    service.list.and.resolveTo(page);
    await component.load(component.data()?.page ?? 0);
    expect(service.list).toHaveBeenCalledWith(0, true);
    expect(component.data()?.page).toBe(0);
    expect(component.error()).toBe('');
  });
});
