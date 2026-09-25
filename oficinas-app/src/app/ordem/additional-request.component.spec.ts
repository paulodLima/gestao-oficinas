import { TestBed } from '@angular/core/testing';
import { ServiceOrder } from './service-order.service';
import { AdditionalRequest, AdditionalRequestService } from './additional-request.service';
import { AdditionalRequestComponent } from './additional-request.component';

describe('Solicitações de serviços adicionais', () => {
  const order: ServiceOrder = { id: 'o1', numero: 12, clienteId: 'c1', clienteNome: 'Ana',
    veiculoId: 'v1', placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross', relatoInicial: 'Ruído',
    entradaEm: '2026-09-25T10:00:00Z', kmEntrada: 1000, status: 'EM_DIAGNOSTICO',
    previsaoEm: null, atrasada: false, aguardandoRetirada: false, versao: 0,
    createdAt: '2026-09-25T10:00:00Z', updatedAt: '2026-09-25T10:00:00Z' };
  const request: AdditionalRequest = { id: 'a1', ordemServicoId: 'o1', estado: 'RASCUNHO',
    versao: 2, motivoCancelamento: null, createdAt: order.createdAt, updatedAt: order.updatedAt,
    versoes: [{ id: 'av1', numero: 1, estado: 'RASCUNHO', problema: 'Disco empenado',
      justificativa: 'Substituição necessária', previsaoProposta: null, impactoPrazo: 'Mais um dia',
      motivoSubstituicao: null, total: 100, fotoIds: [], enviadaEm: null, substituidaEm: null,
      createdAt: order.createdAt, updatedAt: order.updatedAt,
      itens: [{ id: 'i1', tipo: 'PECA', descricao: 'Disco', quantidade: 1,
        valorUnitario: 100, total: 100, grupoDependencia: null, ordem: 1 }] }] };
  let service: jasmine.SpyObj<AdditionalRequestService>;

  beforeEach(() => {
    service = jasmine.createSpyObj<AdditionalRequestService>('AdditionalRequestService',
      ['list', 'create', 'edit', 'send', 'replace', 'cancel']);
    service.list.and.resolveTo([request]);
    service.create.and.resolveTo(request);
    service.edit.and.resolveTo(request);
    service.send.and.resolveTo({ ...request, estado: 'ENVIADA', versao: 3 });
    service.replace.and.resolveTo(request);
    service.cancel.and.resolveTo({ ...request, estado: 'CANCELADA', versao: 3 });
    TestBed.configureTestingModule({ imports: [AdditionalRequestComponent],
      providers: [{ provide: AdditionalRequestService, useValue: service }] });
  });

  function component() {
    const value = TestBed.createComponent(AdditionalRequestComponent).componentInstance;
    value.order = order;
    return value;
  }

  it('carrega e acompanha o histórico da ordem', async () => {
    const value = component();
    await value.load();
    expect(service.list).toHaveBeenCalledWith('o1');
    expect(value.requests()).toEqual([request]);
    expect(value.currentVersion(request).numero).toBe(1);
  });

  it('calcula a prévia e normaliza o rascunho antes de salvar', async () => {
    const value = component();
    service.list.and.resolveTo([]);
    value.startNew();
    value.form.patchValue({ problema: '  Disco empenado ', justificativa: ' Troca necessária ',
      impactoPrazo: ' Mais um dia ' });
    value.items.at(0).patchValue({ descricao: ' Disco dianteiro ', quantidade: '1,125',
      valorUnitario: '19,99', grupoDependencia: '' });
    expect(value.previewTotal()).toBeCloseTo(22.48875, 5);
    await value.save();
    expect(service.create).toHaveBeenCalledWith('o1', jasmine.objectContaining({
      problema: 'Disco empenado', itens: [jasmine.objectContaining({ quantidade: 1.125, valorUnitario: 19.99 })]
    }));
    expect(value.success()).toContain('Rascunho');
  });

  it('envia com versão esperada e congela o conteúdo no servidor', async () => {
    const value = component();
    await value.load();
    await value.send(request);
    expect(service.send).toHaveBeenCalledWith('o1', 'a1', 2);
    expect(value.success()).toContain('congelada');
  });

  it('cria substituição preservando motivo e versão anterior', async () => {
    const sent = { ...request, estado: 'ENVIADA' as const };
    const value = component();
    value.requests.set([sent]);
    value.beginReplacement(sent);
    value.replacementReason.setValue('Fornecedor alterou a peça');
    await value.save();
    expect(service.replace).toHaveBeenCalledWith('o1', 'a1', jasmine.any(Object),
      'Fornecedor alterou a peça', 2);
    expect(value.success()).toContain('Nova versão');
  });
});
