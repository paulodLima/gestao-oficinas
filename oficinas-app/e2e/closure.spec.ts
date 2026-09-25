import { test, expect, Page, BrowserContext } from '@playwright/test';

const initial = { id: 'os-17', numero: 17, clienteId: 'c1', clienteNome: 'Ana Teste',
  veiculoId: 'v1', placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross',
  relatoInicial: 'Ruído na suspensão dianteira.', entradaEm: '2026-09-25T10:00:00Z',
  kmEntrada: 12000, status: 'EM_TESTES', previsaoEm: null, atrasada: false,
  aguardandoRetirada: false, versao: 0, createdAt: '2026-09-25T10:00:00Z', updatedAt: '2026-09-25T10:00:00Z' };

async function mock(context: BrowserContext) {
  let order = { ...initial };
  let current: typeof order | null = order;
  let closure: Record<string, unknown> | null = null;
  let oldLink: Page | undefined;
  const mutations: string[] = [];
  await context.route('**/api/**', route => {
    const request = route.request(), path = new URL(request.url()).pathname;
    if (request.method() !== 'GET') mutations.push(path);
    if (path === '/api/auth/me') return route.fulfill({ json: { id: 'owner', nome: 'Dono', oficina: { id: 'shop' } } });
    if (path === '/api/oficina') return route.fulfill({ json: { nome: 'Oficina Horizonte', temLogo: false } });
    if (path === '/api/auth/csrf') return route.fulfill({ json: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } });
    if (path === '/api/veiculos/v1') return route.fulfill({ json: { id: 'v1', clienteId: 'c1', placa: 'BRA1E23', marca: 'Volkswagen', modelo: 'T-Cross' } });
    if (path === '/api/clientes/c1') return route.fulfill({ json: { id: 'c1', nome: 'Ana Teste', ativo: true } });
    if (path === '/api/portal/acesso/link') return route.fulfill({ status: 400, json: { detail: 'Link inválido ou expirado.' } });
    if (path === '/api/portal/veiculos') return request.frame().page() === oldLink ? route.fulfill({ status: 401 }) :
      route.fulfill({ json: [{ id: 'v1', placa: 'BRA1E23', veiculo: initial.veiculo }] });
    if (path === '/api/portal/servico-atual') return route.fulfill({ json: {
      oficina: { nome: 'Oficina Horizonte', telefone: '(61) 3333-4444', email: '' }, servico: current
    } });
    if (path.endsWith('/encerramento')) {
      if (request.method() === 'GET') return route.fulfill({ json: { versao: order.versao, pendencias: closure ? 0 : 1, encerramento: closure } });
      const input = request.postDataJSON();
      expect(input.confirmado).toBe(true); expect(input.cancelarPendencias).toBe(true);
      expect(input.expectedVersion).toBe(order.versao); expect(request.headers()['x-csrf-token']).toBe('csrf');
      order = { ...order, status: input.tipo, versao: order.versao + 1, aguardandoRetirada: false };
      current = null; closure = { tipo: input.tipo, motivo: input.motivo, pendenciasCanceladas: 1, createdAt: new Date().toISOString(), autorNome: 'Dono' };
      return route.fulfill({ json: order });
    }
    if (path.endsWith('/status')) {
      order = { ...order, status: request.postDataJSON().status, aguardandoRetirada: true, versao: order.versao + 1 }; current = order;
      return route.fulfill({ json: order });
    }
    if (path === '/api/ordens-servico' && request.method() === 'POST') {
      const input = request.postDataJSON();
      expect(input.relatoInicial).toBe('Nova visita para revisão de rotina.'); expect(input.kmEntrada).toBe(15000);
      expect(input.clienteId).toBe('c1'); expect(input.veiculoId).toBe('v1');
      order = { ...initial, ...input, id: 'os-18', numero: 18, status: 'RECEBIDO' }; current = order; closure = null;
      return route.fulfill({ status: 201, json: order });
    }
    if (/^\/api\/ordens-servico\/os-\d+$/.test(path)) return route.fulfill({ json: order });
    if (['/api/ordens-servico', '/api/clientes', '/api/veiculos'].includes(path)) {
      const items = path.endsWith('ordens-servico') ? [order] : path.endsWith('clientes') ? [{ id: 'c1', nome: 'Ana Teste', ativo: true }] :
        [{ id: 'v1', clienteId: 'c1', placa: 'BRA1E23', marca: 'Volkswagen', modelo: 'T-Cross' }];
      return route.fulfill({ json: { items, page: 0, size: 100, totalElements: items.length, totalPages: 1 } });
    }
    if (/\/(atualizacoes|previsoes|fotos|vistoria|adicionais)$/.test(path)) return route.fulfill({ json: [] });
    return route.fulfill({ status: 404 });
  });
  return { mutations, setOldLink: (page: Page) => { oldLink = page; } };
}

