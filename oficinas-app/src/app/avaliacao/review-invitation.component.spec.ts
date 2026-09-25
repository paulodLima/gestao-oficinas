import { TestBed } from '@angular/core/testing';
import { ReviewInvitationComponent } from './review-invitation.component';
import { ReviewInvitation, ReviewService } from './review.service';

describe('ReviewInvitationComponent', () => {
  let api: jasmine.SpyObj<ReviewService>;
  beforeEach(() => {
    api = jasmine.createSpyObj('ReviewService', ['invitation', 'revoke']);
    api.invitation.and.resolveTo({ token: 'synthetic', expiraEm: '2026-10-02T12:00:00Z' }); api.revoke.and.resolveTo();
    TestBed.configureTestingModule({ imports: [ReviewInvitationComponent], providers: [{ provide: ReviewService, useValue: api }] });
  });
  it('não cria ao abrir a OS e usa fragmento sem guardar credencial', async () => {
    const fixture = TestBed.createComponent(ReviewInvitationComponent); fixture.componentRef.setInput('orderId', 'os-18'); fixture.detectChanges();
    expect(api.invitation).not.toHaveBeenCalled(); await fixture.componentInstance.create();
    expect(api.invitation).toHaveBeenCalledOnceWith('os-18'); expect(fixture.componentInstance.url()).toContain('/avaliar#token=synthetic');
  });
  it('exige confirmação para revogar e limpa o link', async () => {
    const fixture = TestBed.createComponent(ReviewInvitationComponent); fixture.componentRef.setInput('orderId', 'os-18'); fixture.detectChanges();
    const component = fixture.componentInstance; await component.create(); await component.revoke(); expect(api.revoke).not.toHaveBeenCalled();
    component.confirming.set(true); await component.revoke(); expect(api.revoke).toHaveBeenCalledOnceWith('os-18'); expect(component.url()).toBe('');
  });
  it('não mostra convite de outra OS recebido com atraso', async () => {
    let release!: (value: ReviewInvitation) => void; api.invitation.and.returnValue(new Promise(resolve => release = resolve));
    const fixture = TestBed.createComponent(ReviewInvitationComponent); fixture.componentRef.setInput('orderId', 'os-18'); fixture.detectChanges();
    const pending = fixture.componentInstance.create(); fixture.componentRef.setInput('orderId', 'os-19'); fixture.detectChanges();
    release({ token: 'old', expiraEm: '2026-10-02T12:00:00Z' }); await pending; expect(fixture.componentInstance.url()).toBe('');
  });
});
