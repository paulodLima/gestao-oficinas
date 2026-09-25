import { test, expect, Page } from '@playwright/test';

const token = 'synthetic-whatsapp-link-never-a-real-secret-16';
const order = { id: 'os-16', numero: 16, clienteId: 'c1', clienteNome: 'Ana Souza',
  veiculoId: 'v1', placa: 'BRA1E23', veiculo: 'Volkswagen T-Cross',
  relatoInicial: 'Ruído na suspensão dianteira.', entradaEm: '2026-09-25T10:00:00Z',
  kmEntrada: 48210, status: 'RECEBIDO', previsaoEm: null, atrasada: false,
  aguardandoRetirada: false, versao: 0, createdAt: '2026-09-25T10:00:00Z', updatedAt: '2026-09-25T10:00:00Z' };

async function mockOffice(page: Page) {
  const mutations: string[] = [];
  await page.route('**/api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (request.method() !== 'GET') mutations.push(`${request.method()} ${path}`);
    if (path === '/api/auth/me') return route.fulfill({ json: { id: 'owner', nome: 'Dono', oficina: { id: 'shop' } } });
    if (path === '/api/oficina') return route.fulfill({ json: { nome: 'Oficina Horizonte', temLogo: false } });
    if (path === '/api/auth/csrf') return route.fulfill({ json: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } });
    if (path === '/api/ordens-servico/os-16/acesso') {
      expect(request.headers()['x-csrf-token']).toBe('csrf');
      return request.method() === 'DELETE' ? route.fulfill({ status: 204 }) :
        route.fulfill({ json: { token, expiraEm: new Date(Date.now() + 86_400_000).toISOString() } });
    }
    if (path === '/api/ordens-servico/os-16') return route.fulfill({ json: order });
    if (path === '/api/ordens-servico' || path === '/api/clientes' || path === '/api/veiculos') {
      const items = path === '/api/ordens-servico' ? [order] : [];
      return route.fulfill({ json: { items, page: 0, size: 100, totalElements: items.length, totalPages: 1 } });
    }
    if (/\/(atualizacoes|previsoes|fotos|vistoria|adicionais)$/.test(path)) return route.fulfill({ json: [] });
    return route.fulfill({ status: 404 });
  });
  await page.goto('/abrir-ordem');
  await expect(page.getByRole('heading', { name: 'Compartilhar acompanhamento' })).toBeVisible();
  return mutations;
}