test('pronto, entrega confirmada, histórico bloqueado e retorno com nova OS', async ({ page, context }, info) => {
  const api = await mock(context);
  await page.goto('/abrir-ordem');
  await page.getByLabel('Nova etapa').selectOption('PRONTO_PARA_RETIRADA');
  await page.getByRole('button', { name: 'Atualizar etapa', exact: true }).click();
  await expect(page.getByText('Pronto · aguardando retirada', { exact: true })).toBeVisible();
  const portal = await context.newPage(); await portal.goto('/acompanhar');
  await expect(portal.getByRole('heading', { name: initial.veiculo })).toBeVisible();
  await page.getByRole('button', { name: 'Revisar encerramento' }).click();
  await page.getByRole('button', { name: 'Confirmar encerramento' }).click();
  await expect(page.locator('app-order-closure').getByRole('alert')).toContainText('Confirme');
  await page.getByLabel('Confirmo o encerramento definitivo desta OS.').check();
  await page.getByRole('button', { name: 'Confirmar encerramento' }).click();
  await expect(page.locator('app-order-closure').getByRole('alert')).toContainText('pendências');
  await page.getByLabel('Cancelar explicitamente todos os adicionais pendentes', { exact: false }).check();
  expect(api.mutations.filter(path => path.endsWith('/encerramento'))).toHaveLength(0);
  await page.locator('app-order-closure').screenshot({ path: `test-results/encerramento-confirmacao-${info.project.name}.png` });
  await page.getByRole('button', { name: 'Confirmar encerramento' }).click();
  await expect(page.getByRole('heading', { name: 'Atendimento encerrado' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Atualizar etapa', exact: true })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Registrar atualização', exact: true })).toBeDisabled();
  await expect(page.getByRole('button', { name: '+ Nova solicitação adicional', exact: true })).toBeDisabled();
  await portal.reload(); await expect(portal.getByText('Nenhum serviço em andamento', { exact: false })).toBeVisible();
  await page.getByRole('button', { name: 'Abrir nova OS para este veículo' }).click();
  await expect(page.getByLabel('Cliente responsável')).toHaveValue('c1');
  await expect(page.getByLabel('Veículo *', { exact: true })).toHaveValue('v1');
  await expect(page.getByLabel('Relato inicial')).toHaveValue(''); await expect(page.getByLabel('Quilometragem *', { exact: true })).toHaveValue('');
  await page.getByLabel('Relato inicial').fill('Nova visita para revisão de rotina.');
  await page.getByLabel('Quilometragem *', { exact: true }).fill('15000');
  await page.getByRole('button', { name: 'Abrir ordem de serviço', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'OS-000018', exact: true })).toBeVisible();
  await portal.reload(); await expect(portal.getByRole('heading', { name: initial.veiculo })).toBeVisible();
  const link = await context.newPage(); api.setOldLink(link); await link.goto('/acompanhar#token=synthetic-old-17');
  await expect(link.getByRole('heading', { name: 'Acompanhe seu veículo' })).toBeVisible();
  await expect(link.getByRole('heading', { name: initial.veiculo })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  expect(api.mutations.filter(path => path.endsWith('/encerramento'))).toHaveLength(1);
});

test('cancelamento exige motivo e voltar não encerra', async ({ page, context }) => {
  const api = await mock(context); await page.goto('/abrir-ordem');
  await page.getByRole('button', { name: 'Revisar encerramento' }).click();
  await page.getByLabel('Tipo de encerramento').selectOption('CANCELADO');
  await page.getByLabel('Confirmo o encerramento definitivo desta OS.').check();
  await page.getByLabel('Cancelar explicitamente todos os adicionais pendentes', { exact: false }).check();
  await page.getByRole('button', { name: 'Confirmar encerramento' }).click();
  await expect(page.locator('app-order-closure').getByRole('alert')).toContainText('motivo');
  await page.getByRole('button', { name: 'Voltar sem encerrar' }).click();
  expect(api.mutations).toHaveLength(0);
  await page.getByRole('button', { name: 'Revisar encerramento' }).click();
  await page.getByLabel('Motivo (obrigatório)', { exact: true }).fill('Cliente solicitou cancelamento.');
  await page.getByLabel('Confirmo o encerramento definitivo desta OS.').check();
  await page.getByLabel('Cancelar explicitamente todos os adicionais pendentes', { exact: false }).check();
  await page.getByRole('button', { name: 'Confirmar encerramento' }).click();
  await expect(page.getByRole('heading', { name: 'Atendimento encerrado' })).toBeVisible();
  await expect(page.locator('app-order-closure')).toContainText('Cliente solicitou cancelamento.');
  expect(api.mutations).toHaveLength(1);
});
