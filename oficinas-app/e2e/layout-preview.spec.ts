import { test, expect } from '@playwright/test';

test('layout das telas e gráficos sem transbordamento', async ({ page }, testInfo) => {
  const profile = { id: 'preview', slug: 'preview', nome: 'Oficina Horizonte', telefone: '(61) 3333-4444', emailContato: '', endereco: 'Brasília, DF', horario: '', fuso: 'America/Sao_Paulo', perfilPublico: true, temLogo: false, versao: 0 };
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    let data: unknown = { items: [], totalElements: 0, totalPages: 0, page: 0 };
    if (path === '/api/auth/me') data = { id: 'preview', nome: 'Proprietário', email: 'preview@example.test', oficina: { id: 'preview', nome: profile.nome } };
    else if (path === '/api/oficina' || path.startsWith('/api/publico/oficinas/')) data = profile;
    else if (path === '/api/painel') data = { indicadores: { ativas: 24, atrasadas: 3, aprovacoes: 5, pecas: 7, prontas: 6 }, ordens: { items: [], totalElements: 0, totalPages: 0 }, fuso: 'America/Sao_Paulo', verificadoEm: '2026-09-29T12:00:00Z' };
    else if (path === '/api/portal/oficinas') data = [];
    else if (path === '/api/notificacoes') data = { items: [], totalElements: 2, totalPages: 1, page: 0 };
    await route.fulfill({ json: data });
  });
  for (const path of ['/painel', '/clientes', '/veiculos', '/abrir-ordem', '/perfil', '/notificacoes', '/acompanhar', '/oficina/preview']) {
    await page.goto(path);
    await expect(page.getByRole('heading').first()).toBeVisible();
    await page.screenshot({ path: testInfo.outputPath(path.replaceAll('/', '-') + '.png'), fullPage: true, animations: 'disabled' });
    if (testInfo.project.name === 'desktop' && await page.locator('.sidebar').count()) {
      const sidebar = await page.locator('.sidebar').boundingBox();
      const content = await page.locator('.office-content').boundingBox();
      expect(content!.x).toBeGreaterThanOrEqual(sidebar!.x + sidebar!.width);
    }
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), path).toBeTruthy();
    await expect(page.getByText('OPERAÇÃO / VISÃO GERAL', { exact: true })).toHaveCount(0);
  }
  await page.goto('/painel');
  await expect(page.getByRole('heading', { name: 'Situação dos serviços' })).toBeVisible();
  await page.getByRole('button', { name: 'Ver veículos prontos' }).click();
  await expect(page).toHaveURL(/situacao=PRONTAS/);
  await expect(page.getByRole('button', { name: 'Recolher menu', exact: true })).toHaveCount(0);
  await expect(page.locator('.sidebar a[href="/notificacoes"]')).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Avisos: 2 não lidos' })).toBeVisible();
  await expect(page.locator('app-notification-bell .count')).toHaveText('2');
  await page.getByRole('link', { name: 'Avisos: 2 não lidos' }).click();
  await expect(page).toHaveURL(/notificacoes/);
});

test('login personalizado preserva identidade e cabe em telas pequenas', async ({ page }, info) => {
  await page.route('**/api/publico/oficinas/horizonte/identidade', route => route.fulfill({
    json: { nome: 'Oficina Horizonte', temLogo: false, temCapa: false }
  }));
  await page.goto('/entrar?oficina=horizonte');
  await expect(page.locator('.login-brand')).toContainText('Oficina Horizonte');
  await expect(page.getByLabel('E-mail', { exact: true })).toBeVisible();
  await page.screenshot({ path: info.outputPath('login-personalizado.png'), fullPage: true, animations: 'disabled' });
  await page.getByRole('link', { name: 'Esqueci minha senha' }).click();
  await expect(page).toHaveURL(/oficina=horizonte/);
  await expect(page.locator('.login-brand')).toContainText('Oficina Horizonte');
  await page.setViewportSize({ width: 320, height: 740 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  await page.goto('/entrar');
  await expect(page.locator('.login-brand')).toContainText('Sua oficina');
});
