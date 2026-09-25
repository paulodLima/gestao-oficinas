import { ComponentFixture, TestBed, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { OrderShareComponent } from './order-share.component';
import { CustomerAccessLink, ServiceOrder, ServiceOrderService } from './service-order.service';

describe('OrderShareComponent', () => {
  let fixture: ComponentFixture<OrderShareComponent>;
  let component: OrderShareComponent;
  let service: jasmine.SpyObj<ServiceOrderService>;
  const order = { id: 'os-16', status: 'RECEBIDO', numero: 16 } as ServiceOrder;
  const link = () => ({ token: 'synthetic-token', expiraEm: new Date(Date.now() + 60_000).toISOString() });

  beforeEach(() => {
    service = jasmine.createSpyObj('ServiceOrderService', ['createCustomerAccess', 'revokeCustomerAccess']);
    service.createCustomerAccess.and.callFake(async () => link());
    service.revokeCustomerAccess.and.resolveTo();
    TestBed.configureTestingModule({ imports: [OrderShareComponent], providers: [
      { provide: ServiceOrderService, useValue: service }
    ] });
    fixture = TestBed.createComponent(OrderShareComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('order', order);
    fixture.detectChanges();
  });

  it('prepara link só por ação explícita e não registra envio ou entrega', async () => {
    expect(service.createCustomerAccess).not.toHaveBeenCalled();
    await component.prepare();
    fixture.detectChanges();
    expect(service.createCustomerAccess).toHaveBeenCalledOnceWith('os-16');
    expect(component.url()).toContain('/acompanhar#token=');
    const anchor = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    expect(anchor.target).toBe('_blank');
    expect(anchor.rel).toContain('noreferrer');
    const event = new Event('click', { cancelable: true });
    component.openWhatsApp(event);
    expect(event.defaultPrevented).toBeFalse();
    expect(component.notice()).toContain('não confirma envio nem entrega');
    expect(service.createCustomerAccess).toHaveBeenCalledTimes(1);
  });

  it('copia o link e só confirma a cópia após sucesso', async () => {
    const clipboard = spyOn(navigator.clipboard, 'writeText').and.resolveTo();
    await component.prepare();
    await component.copy();
    expect(clipboard).toHaveBeenCalledOnceWith(component.url());
    expect(component.notice()).toContain('Link copiado');
  });

  it('orienta cópia manual se a permissão for negada', async () => {
    spyOn(navigator.clipboard, 'writeText').and.rejectWith(new Error('Denied'));
    await component.prepare();
    await component.copy();
    expect(component.error()).toContain('copie manualmente');
    expect(component.notice()).toBe('');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input').readOnly).toBeTrue();
  });

  it('revoga mesmo sem possuir o token local e remove ações após revogar', async () => {
    await component.revoke();
    expect(service.revokeCustomerAccess).toHaveBeenCalledWith('os-16');
    await component.prepare();
    await component.revoke();
    fixture.detectChanges();
    expect(component.url()).toBe('');
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
  });

  it('não confirma revogação quando a API falha', async () => {
    service.revokeCustomerAccess.and.rejectWith(new Error('offline'));
    await component.prepare();
    await component.revoke();
    expect(component.url()).toBe('');
    expect(component.error()).toContain('pode continuar válido');
    expect(component.notice()).toBe('');
  });

  it('trata falha na geração sem exibir link anterior', async () => {
    await component.prepare();
    service.createCustomerAccess.and.rejectWith(new Error('offline'));
    await component.prepare();
    expect(component.url()).toBe('');
    expect(component.error()).toContain('Não foi possível preparar');
    expect(component.busy()).toBeFalse();
  });

  it('ignora respostas antigas ao trocar de OS, mesmo voltando à anterior', async () => {
    let finish!: (value: CustomerAccessLink) => void;
    service.createCustomerAccess.and.returnValue(new Promise(resolve => finish = resolve));
    const pending = component.prepare();
    fixture.componentRef.setInput('order', { ...order, id: 'os-17' });
    fixture.detectChanges();
    fixture.componentRef.setInput('order', order);
    fixture.detectChanges();
    finish(link());
    await pending;
    expect(component.url()).toBe('');
    expect(component.notice()).toBe('');
  });

  it('impede geração duplicada enquanto aguarda e ignora resposta após destruir', async () => {
    let finish!: (value: CustomerAccessLink) => void;
    service.createCustomerAccess.and.returnValue(new Promise(resolve => finish = resolve));
    const pending = component.prepare();
    await component.prepare();
    expect(service.createCustomerAccess).toHaveBeenCalledTimes(1);
    fixture.destroy();
    finish(link());
    await pending;
    expect(component.url()).toBe('');
  });

  it('limpa link ao encerrar e impede nova geração', async () => {
    await component.prepare();
    fixture.componentRef.setInput('order', { ...order, status: 'ENTREGUE' });
    fixture.detectChanges();
    await component.prepare();
    expect(component.url()).toBe('');
    expect(service.createCustomerAccess).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('OS encerrada');
  });

  it('retira ações ao expirar sem afirmar envio', fakeAsync(() => {
    void component.prepare();
    flushMicrotasks();
    tick(60_001);
    expect(component.url()).toBe('');
    const event = new Event('click', { cancelable: true });
    component.openWhatsApp(event);
    expect(event.defaultPrevented).toBeTrue();
    expect(component.notice()).toContain('expirou');
  }));
});
