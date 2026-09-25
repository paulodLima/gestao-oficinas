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
