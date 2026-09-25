import { TestBed } from '@angular/core/testing';
import { AdditionalDecisionComponent } from './additional-decision.component';
import { AdditionalDecisionService } from './additional-decision.service';

describe('Estado de erro dos adicionais no portal', () => {
  it('mostra erro com lista vazia e oferece recuperação', async () => {
    const service = { list: jasmine.createSpy().and.rejectWith(new Error('offline')) };
    TestBed.configureTestingModule({ providers: [{ provide: AdditionalDecisionService, useValue: service }] });
    const fixture = TestBed.createComponent(AdditionalDecisionComponent);
    fixture.componentInstance.orderId = 'os';
    await fixture.componentInstance.load(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Não foi possível');
    service.list.and.resolveTo([]);
    await fixture.componentInstance.load(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]')).toBeNull();
    expect(fixture.componentInstance.loadError()).toBeFalse();
  });
});