test('prepara, copia, abre WhatsApp manualmente e revoga sem registrar entrega', async ({ page, context }, testInfo) => {
  const mutations = await mockOffice(page);
  await context.grantPermissions(['clipboard-read', 'clipboard-write']);
  // Synthetic destination is fulfilled locally: nothing reaches WhatsApp or a contact.
  await context.route('https://wa.me/**', route => route.fulfill({ contentType: 'text/plain', body: 'WhatsApp simulado' }));
  await page.getByRole('button', { name: 'Preparar link seguro' }).click();
  const linkField = page.getByLabel('Link para copiar manualmente');
  await expect(linkField).toHaveValue(`http://localhost:4200/acompanhar#token=${token}`);
  await expect(page.locator('app-order-share')).not.toContainText(/Ana Souza|BRA1E23|52998224725/);
  await page.getByRole('button', { name: 'Copiar link', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Link copiado');
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(await linkField.inputValue());
  const opened = context.waitForEvent('page');
  await page.getByRole('link', { name: 'Abrir WhatsApp' }).click();
  const whatsapp = await opened;
  await whatsapp.waitForURL('https://wa.me/**');
  const destination = new URL(whatsapp.url());
  expect(destination.searchParams.get('text')).toContain(await linkField.inputValue());
  expect(destination.searchParams.get('text')).toContain('Olá!');
  expect(destination.searchParams.has('phone')).toBeFalsy();
  await whatsapp.close();
  await expect(page.getByRole('status')).toContainText('não confirma envio nem entrega');
  if (testInfo.project.name === 'mobile') await page.setViewportSize({ width: 320, height: 740 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.locator('app-order-share').screenshot({ path: `test-results/compartilhamento-${testInfo.project.name}.png` });
  await page.getByRole('button', { name: 'Revogar links desta OS' }).click();
  await expect(page.getByRole('link', { name: 'Abrir WhatsApp' })).toHaveCount(0);
  await expect(page.getByRole('status')).toContainText('Links desta OS revogados');
  expect(mutations).toEqual(['POST /api/ordens-servico/os-16/acesso', 'DELETE /api/ordens-servico/os-16/acesso']);
});

test('oferece seleção manual quando a cópia é negada e permite revogar após reload', async ({ page }) => {
  const mutations = await mockOffice(page);
  await page.evaluate(() => Object.defineProperty(navigator, 'clipboard', {
    value: { writeText: async () => { throw new Error('Denied'); } }, configurable: true
  }));
  await page.getByRole('button', { name: 'Preparar link seguro' }).click();
  await page.getByRole('button', { name: 'Copiar link', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('copie manualmente');
  const field = page.getByLabel('Link para copiar manualmente');
  await field.focus();
  expect(await field.evaluate((input: HTMLInputElement) => input.selectionEnd! - input.selectionStart!)).toBe((await field.inputValue()).length);
  await page.reload();
  await page.getByRole('button', { name: 'Revogar links desta OS' }).click();
  await expect(page.getByRole('status')).toContainText('revogados');
  expect(mutations).toHaveLength(2);
});

test('fragmento troca por sessão, some da URL e não é necessário no reload', async ({ page }) => {
  let exchanges = 0;
  let revoked = false;
  await page.route('**/api/**', route => {
    const request = route.request();
    const url = new URL(request.url());
    expect(url.searchParams.has('token')).toBeFalsy();
    if (url.pathname === '/api/auth/csrf') return route.fulfill({ json: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } });
    if (url.pathname === '/api/portal/acesso/link') {
      expect(request.postDataJSON()).toEqual({ token }); exchanges++;
      return route.fulfill({ status: 204 });
    }
    if (url.pathname === '/api/portal/veiculos') return revoked ? route.fulfill({ status: 401 }) : route.fulfill({ json: [] });
    if (url.pathname === '/api/portal/servico-atual') return route.fulfill({ json: {
      oficina: { nome: 'Oficina Horizonte', telefone: '', email: '' }, servico: order
    } });
    if (/\/(fotos|atualizacoes|adicionais)$/.test(url.pathname)) return route.fulfill({ json: [] });
    return route.fulfill({ status: 404 });
  });
  await page.goto(`/acompanhar#token=${token}`);
  await expect(page.getByRole('heading', { name: 'Volkswagen T-Cross' })).toBeVisible();
  await expect(page).toHaveURL('http://localhost:4200/acompanhar');
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Volkswagen T-Cross' })).toBeVisible();
  expect(exchanges).toBe(1);
  revoked = true;
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Acompanhe seu veículo' })).toBeVisible();
});

test('consome links diferentes ou repetidos na mesma aba sem manter a OS anterior', async ({ page }) => {
  const exchanged: string[] = [];
  let activeToken = '';
  let documents = 0;
  page.on('request', request => { if (request.resourceType() === 'document') documents++; });
  await page.route('**/api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/api/auth/csrf') return route.fulfill({ json: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } });
    if (path === '/api/portal/veiculos') return route.fulfill({ status: 401 });
    if (path === '/api/portal/acesso/link') {
      activeToken = request.postDataJSON().token;
      exchanged.push(activeToken);
      return route.fulfill({ status: 204 });
    }
    if (path === '/api/portal/servico-atual') return route.fulfill({ json: {
      oficina: { nome: 'Oficina Horizonte', telefone: '', email: '' },
      servico: { ...order, id: activeToken, veiculo: `Veículo ${activeToken}` }
    } });
    if (/\/(fotos|atualizacoes|adicionais)$/.test(path)) return route.fulfill({ json: [] });
    return route.fulfill({ status: 404 });
  });
  await page.goto('/acompanhar');
  await expect(page.getByRole('heading', { name: 'Acompanhe seu veículo' })).toBeVisible();
  for (const next of ['synthetic-A', 'synthetic-B', 'synthetic-B']) {
    await page.goto(`/acompanhar#token=${next}`);
    await expect(page).toHaveURL('http://localhost:4200/acompanhar');
    await expect(page.getByRole('heading', { name: `Veículo ${next}` })).toBeVisible();
  }
  expect(exchanged).toEqual(['synthetic-A', 'synthetic-B', 'synthetic-B']);
  expect(documents).toBe(1);
});

test('não restaura dados atrasados de A durante a navegação para o link B', async ({ page }) => {
  let activeToken = '';
  let release!: () => void;
  const delay = new Promise<void>(resolve => release = resolve);
  let requested!: () => void;
  const pending = new Promise<void>(resolve => requested = resolve);
  await page.route('**/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/api/auth/csrf') return route.fulfill({ json: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } });
    if (path === '/api/portal/acesso/link') {
      activeToken = request.postDataJSON().token;
      return route.fulfill({ status: 204 });
    }
    if (path === '/api/portal/servico-atual') {
      const requestedToken = activeToken;
      if (requestedToken === 'synthetic-A') { requested(); await delay; }
      return route.fulfill({ json: { oficina: { nome: 'Oficina Horizonte', telefone: '', email: '' },
        servico: { ...order, id: requestedToken, veiculo: `Veículo ${requestedToken}` } } });
    }
    if (/\/(fotos|atualizacoes|adicionais)$/.test(path)) return route.fulfill({ json: [] });
    return route.fulfill({ status: 404 });
  });
  await page.goto('/acompanhar#token=synthetic-A');
  await pending;
  await page.goto('/acompanhar#token=synthetic-B');
  await expect(page).toHaveURL('http://localhost:4200/acompanhar');
  await expect(page.getByRole('heading', { name: 'Veículo synthetic-A' })).toHaveCount(0);
  release();
  await expect(page.getByRole('heading', { name: 'Veículo synthetic-B' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Veículo synthetic-A' })).toHaveCount(0);
});
