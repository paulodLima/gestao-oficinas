import { test, expect, APIRequestContext, Page, TestInfo } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const mailpit = process.env['MAILPIT_URL'] || 'http://localhost:18025';
const pixel = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64');

async function post(request: APIRequestContext, path: string, data: unknown) {
  const csrf = await (await request.get('/api/auth/csrf')).json();
  const result = await request.post(path, { data, headers: { [csrf.headerName]: csrf.token } });
  expect(result.ok(), `${path}: ${result.status()} ${await result.text()}`).toBeTruthy();
  return result.status() === 204 ? null : result.json();
}

async function email(request: APIRequestContext, recipient: string, subject: RegExp) {
  let id = '';
  await expect.poll(async () => {
    const inbox = await (await request.get(`${mailpit}/api/v1/messages?limit=200`)).json();
    const item = inbox.messages.find((message: { To: { Address: string }[]; Subject: string; ID: string }) =>
      message.To.some(to => to.Address === recipient) && subject.test(message.Subject));
    id = item?.ID ?? ''; return !!id;
  }, { timeout: 25_000 }).toBeTruthy();
  return (await request.get(`${mailpit}/api/v1/message/${id}`)).json();
}

async function code(request: APIRequestContext, recipient: string, subject: RegExp) {
  const message = await email(request, recipient, subject);
  const match = message.Text.match(/\b\d{6}\b/);
  expect(match, 'SMTP local deve conter código de seis dígitos').toBeTruthy();
  return match[0] as string;
}

