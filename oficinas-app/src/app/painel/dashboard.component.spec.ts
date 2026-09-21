import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { DashboardCard, DashboardService, DashboardSnapshot, durationLabel, groupCards } from './dashboard.service';

const snapshot = (total: number): DashboardSnapshot => ({ indicadores: { ativas: total, atrasadas: 0, aprovacoes: 0, pecas: 0, prontas: 0 },
  ordens: { items: [], totalElements: total, totalPages: 1, page: 0, size: 20 }, fuso: 'America/Sao_Paulo', verificadoEm: '2026-01-01T12:00:00Z' });
describe('DashboardComponent', () => {
  let service: jasmine.SpyObj<DashboardService>;
  beforeEach(() => {
    service = jasmine.createSpyObj('DashboardService', ['get']);
    TestBed.configureTestingModule({ imports: [DashboardComponent], providers: [provideRouter([]), { provide: DashboardService, useValue: service }] });
  });
  it('ignora respostas antigas que chegam depois do filtro novo', async () => {
    let resolveOld!: (value: DashboardSnapshot) => void;
    service.get.and.returnValues(new Promise(resolve => resolveOld = resolve), Promise.resolve(snapshot(2)));
    const component = TestBed.createComponent(DashboardComponent).componentInstance;
    const first = component.fetch(); await component.fetch(); resolveOld(snapshot(9)); await first;
    expect(component.snapshot()?.indicadores.ativas).toBe(2); expect(component.loading()).toBeFalse();
  });
  it('remove dados antigos e informa uma falha de atualização', async () => {
    service.get.and.rejectWith(new Error('offline'));
    const component = TestBed.createComponent(DashboardComponent).componentInstance;
    component.snapshot.set(snapshot(3)); await component.fetch();
    expect(component.snapshot()).toBeNull(); expect(component.error()).toContain('Não foi possível');
  });
  it('agrupa cada cartão na mesma etapa sem duplicar', () => {
    const cards = [{ id: 'a', status: 'RECEBIDO' }, { id: 'b', status: 'PINTURA' }] as DashboardCard[];
    const groups = groupCards(cards);
    expect(groups.flatMap(group => group.cards)).toEqual(cards);
    expect(groups.find(group => group.value === 'PINTURA')?.cards[0].id).toBe('b');
  });
  it('formata durações sem apresentar valores negativos', () => {
    expect(durationLabel(-1)).toBe('menos de 1 min'); expect(durationLabel(3600)).toBe('1 h 0 min');
    expect(durationLabel(90000)).toBe('1 d 1 h');
  });
  it('atualizar ou trocar visualização não aplica filtros ainda em edição', async () => {
    service.get.and.resolveTo(snapshot(2));
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const component = TestBed.createComponent(DashboardComponent).componentInstance;
    component.filters.controls.q.setValue('rascunho'); component.page.set(1);
    await component.fetch();
    expect(service.get).toHaveBeenCalledWith(jasmine.objectContaining({ q: '' }), 1);
    component.changeView('quadro');
    expect(navigate).toHaveBeenCalledWith([], jasmine.objectContaining({ queryParams: { view: 'quadro' }, queryParamsHandling: 'merge' }));
    component.changePage(2);
    expect(navigate.calls.mostRecent().args[1]?.queryParams?.['q']).toBe('');
  });
});
