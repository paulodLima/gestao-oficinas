import { TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { ReviewPageComponent } from './review-page.component';
import { ReviewService, ReviewSummary } from './review.service';

const summary: ReviewSummary = {
  contexto: 'context-A',
  atendimento: { numero: 18, entregueEm: '2026-09-25T12:00:00Z', oficinaNome: 'Oficina', veiculo: 'Marca Modelo', telefone: null, email: null, googleUrl: 'https://g.page/r/Test/review' },
  atualizacoes: [], avaliacao: null, expiraEm: '2026-10-02T12:00:00Z', textoConsentimento: 'Publicação opcional'
};
describe('ReviewPageComponent', () => {
  let api: jasmine.SpyObj<ReviewService>;
  let navigation: Subject<NavigationEnd>;
  beforeEach(() => {
    api = jasmine.createSpyObj('ReviewService', ['summary', 'exchange', 'submit']);
    api.summary.and.resolveTo(summary); api.exchange.and.resolveTo();
    api.submit.and.callFake(async input => ({ ...input, createdAt: '2026-09-25T12:10:00Z' }));
    navigation = new Subject<NavigationEnd>();
    TestBed.configureTestingModule({ imports: [ReviewPageComponent], providers: [
      { provide: ReviewService, useValue: api }, { provide: Router, useValue: { events: navigation } },
      { provide: ActivatedRoute, useValue: { snapshot: { fragment: null } } }
    ] });
  });
  it('remove o token antes da troca e restaura apenas o resumo restrito', async () => {
    TestBed.inject(ActivatedRoute).snapshot.fragment = 'token=synthetic';
    const replace = spyOn(TestBed.inject(Location), 'replaceState');
    const component = TestBed.createComponent(ReviewPageComponent).componentInstance;
    const loading = component.ngOnInit(); expect(replace).toHaveBeenCalledWith('/avaliar');
    await loading; expect(api.exchange).toHaveBeenCalledOnceWith('synthetic'); expect(component.summary()).toEqual(summary);
    expect(component.consent).toBeFalse(); expect(component.score).toBe(0);
  });
  it('valida a nota, preserva consentimento privado e impede segundo envio', async () => {
    const component = TestBed.createComponent(ReviewPageComponent).componentInstance; await component.ngOnInit();
    await component.submit(); expect(api.submit).not.toHaveBeenCalled();
    component.score = 1; component.comment = ' Precisa melhorar '; await component.submit();
    expect(api.submit).toHaveBeenCalledOnceWith({ contexto: 'context-A', nota: 1, comentario: 'Precisa melhorar', consentimentoPublicacao: false });
    await component.submit(); expect(api.submit).toHaveBeenCalledTimes(1);
  });
  it('exibe o mesmo link Google antes e depois de nota baixa', async () => {
    const fixture = TestBed.createComponent(ReviewPageComponent); fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    const before: HTMLAnchorElement = fixture.nativeElement.querySelector('.google a');
    expect(before.href).toBe(summary.atendimento.googleUrl!); expect(before.rel).toContain('noreferrer');
    fixture.componentInstance.score = 1; await fixture.componentInstance.submit(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.google a').href).toBe(before.href);
    expect(fixture.nativeElement.textContent).toContain('Você não autorizou a publicação');
  });
  it('apaga o resumo quando a sessão expira durante o envio', async () => {
    const component = TestBed.createComponent(ReviewPageComponent).componentInstance; await component.ngOnInit();
    api.submit.and.rejectWith(new HttpErrorResponse({ status: 401, error: { detail: 'Convite expirado' } }));
    component.score = 4; await component.submit(); expect(component.summary()).toBeNull(); expect(component.message()).toBe('Convite expirado');
  });
  it('não restaura o resumo anterior depois de receber convite inválido', async () => {
    const component = TestBed.createComponent(ReviewPageComponent).componentInstance; await component.ngOnInit();
    api.exchange.and.rejectWith(new HttpErrorResponse({ status: 401 }));
    await component.load('invalid'); expect(component.summary()).toBeNull(); expect(api.summary).toHaveBeenCalledTimes(1);
  });
  it('serializa trocas na mesma aba e ignora resposta atrasada', async () => {
    let release!: () => void;
    api.exchange.and.callFake(token => token === 'first' ? new Promise<void>(resolve => release = resolve) : Promise.resolve());
    const component = TestBed.createComponent(ReviewPageComponent).componentInstance;
    const first = component.load('first'); await Promise.resolve();
    const next = component.load('second'); expect(api.exchange).toHaveBeenCalledTimes(1);
    release(); await first; await next;
    expect(api.exchange.calls.allArgs()).toEqual([['first'], ['second']]); expect(api.summary).toHaveBeenCalledTimes(1);
  });
  it('não envia contra uma nova OS ao trocar convite com envio em andamento', async () => {
    let release!: (value: Awaited<ReturnType<ReviewService['submit']>>) => void;
    api.submit.and.returnValue(new Promise(resolve => release = resolve));
    const component = TestBed.createComponent(ReviewPageComponent).componentInstance; await component.ngOnInit();
    component.score = 4; const submitting = component.submit(); await Promise.resolve();
    const loading = component.load('next'); expect(api.exchange).not.toHaveBeenCalled();
    release({ nota: 4, comentario: null, consentimentoPublicacao: false, createdAt: 'now' }); await submitting; await loading;
    expect(api.exchange).toHaveBeenCalledOnceWith('next'); expect(component.summary()?.avaliacao).toBeNull();
  });
  it('remove formulário obsoleto ao detectar troca de contexto em outra aba', async () => {
    const component = TestBed.createComponent(ReviewPageComponent).componentInstance; await component.ngOnInit();
    api.submit.and.rejectWith(new HttpErrorResponse({ status: 409, error: { code: 'AVALIACAO_CONTEXTO_ALTERADO', detail: 'Reabra o convite.' } }));
    component.score = 5; component.consent = true; await component.submit();
    expect(api.submit).toHaveBeenCalledWith(jasmine.objectContaining({ contexto: 'context-A' }));
    expect(component.summary()).toBeNull(); expect(component.message()).toBe('Reabra o convite.');
  });
  for (const fragment of ['token=', 'token']) {
    it(`rejeita convite vazio na inicialização: ${fragment}`, async () => {
      TestBed.inject(ActivatedRoute).snapshot.fragment = fragment;
      api.exchange.and.rejectWith(new HttpErrorResponse({ status: 401 }));
      const component = TestBed.createComponent(ReviewPageComponent).componentInstance; await component.ngOnInit();
      expect(api.exchange).toHaveBeenCalledOnceWith(''); expect(api.summary).not.toHaveBeenCalled(); expect(component.summary()).toBeNull();
    });
  }
  it('rejeita token vazio na navegação da mesma instância e não restaura o anterior', async () => {
    const component = TestBed.createComponent(ReviewPageComponent).componentInstance; await component.ngOnInit();
    api.exchange.and.rejectWith(new HttpErrorResponse({ status: 401 }));
    spyOn(TestBed.inject(Location), 'path').and.returnValue('/avaliar#token=');
    navigation.next(new NavigationEnd(2, '/avaliar#token=', '/avaliar#token='));
    await Promise.resolve(); await Promise.resolve(); await Promise.resolve();
    expect(api.exchange).toHaveBeenCalledOnceWith(''); expect(api.summary).toHaveBeenCalledTimes(1); expect(component.summary()).toBeNull();
  });
});