async function layout(page: Page, info: TestInfo, name: string) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${name}: overflow`).toBeTruthy();
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']).analyze();
  await info.attach(`${name}-accessibility`, { body: JSON.stringify(result, null, 2), contentType: 'application/json' });
  expect.soft(result.violations.map(item => ({ id: item.id, targets: item.nodes.map(node => node.target) })), name).toEqual([]);
  const small = await page.locator('button:not(:disabled), input:not(:disabled):not([type=hidden]), select:not(:disabled), summary').evaluateAll(elements => elements.flatMap(element => {
    if (!(element instanceof HTMLElement) || !element.getClientRects().length) return [];
    if (element instanceof HTMLInputElement && ['checkbox', 'radio'].includes(element.type)) {
      const label = element.labels?.[0]; if (label) element = label;
    }
    const rect = element.getBoundingClientRect();
    return rect.width >= 44 && rect.height >= 44 ? [] : [{ tag: element.tagName, text: element.getAttribute('id') || element.textContent?.trim().slice(0, 60), width: rect.width, height: rect.height }];
  }));
  expect.soft(small, `${name}: alvos 44px`).toEqual([]);
  await page.screenshot({ path: info.outputPath(`${name}.png`), fullPage: true });
}

test('jornada real: entrada, fotos, adicional, atraso, entrega, avaliação e retorno', async ({ page, browser, browserName }, info) => {
  const request = page.request;
  const stamp = `${Date.now()}-${info.project.name}`;
  const ownerEmail = `owner-${stamp}@qa.test`, customerEmail = `customer-${stamp}@qa.test`;
  await post(request, '/api/auth/cadastro', { nome: 'Oficina QA', nomeOficina: 'Oficina de Validação', email: ownerEmail, senha: 'Qa-segura-2026!' });
  await post(request, '/api/auth/login', { email: ownerEmail, senha: 'Qa-segura-2026!' });
  const shop = await (await request.get('/api/oficina')).json();
  const customer = await post(request, '/api/clientes', { nome: 'Cliente de Validação', cpf: '52998224725', telefone: '', email: customerEmail });
  const challenge = await post(request, `/api/clientes/${customer.id}/verificacao`, {});
  await post(request, `/api/clientes/${customer.id}/verificacao/confirmacao`, {
    desafioId: challenge.desafioId, codigo: await code(request, customerEmail, /Confirme seu e-mail/i)
  });
  const vehicle = await post(request, '/api/veiculos', { clienteId: customer.id, placa: 'BRA1E23', marca: 'Fiat', modelo: 'Uno', ano: 2020, cor: 'Branco' });
  await page.goto('/abrir-ordem');
  await expect(page.getByText('Nenhuma ordem encontrada.')).toBeVisible();
  await layout(page, info, 'entrada-vazia');
  await page.getByLabel('Cliente responsável *').selectOption(customer.id);
  await page.getByLabel('Veículo *').selectOption(vehicle.id);
  await page.getByLabel('Relato inicial *').fill('Conferir ruído no motor e na suspensão.');
  await page.getByLabel('Quilometragem *').fill('25000');
  await page.getByLabel('Data e hora de entrada *').fill(new Date(Date.now() - 27 * 3_600_000).toISOString().slice(0, 16));
  await page.getByLabel('Previsão inicial').fill(new Date(Date.now() - 4 * 3_600_000).toISOString().slice(0, 16));
  const opening = page.waitForResponse(response => response.url().endsWith('/api/ordens-servico') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Abrir ordem de serviço' }).click();
  expect((await opening).status()).toBe(201);
  await expect(page.getByRole('heading', { name: 'OS-000001', exact: true })).toBeVisible();
  const persisted = await request.get('/api/ordens-servico');
  expect(persisted.ok()).toBeTruthy();
  const orders = (await persisted.json()).items;
  expect(orders).toHaveLength(1);
  const order = orders[0];
  await expect(page.getByText('Previsão ultrapassada', { exact: true })).toBeVisible();
  await page.getByLabel('Quilometragem', { exact: true }).fill('25000');
  await page.getByLabel('Combustível aproximado').selectOption('METADE');
  await page.getByLabel('Avarias aparentes').fill('Risco superficial no para-choque.');
  await page.getByRole('button', { name: 'Confirmar vistoria' }).click();
  await expect(page.getByText('Versão 1 · Confirmada')).toBeVisible();

  // Native picker cancellation/denial is represented by returning no file, not a fabricated camera permission.
  await page.locator('#order-camera').setInputFiles([]);
  await expect(page.getByText('Nenhuma foto registrada nesta ordem.')).toBeVisible();
  await page.locator('#order-camera').focus(); await page.keyboard.press('Tab');
  await expect(page.locator('#order-gallery')).toBeFocused();
  await page.getByLabel('Publicar novas fotos no portal do cliente').check();
  let lost = false;
  await page.route(`**/api/ordens-servico/${order.id}/fotos`, async route => {
    if (route.request().method() !== 'POST' || lost) return route.continue();
    lost = true;
    await new Promise(resolve => setTimeout(resolve, 750));
    // WebKit interception omits file bodies (Playwright #14624). Test transport failure there;
    // Chromium additionally verifies the harder case of losing a response after persistence.
    if (browserName !== 'webkit') {
      const response = await route.fetch(); expect(response.status(), await response.text()).toBe(201);
    }
    await route.abort('failed');
  });
  await page.locator('#order-gallery').setInputFiles({ name: 'vistoria.png', mimeType: 'image/png', buffer: pixel });
  await expect(page.getByRole('button', { name: 'Tentar novamente: vistoria.png' })).toBeVisible();
  await page.unroute(`**/api/ordens-servico/${order.id}/fotos`);
  await page.getByRole('button', { name: 'Tentar novamente: vistoria.png' }).click();
  await expect(page.locator('.photo-grid figure')).toHaveCount(1);
  await expect(page.getByRole('button', { name: 'Tentar novamente: vistoria.png' })).toHaveCount(0);
  expect((await (await request.get(`/api/ordens-servico/${order.id}/fotos`)).json()).length).toBe(1);
  await page.unroute(`**/api/ordens-servico/${order.id}/fotos`);
  await layout(page, info, 'oficina-vistoria');

  await page.getByRole('button', { name: '+ Nova solicitação adicional', exact: true }).click();
  await page.getByLabel('Problema encontrado *').fill('Pastilhas de freio desgastadas');
  await page.getByLabel('Justificativa *').fill('Substituição necessária para segurança.');
  await page.getByLabel('Impacto no prazo *').fill('Acrescenta um dia para testes.');
  await page.getByLabel('Descrição *', { exact: true }).fill('Jogo de pastilhas');
  await page.getByLabel('Quantidade *').fill('1');
  await page.getByLabel('Valor unitário *').fill('150');
  await page.locator('app-additional-request').getByRole('button', { name: 'Salvar rascunho', exact: true }).click();
  await page.getByRole('button', { name: 'Enviar ao cliente', exact: true }).click();
  await expect(page.locator('app-additional-request')).toContainText('Enviada');
  await email(request, customerEmail, /adiciona/i);

  const clientContext = await browser.newContext({ ...info.project.use, baseURL: info.project.use.baseURL } as Parameters<typeof browser.newContext>[0]);
  const client = await clientContext.newPage();
  let failedAdditional = false;
  await client.route(`**/api/portal/ordens-servico/${order.id}/adicionais`, async route => {
    if (failedAdditional) return route.continue();
    failedAdditional = true;
    await new Promise(resolve => setTimeout(resolve, 500));
    await route.abort('failed');
  });
  await client.goto('/acompanhar');
  await client.getByLabel('Identificador da oficina').fill(shop.slug);
  await client.getByLabel('Placa do veículo').fill('BRA1E23');
  await client.getByRole('button', { name: 'Receber código' }).click();
  await client.getByLabel('Código de 6 dígitos').fill(await code(request, customerEmail, /acesso/i));
  await client.getByRole('button', { name: 'Entrar no acompanhamento' }).click();
  await expect(client.getByRole('heading', { name: 'Fiat Uno', exact: true })).toBeVisible();
  await expect(client.getByRole('alert')).toContainText('Não foi possível carregar os serviços adicionais.');
  await client.getByRole('button', { name: 'Recarregar serviços adicionais' }).click();
  await expect(client.getByRole('button', { name: 'Ampliar foto do serviço' })).toHaveCount(1);
  await client.getByRole('button', { name: 'Aprovar', exact: true }).click();
  await client.getByRole('button', { name: 'Confirmar decisões selecionadas' }).click();
  await client.getByLabel('Código de 6 dígitos').fill(await code(request, customerEmail, /decidir adicionais/i));
  await client.getByRole('button', { name: 'Registrar decisão definitiva' }).click();
  await expect(client.getByRole('status')).toContainText('Decisão registrada');
  await layout(client, info, 'portal-aprovado');

  await page.reload();
  const future = new Date(Date.now() + 24 * 3_600_000);
  const localFuture = new Date(future.getTime() - 3 * 3_600_000).toISOString().slice(0, 16);
  await page.locator('#forecast-new').fill(localFuture);
  await page.getByLabel('Motivo público *').fill('Peça chegou depois do previsto');
  await page.getByLabel('Próxima ação *').fill('Concluir os testes de segurança.');
  await page.getByRole('button', { name: 'Atualizar previsão' }).click();
  await expect(page.getByText('Dentro da previsão', { exact: true })).toBeVisible();
  await email(request, customerEmail, /Previsão atualizada/);
  await client.reload();
  await expect(client.locator('.forecast-note')).toContainText('Concluir os testes de segurança.');
  await page.getByLabel('Nova etapa *').selectOption('PRONTO_PARA_RETIRADA');
  await page.getByRole('button', { name: 'Atualizar etapa', exact: true }).click();
  await expect(page.getByText('Pronto · aguardando retirada')).toBeVisible();
  await email(request, customerEmail, /Veículo pronto/);
  await page.getByRole('button', { name: 'Revisar encerramento' }).click();
  await page.getByLabel('Confirmo o encerramento definitivo desta OS.').check();
  await page.getByRole('button', { name: 'Confirmar encerramento', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Atendimento encerrado' })).toBeVisible();
  const photoRoute = `**/api/ordens-servico/${order.id}/fotos`;
  await page.route(photoRoute, route => route.abort('failed'), { times: 1 });
  await page.reload();
  const reloadPhotos = page.getByRole('button', { name: 'Recarregar fotos', exact: true });
  await expect(reloadPhotos).toBeEnabled();
  await expect(page.locator('#order-gallery')).toBeDisabled();
  await reloadPhotos.click();
  await expect(page.locator('.photo-grid figure')).toHaveCount(1);
  await expect(page.getByRole('button', { name: 'Remover', exact: true })).toBeDisabled();
  await expect(reloadPhotos).toHaveCount(0);
  await layout(page, info, 'historico-encerrado');
  await email(request, customerEmail, /Serviço encerrado/);
  await email(request, customerEmail, /Avalie o atendimento/);
  await page.getByRole('button', { name: 'Obter link de avaliação' }).click();
  const reviewLink = await page.getByLabel('Link restrito de avaliação').inputValue();
  await client.goto(reviewLink);
  await client.getByRole('radio', { name: '5 estrelas', exact: true }).check();
  await client.getByLabel('Comentário (opcional)').fill('Atendimento claro e cuidadoso.');
  await expect(client.getByRole('checkbox')).not.toBeChecked();
  await layout(client, info, 'avaliacao');
  await client.getByRole('button', { name: 'Enviar avaliação' }).click();
  await expect(client.getByRole('heading', { name: 'Obrigado pela sua avaliação' })).toBeVisible();
  await expect(client.getByText('Sua avaliação é privada. Você não autorizou a publicação.')).toBeVisible();
  await client.goto('/acompanhar');
  await expect(client.getByRole('heading', { name: 'Nenhum serviço em andamento' })).toBeVisible();
  expect((await client.request.get(`/api/portal/ordens-servico/${order.id}/fotos`)).ok()).toBeFalsy();
  await page.getByRole('button', { name: 'Abrir nova OS para este veículo' }).click();
  await expect(page.getByLabel('Cliente responsável *')).toHaveValue(customer.id);
  await expect(page.getByLabel('Veículo *')).toHaveValue(vehicle.id);
  await expect(page.getByLabel('Relato inicial *')).toHaveValue('');
  await page.getByLabel('Relato inicial *').fill('Retorno para revisão preventiva do veículo.');
  await page.getByLabel('Quilometragem *').fill('26000');
  await page.getByRole('button', { name: 'Abrir ordem de serviço' }).click();
  await expect(page.getByRole('heading', { name: 'OS-000002', exact: true })).toBeVisible();
  expect((await (await request.get(`/api/ordens-servico/${order.id}`)).json()).status).toBe('ENTREGUE');
  await layout(page, info, 'nova-visita');
  await clientContext.close();
});
