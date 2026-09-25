import { test, expect, BrowserContext } from '@playwright/test';

const googleUrl = 'https://g.page/r/Test_18/review';
const consent = 'Autorizo a oficina a publicar minha nota e meu comentário, sem nome, placa, CPF, contatos ou fotos do veículo. A autorização é opcional e não publica automaticamente a avaliação.';
const initial = {
  contexto: 'context-A',
  atendimento: { numero: 18, entregueEm: '2026-09-25T14:00:00Z', oficinaNome: 'Oficina Horizonte', telefone: '(61) 3333-4444', email: 'oficina@example.test', googleUrl, veiculo: 'Volkswagen T-Cross' },
  atualizacoes: [{ texto: 'Revisão da suspensão concluída e veículo testado.', createdAt: '2026-09-25T12:00:00Z' }, { texto: 'Veículo entregue ao responsável.', createdAt: '2026-09-25T14:00:00Z' }],
  avaliacao: null as { nota: number; comentario: string | null; consentimentoPublicacao: boolean; createdAt: string } | null,
  expiraEm: '2026-10-02T14:00:00Z', textoConsentimento: consent
};

async function mock(context: BrowserContext) {
  let grant = false, expired = false, summary = structuredClone(initial), url: string | null = googleUrl;
  const writes: { path: string; body: Record<string, unknown> }[] = [];
  await context.route('**/api/**', route => {
    const request = route.request(), path = new URL(request.url()).pathname;
    const ok = (json: unknown) => route.fulfill({ json });
    const denied = () => route.fulfill({ status: 401, json: { detail: 'Convite inválido ou expirado.' } });
    if (request.method() !== 'GET') {
      expect(request.headers()['x-csrf-token']).toBe('csrf');
      writes.push({ path, body: request.postDataJSON() || {} });
    }
    if (path === '/api/auth/csrf') return ok({ token: 'csrf', headerName: 'X-CSRF-TOKEN' });
    if (path === '/api/auth/me') return ok({ id: 'owner', nome: 'Dono', oficina: { id: 'shop' } });
    if (path === '/api/oficina') return ok({ nome: 'Oficina Horizonte', temLogo: false });
    if (path === '/api/portal/avaliacoes/acesso') {
      const token = request.postDataJSON().token;
      grant = ['synthetic-review', 'synthetic-review-B'].includes(token);
      if (grant) summary = { ...summary, contexto: token === 'synthetic-review' ? 'context-A' : 'context-B', atendimento: { ...summary.atendimento, numero: token === 'synthetic-review' ? 18 : 19 } };
      return grant ? route.fulfill({ status: 204 }) : denied();
    }
    if (path === '/api/portal/avaliacoes/resumo') return grant && !expired ? ok({ ...summary, atendimento: { ...summary.atendimento, googleUrl: url } }) : denied();
    if (path === '/api/portal/avaliacoes') {
      if (!grant || expired) return denied();
      if (request.postDataJSON().contexto !== summary.contexto) return route.fulfill({ status: 409, json: { code: 'AVALIACAO_CONTEXTO_ALTERADO', detail: 'O convite mudou em outra aba. Reabra o convite do atendimento que deseja avaliar.' } });
      if (summary.avaliacao) return route.fulfill({ status: 409, json: { detail: 'Já avaliado.' } });
      summary = { ...summary, avaliacao: { ...request.postDataJSON(), createdAt: '2026-09-25T15:00:00Z' } };
      return ok(summary.avaliacao);
    }
    if (path === '/api/avaliacoes/configuracao') {
      if (request.method() === 'PATCH') url = request.postDataJSON().googleUrl || null;
      return ok({ googleUrl: url });
    }
    if (path === '/api/avaliacoes') {
      const page = Number(new URL(request.url()).searchParams.get('page') || 0);
      return ok({ items: [{ id: 'review-' + page, ordemServicoId: 'os-18', numero: 18 + page, nota: page ? 5 : 1, comentario: page ? 'Bom atendimento.' : 'Prazo pode melhorar.', consentimentoPublicacao: page > 0, createdAt: '2026-09-25T15:00:00Z' }], page, totalPages: 2, totalElements: 21 });
    }
    if (path === '/api/portal/veiculos') return denied();
    return route.fulfill({ status: 404 });
  });
  return { writes, expire: () => { expired = true; } };
}

