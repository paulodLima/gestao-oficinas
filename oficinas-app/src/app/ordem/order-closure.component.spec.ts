import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { OrderClosureComponent } from './order-closure.component';
import { ClosureSummary, ServiceOrder, ServiceOrderService } from './service-order.service';

describe('OrderClosureComponent', () => {
  let fixture: ComponentFixture<OrderClosureComponent>;
  let component: OrderClosureComponent;
  let service: jasmine.SpyObj<ServiceOrderService>;
  const order = { id: 'os-17', status: 'PRONTO_PARA_RETIRADA', versao: 4 } as ServiceOrder;
  const summary: ClosureSummary = { versao: 4, pendencias: 1, encerramento: null };
  beforeEach(() => {
    service = jasmine.createSpyObj('ServiceOrderService', ['closureSummary', 'closeOrder']);
    service.closureSummary.and.resolveTo(summary);
    service.closeOrder.and.resolveTo({ ...order, status: 'ENTREGUE', versao: 5 });
    TestBed.configureTestingModule({ imports: [OrderClosureComponent], providers: [{ provide: ServiceOrderService, useValue: service }] });
    fixture = TestBed.createComponent(OrderClosureComponent); component = fixture.componentInstance;
    fixture.componentRef.setInput('order', order); fixture.detectChanges();
  });
  it('pronto permanece ativo e exige revisão e duas confirmações explícitas', async () => {
    expect(component.active()).toBeTrue();
    expect(service.closureSummary).not.toHaveBeenCalled();
    await component.confirm(); expect(service.closeOrder).not.toHaveBeenCalled();
    await component.review(); await component.confirm();
    expect(service.closeOrder).not.toHaveBeenCalled();
    component.form.controls.confirmado.setValue(true); await component.confirm();
    expect(component.error()).toContain('pendências');
    component.form.controls.cancelarPendencias.setValue(true);
    const emitted = spyOn(component.closed, 'emit');
    await component.confirm();
    expect(service.closeOrder).toHaveBeenCalledOnceWith('os-17', {
      tipo: 'ENTREGUE', motivo: '', confirmado: true, cancelarPendencias: true, expectedVersion: 4
    });
    expect(emitted).toHaveBeenCalledWith(jasmine.objectContaining({ status: 'ENTREGUE' }));
  });
  it('exige motivo no cancelamento e não envia ao voltar', async () => {
    await component.review(); component.form.patchValue({ tipo: 'CANCELADO', confirmado: true, cancelarPendencias: true });
    await component.confirm(); expect(service.closeOrder).not.toHaveBeenCalled();
    component.form.controls.motivo.setValue('Pedido do cliente'); component.back();
    await component.confirm(); expect(service.closeOrder).not.toHaveBeenCalled();
  });
  it('conflito exige nova revisão sem repetir automaticamente', async () => {
    service.closeOrder.and.rejectWith(new HttpErrorResponse({ status: 409, error: { detail: 'A ordem mudou' } }));
    await component.review(); component.form.patchValue({ confirmado: true, cancelarPendencias: true }); await component.confirm();
    expect(component.reviewing()).toBeFalse(); expect(component.error()).toBe('A ordem mudou');
    await component.confirm(); expect(service.closeOrder).toHaveBeenCalledTimes(1);
  });
  it('ignora resumo atrasado quando a OS selecionada muda', async () => {
    let resolve!: (value: ClosureSummary) => void;
    service.closureSummary.and.returnValue(new Promise(done => resolve = done));
    const pending = component.review();
    fixture.componentRef.setInput('order', { ...order, id: 'outra' }); fixture.detectChanges();
    resolve(summary); await pending;
    expect(component.summary()).toBeNull(); expect(component.reviewing()).toBeFalse();
  });
  it('não emite encerramento atrasado sobre outra OS', async () => {
    let resolve!: (value: ServiceOrder) => void;
    service.closeOrder.and.returnValue(new Promise(done => resolve = done));
    await component.review(); component.form.patchValue({ confirmado: true, cancelarPendencias: true });
    const emitted = spyOn(component.closed, 'emit'); const pending = component.confirm();
    fixture.componentRef.setInput('order', { ...order, id: 'outra' }); fixture.detectChanges();
    resolve({ ...order, status: 'ENTREGUE' }); await pending; expect(emitted).not.toHaveBeenCalled();
  });
  it('exibe encerramento e retorno sem permitir novo fechamento', async () => {
    fixture.componentRef.setInput('order', { ...order, status: 'CANCELADO' }); fixture.detectChanges();
    await fixture.whenStable(); fixture.detectChanges();
    expect(component.active()).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Abrir nova OS para este veículo');
    await component.confirm(); expect(service.closeOrder).not.toHaveBeenCalled();
  });
});
