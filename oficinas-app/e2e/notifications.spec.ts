import { test, expect } from '@playwright/test';

test('central permite ler avisos e solicitar reenvio sem simular entrega', async ({ page }, testInfo) => {
  let read = false;
  let emailState = 'FALHOU';
  await page.route('**/api/**', route => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/api/auth/me') return route.fulfill({ json: { id: 'owner', nome: 'Oficina', email: 'owner@example.test', oficina: { id: 'shop', nome: 'Oficina Horizonte' } } });
    if (url.pathname === '/api/oficina') return route.fulfill({ json: { nome: 'Oficina Horizonte', temLogo: false } });
    if (url.pathname === '/api/auth/csrf') return route.fulfill({ json: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } });
    if (url.pathname === '/api/notificacoes/notice-1' && request.method() === 'PATCH') {
      expect(request.headers()['x-csrf-token']).toBe('csrf'); read = true;
      return route.fulfill({ status: 204 });
    }
    if (url.pathname.endsWith('/reenvio')) { emailState = 'PENDENTE'; return route.fulfill({ status: 202 }); }
    if (url.pathname === '/api/notificacoes') {
      const items = url.searchParams.get('naoLidas') === 'true' && read ? [] : [{ id: 'notice-1', ordemServicoId: 'os-1',
        evento: 'ADICIONAL_ENVIADO', titulo: 'Adicional disponível', mensagem: 'OS-42 · Há uma solicitação adicional disponível para sua análise.',
        lida: read, createdAt: '2026-09-25T12:00:00Z', emailEstado: emailState, tentativas: 5,
        proximaTentativaEm: '2026-09-25T15:00:00Z', ultimoErro: 'EMAIL_INDISPONIVEL' }];
      return route.fulfill({ json: { items, page: 0, size: 20, totalElements: items.length, totalPages: items.length ? 1 : 0 } });
    }
    return route.fulfill({ status: 404 });
  });
  await page.goto('/notificacoes');
  await expect(page.getByRole('heading', { name: 'Central de avisos' })).toBeVisible();
  await expect(page.getByText('Falha no envio', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Reenviar e-mail: Adicional disponível' }).click();
  await expect(page.getByText('Reenvio solicitado. O e-mail ainda não foi enviado.')).toBeVisible();
  await expect(page.getByText('Na fila de envio')).toBeVisible();
  await page.getByRole('button', { name: 'Marcar como lido: Adicional disponível' }).click();
  await expect(page.getByText('Lido', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Não lidos', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Tudo em dia' })).toBeVisible();
  await page.getByRole('button', { name: 'Todos', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Adicional disponível' })).toBeVisible();
  if (testInfo.project.name === 'mobile') await page.setViewportSize({ width: 320, height: 740 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.screenshot({ path: `test-results/notificacoes-${testInfo.project.name}.png`, fullPage: true });
});

test('central exibe erro recuperável ao carregar', async ({ page }) => {
  let fail = true;
  await page.route('**/api/**', route => {
    if (route.request().url().includes('/auth/me')) return route.fulfill({ json: { id: 'owner', oficina: { id: 'shop' } } });
    if (route.request().url().includes('/notificacoes')) return fail ? route.fulfill({ status: 503 }) :
      route.fulfill({ json: { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } });
    return route.fulfill({ json: { nome: 'Oficina Horizonte' } });
  });
  await page.goto('/notificacoes');
  await expect(page.getByRole('alert')).toContainText('Não foi possível carregar');
  fail = false;
  await page.getByRole('button', { name: 'Atualizar avisos' }).click();
  await expect(page.getByRole('heading', { name: 'Nenhum aviso por enquanto' })).toBeVisible();
});
