import { test, expect } from '@playwright/test';

test('portal mantém fotos antigas após mudança de status e permite filtrar e ampliar', async ({ page }, testInfo) => {
  let status = 'EM_DIAGNOSTICO';
  const photos = [
    { id: 'foto-entrada', etapa: 'RECEBIDO', legenda: 'Chegada do veículo', createdAt: '2026-09-24T12:00:00Z' },
    { id: 'foto-diagnostico', etapa: 'EM_DIAGNOSTICO', legenda: 'Inspeção do motor', createdAt: '2026-09-25T12:00:00Z' }
  ];
  const pixel = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64');

  await page.route('**/api/**', route => {
    const url = new URL(route.request().url());
    if (url.pathname === '/api/auth/csrf') return route.fulfill({ json: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } });
    if (url.pathname === '/api/portal/acesso/link') return route.fulfill({ status: 204 });
    if (url.pathname === '/api/portal/veiculos') return route.fulfill({ json: [] });
    if (url.pathname === '/api/portal/servico-atual') return route.fulfill({ json: {
      oficina: { nome: 'Oficina Horizonte', telefone: '(61) 3333-4444', email: 'contato@horizonte.test' },
      servico: { id: 'os-11', numero: 11, status, previsaoEm: '2026-09-28T18:00:00Z',
        placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross', pendencia: null,
        motivoPrevisao: null, proximaAcao: null, ultimaAtualizacao: '2026-09-25T12:00:00Z' }
    } });
    if (url.pathname.endsWith('/fotos')) return route.fulfill({ json: photos });
    if (url.pathname.endsWith('/atualizacoes')) return route.fulfill({ json: [
      { id: 'u1', tipo: 'ATUALIZACAO', statusAnterior: null, statusNovo: null,
        texto: 'Diagnóstico compartilhado com o cliente.', createdAt: '2026-09-25T12:00:00Z' }
    ] });
    if (url.pathname.endsWith('/adicionais')) return route.fulfill({ json: [] });
    if (url.pathname.includes('/fotos/')) return route.fulfill({ contentType: 'image/png', body: pixel });
    return route.fulfill({ status: 404, json: {} });
  });

  await page.goto('/acompanhar?token=token-seguro-da-ordem-de-servico-1234567890');
  await expect(page.getByRole('heading', { name: 'Volkswagen T-Cross' })).toBeVisible();
  await expect(page.getByText('Chegada do veículo', { exact: false })).toBeVisible();
  await expect(page.getByText('Inspeção do motor', { exact: false })).toBeVisible();

  await page.getByRole('button', { name: 'Recebido', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Ampliar Chegada do veículo' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ampliar Inspeção do motor' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Todas', exact: true }).click();
  await page.getByRole('button', { name: 'Ampliar Chegada do veículo' }).click();
  await expect(page.getByRole('dialog', { name: 'Foto ampliada do serviço' })).toBeVisible();
  await page.getByRole('button', { name: 'Próxima foto' }).click();
  await expect(page.getByRole('dialog')).toContainText('Inspeção do motor');
  await page.getByRole('button', { name: 'Fechar foto ampliada' }).click();

  status = 'EM_MANUTENCAO';
  await page.reload();
  await expect(page.getByText('Em manutenção')).toBeVisible();
  await expect(page.getByText('Chegada do veículo', { exact: false })).toBeVisible();
  await expect(page.getByText('Inspeção do motor', { exact: false })).toBeVisible();
  if (testInfo.project.name === 'mobile') await page.setViewportSize({ width: 320, height: 740 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
});

test('cliente decide grupo adicional com código e confirmação idempotente', async ({ page }) => {
  let decision: string | null = null;
  const additional = () => [{
    id: 'adicional-1', ordemServicoId: 'os-11', estado: decision ? 'DECIDIDA' : 'ENVIADA',
    versao: decision ? 2 : 1, createdAt: '2026-09-25T12:00:00Z', updatedAt: '2026-09-25T12:00:00Z',
    versoes: [{ id: 'versao-1', numero: 1, estado: 'ENVIADA', problema: 'Discos desgastados',
      justificativa: 'Substituição necessária para frenagem segura', previsaoProposta: '2026-09-29T18:00:00Z',
      impactoPrazo: 'Acrescenta um dia útil', motivoSubstituicao: null, total: 449.8,
      totalAprovado: decision ? 449.8 : 0, fotoIds: [],
      enviadaEm: '2026-09-25T12:00:00Z', substituidaEm: null,
      blocos: [{ id: 'grupo:freios', grupoDependencia: 'freios', total: 449.8,
        decisao: decision, decididaEm: decision ? '2026-09-25T12:10:00Z' : null,
        itens: [{ id: 'item-1', tipo: 'PECA', descricao: 'Kit de discos dianteiros',
          quantidade: 1, valorUnitario: 449.8, total: 449.8 }] }] }]
  }];

  await page.route('**/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/api/auth/csrf') return route.fulfill({ json: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } });
    if (path === '/api/portal/acesso/link') return route.fulfill({ status: 204 });
    if (path === '/api/portal/servico-atual') return route.fulfill({ json: { oficina: { nome: 'Oficina Horizonte', telefone: '', email: '' },
      servico: { id: 'os-11', numero: 11, status: 'AGUARDANDO_APROVACAO', previsaoEm: null,
        placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross', pendencia: 'Aguardando aprovação do cliente',
        motivoPrevisao: null, proximaAcao: null, ultimaAtualizacao: '2026-09-25T12:00:00Z' } } });
    if (path.endsWith('/fotos') || path.endsWith('/atualizacoes')) return route.fulfill({ json: [] });
    if (path.endsWith('/adicionais') && request.method() === 'GET') return route.fulfill({ json: additional() });
    if (path.endsWith('/codigo')) return route.fulfill({ json: { desafioId: 'challenge-1', expiraEm: '2026-09-25T12:20:00Z' } });
    if (path.endsWith('/decisoes')) {
      expect(request.headers()['idempotency-key']).toBeTruthy();
      expect(request.postDataJSON().decisoes).toEqual([{ bloco: 'grupo:freios', decisao: 'APROVADO' }]);
      decision = 'APROVADO';
      return route.fulfill({ json: additional()[0] });
    }
    return route.fulfill({ status: 404, json: {} });
  });

  await page.goto('/acompanhar?token=token-seguro-da-ordem-de-servico-1234567890');
  await expect(page.getByRole('heading', { name: 'Serviços adicionais' })).toBeVisible();
  await page.getByRole('button', { name: 'Aprovar', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar decisões selecionadas' }).click();
  await page.getByLabel('Código de 6 dígitos').fill('123456');
  await page.getByRole('button', { name: 'Registrar decisão definitiva' }).click();
  await expect(page.getByText('Aprovado em')).toBeVisible();
});