test('resumo restrito, nota baixa privada, Google neutro e recarga sem duplicação', async ({ page, context }, info) => {
  const api = await mock(context);
  await page.goto('/avaliar#token=synthetic-review');
  await expect(page.getByRole('heading', { name: 'Seu atendimento, concluído.' })).toBeVisible();
  await expect(page).toHaveURL(/\/avaliar$/);
  await expect(page.getByRole('checkbox')).not.toBeChecked();
  const google = page.getByRole('link', { name: 'Avaliar no Google' }); await expect(google).toHaveAttribute('href', googleUrl);
  await expect(google).toHaveAttribute('rel', 'noopener noreferrer');
  await page.getByRole('button', { name: 'Enviar avaliação' }).click(); await expect(page.getByRole('alert')).toContainText('Selecione uma nota');
  await page.getByRole('radio', { name: '1 estrela', exact: true }).check();
  await page.getByLabel('Comentário (opcional)').fill('Prazo pode melhorar.');
  await page.screenshot({ path: `test-results/avaliacao-formulario-${info.project.name}.png`, fullPage: true });
  await page.getByRole('button', { name: 'Enviar avaliação' }).click();
  await expect(page.getByRole('heading', { name: 'Obrigado pela sua avaliação' })).toBeVisible();
  await expect(page.getByText('Você não autorizou a publicação.', { exact: false })).toBeVisible();
  await expect(google).toHaveAttribute('href', googleUrl);
  expect(api.writes.filter(write => write.path === '/api/portal/avaliacoes')).toEqual([{ path: '/api/portal/avaliacoes', body: { contexto: 'context-A', nota: 1, comentario: 'Prazo pode melhorar.', consentimentoPublicacao: false } }]);
  await page.reload(); await expect(page.getByRole('heading', { name: 'Obrigado pela sua avaliação' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Enviar avaliação' })).toHaveCount(0);
  await page.screenshot({ path: `test-results/avaliacao-confirmada-${info.project.name}.png`, fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect(await page.evaluate(() => Object.values(localStorage).some(value => String(value).includes('synthetic-review')))).toBe(false);
  await page.goto('/acompanhar'); await expect(page.getByRole('heading', { name: 'Acompanhe seu veículo' })).toBeVisible();
});

test('consentimento é explícito e não publica automaticamente', async ({ page, context }) => {
  const api = await mock(context); await page.goto('/avaliar#token=synthetic-review');
  await page.getByRole('radio', { name: '5 estrelas', exact: true }).check(); await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Enviar avaliação' }).click();
  await expect(page.getByText('Nada é publicado automaticamente.', { exact: false })).toBeVisible();
  expect(api.writes.at(-1)?.body['consentimentoPublicacao']).toBe(true);
  await expect(page.getByRole('link', { name: 'Avaliar no Google' })).toHaveAttribute('href', googleUrl);
});

test('expiração no envio e convite inválido não recuperam a OS anterior', async ({ page, context }) => {
  const api = await mock(context); await page.goto('/avaliar#token=synthetic-review');
  await page.getByRole('radio', { name: '3 estrelas', exact: true }).check(); api.expire();
  await page.getByRole('button', { name: 'Enviar avaliação' }).click();
  await expect(page.getByRole('alert')).toContainText('expirado'); await expect(page.getByRole('heading', { name: 'Seu atendimento, concluído.' })).toHaveCount(0);
  await page.goto('/avaliar#token=invalid'); await expect(page.getByRole('alert')).toContainText('inválido');
  await expect(page.getByRole('radio')).toHaveCount(0);
});

test('oficina consulta respostas privadas, pagina e desativa o Google', async ({ page, context }, info) => {
  await mock(context); await page.goto('/avaliacoes');
  await expect(page.getByRole('heading', { name: 'Avaliações', exact: true })).toBeVisible();
  await expect(page.getByText('Privada · publicação não autorizada.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'OS 18', exact: true })).toHaveAttribute('href', '/abrir-ordem?id=os-18');
  await page.screenshot({ path: `test-results/avaliacoes-oficina-${info.project.name}.png`, fullPage: true });
  await page.getByRole('button', { name: 'Próxima', exact: true }).click(); await expect(page.getByRole('link', { name: 'OS 19', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Próxima', exact: true })).toBeDisabled();
  await page.getByLabel('Link de avaliação do Perfil da Empresa (opcional)').fill(''); await page.getByRole('button', { name: 'Salvar link do Google' }).click();
  await expect(page.getByRole('status')).toHaveText('Link do Google atualizado.');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.goto('/avaliar#token=synthetic-review'); await expect(page.getByRole('heading', { name: 'Seu atendimento, concluído.' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Avaliar no Google' })).toHaveCount(0);
});

test('duas abas não gravam resposta nem consentimento na OS da outra aba', async ({ page, context }) => {
  const api = await mock(context); await page.goto('/avaliar#token=synthetic-review');
  await page.getByRole('radio', { name: '5 estrelas', exact: true }).check(); await page.getByRole('checkbox').check();
  const second = await context.newPage(); await second.goto('/avaliar#token=synthetic-review-B');
  await expect(second.getByText('ENTREGUE · OS 19', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Enviar avaliação' }).click();
  await expect(page.getByRole('alert')).toContainText('outra aba'); await expect(page.getByRole('radio')).toHaveCount(0);
  expect(api.writes.at(-1)?.body['contexto']).toBe('context-A');
  await second.reload(); await expect(second.getByRole('heading', { name: 'Como foi o atendimento?' })).toBeVisible();
  await second.getByRole('radio', { name: '2 estrelas', exact: true }).check(); await second.getByRole('button', { name: 'Enviar avaliação' }).click();
  await expect(second.getByRole('heading', { name: 'Obrigado pela sua avaliação' })).toBeVisible();
  expect(api.writes.at(-1)?.body).toMatchObject({ contexto: 'context-B', nota: 2, consentimentoPublicacao: false });
});

test('convite vazio na mesma aba ou em outra página não restaura atendimento anterior', async ({ page, context }) => {
  const api = await mock(context); await page.goto('/avaliar#token=synthetic-review');
  await expect(page.getByRole('heading', { name: 'Seu atendimento, concluído.' })).toBeVisible();
  await page.goto('/avaliar#token='); await expect(page.getByRole('alert')).toContainText('inválido');
  await expect(page.getByRole('radio')).toHaveCount(0); expect(api.writes.at(-1)?.body).toEqual({ token: '' });
  await page.goto('/avaliar#token=synthetic-review'); await expect(page.getByRole('heading', { name: 'Seu atendimento, concluído.' })).toBeVisible();
  const second = await context.newPage(); await second.goto('/avaliar#token');
  await expect(second.getByRole('alert')).toContainText('inválido'); await expect(second.getByRole('radio')).toHaveCount(0);
  expect(api.writes.at(-1)?.body).toEqual({ token: '' });
  await page.reload(); await expect(page.getByRole('radio')).toHaveCount(0);
});
