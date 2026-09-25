import { test, expect } from '@playwright/test';

test('abre, localiza e consulta uma ordem de serviço', async ({ page }, info) => {
  const email = `ordem-${Date.now()}-${info.project.name}@example.test`;
  await page.goto('/cadastro');
  await page.getByLabel('Seu nome', { exact: true }).fill('Dono Operação');
  await page.getByLabel('Nome da oficina').fill('Oficina Operação');
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill('Oficina-segura-123');
  await page.getByLabel('Confirmar senha').fill('Oficina-segura-123');
  await page.getByRole('button', { name: 'Criar minha conta' }).click();
  await page.getByRole('link', { name: /Ir para o login/ }).click();
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill('Oficina-segura-123');
  await page.getByRole('button', { name: 'Entrar na oficina' }).click();
  await page.getByRole('link', { name: 'Clientes', exact: true }).click();

  await page.getByLabel('Nome completo *').fill('Ana Souza');
  await page.getByLabel('CPF *').fill('529.982.247-25');
  await page.getByRole('button', { name: 'Cadastrar cliente' }).click();
  await expect(page.getByRole('status')).toContainText('Cliente cadastrado');
  await page.getByRole('link', { name: 'Veículos', exact: true }).click();
  await page.getByLabel('Placa antiga ou Mercosul *').fill('BRA-1E23');
  await page.getByLabel('Marca *').fill('Volkswagen');
  await page.getByLabel('Modelo *').fill('T-Cross');
  await page.getByLabel('Responsável *').selectOption({ label: 'Ana Souza' });
  await page.getByRole('button', { name: 'Cadastrar veículo' }).click();
  await expect(page.getByRole('status')).toContainText('Veículo cadastrado');

  await page.getByRole('link', { name: 'Abrir ordem de serviço', exact: true }).click();
  await page.getByLabel('Cliente responsável *').selectOption({ label: 'Ana Souza' });
  await page.getByLabel('Veículo *').selectOption({ label: 'BRA-1E23 · Volkswagen T-Cross' });
  await page.getByLabel('Relato inicial *').fill('Ruído na suspensão dianteira ao passar em desníveis.');
  await page.getByLabel('Quilometragem *').fill('48210');
  await page.getByRole('button', { name: 'Abrir ordem de serviço' }).click();
  await expect(page.getByRole('status')).toContainText('Ordem de serviço aberta');
  await expect(page.getByRole('heading', { name: 'OS-000001' })).toBeVisible();
  await expect(page.getByText('48.210 km')).toBeVisible();

  await page.getByLabel('Quilometragem', { exact: true }).last().fill('48211');
  await page.getByLabel('Combustível aproximado').selectOption('METADE');
  await page.getByLabel('Avarias aparentes').fill('Risco leve no para-choque traseiro.');
  await page.route('**/vistoria/confirmacoes', route => route.abort('failed'));
  await page.getByRole('button', { name: 'Confirmar vistoria' }).click();
  await expect(page.getByRole('alert')).toContainText('Não foi possível');
  await expect(page.getByLabel('Avarias aparentes')).toHaveValue('Risco leve no para-choque traseiro.');
  await page.unroute('**/vistoria/confirmacoes');
  await page.getByRole('button', { name: 'Confirmar vistoria' }).click();
  await expect(page.getByRole('status')).toContainText('Vistoria confirmada');
  await expect(page.getByText('Versão 1 · Confirmada')).toBeVisible();

  await page.getByLabel('Nova etapa *').selectOption('EM_TESTES');
  await page.getByRole('button', { name: 'Atualizar etapa' }).click();
  await expect(page.getByRole('status')).toContainText('Etapa atualizada');
  await expect(page.locator('.status')).toContainText('Em testes');

  await page.getByLabel('Nova etapa *').selectOption('EM_DIAGNOSTICO');
  await page.getByRole('button', { name: 'Atualizar etapa' }).click();
  await expect(page.getByRole('alert')).toContainText('motivo');
  await page.locator('#status-reason').fill('Sintoma reapareceu durante o teste.');
  await page.getByLabel('Texto público opcional').fill('Retornamos ao diagnóstico para uma nova conferência.');
  await page.getByLabel('Observação interna opcional').fill('Rever fixação do agregado dianteiro.');
  await page.getByRole('button', { name: 'Atualizar etapa' }).click();
  await expect(page.locator('.status')).toContainText('Em diagnostico');
  await expect(page.getByText('Rever fixação do agregado dianteiro.')).toBeVisible();

  await page.getByLabel('Texto público', { exact: true }).fill('Diagnóstico complementar iniciado.');
  await page.getByLabel('Publicar para o cliente').check();
  await page.getByLabel('Observação interna', { exact: true }).fill('Aguardar leitura do scanner.');
  await page.getByRole('button', { name: 'Registrar atualização' }).click();
  await expect(page.getByRole('status')).toContainText('publicada');
  await expect(page.getByText('Diagnóstico complementar iniciado.')).toBeVisible();
  await expect(page.getByText('Aguardar leitura do scanner.')).toBeVisible();

  const forecast = new Date(Date.now() + 2 * 86_400_000);
  const localForecast = new Date(forecast.getTime() - forecast.getTimezoneOffset() * 60_000)
    .toISOString().slice(0, 16);
  await page.locator('#forecast-new').fill(localForecast);
  await page.getByLabel('Motivo público *').fill('Atraso do fornecedor');
  await page.getByLabel('Próxima ação *').fill('Confirmar a entrega da peça com o fornecedor.');
  await page.getByRole('button', { name: 'Atualizar previsão' }).click();
  await expect(page.getByRole('status')).toContainText('Previsão atualizada');
  await expect(page.locator('.forecast-history')).toContainText('Atraso do fornecedor');
  await expect(page.getByText('Dentro da previsão')).toBeVisible();

  await page.getByLabel('Ainda sem nova previsão').check();
  await page.getByLabel('Motivo público *').fill('Peça incompatível');
  await page.getByLabel('Próxima ação *').fill('Localizar fornecedor alternativo.');
  await page.getByRole('button', { name: 'Atualizar previsão' }).click();
  await expect(page.getByText('Sem previsão')).toBeVisible();
  await expect(page.locator('.forecast-history')).toContainText('Localizar fornecedor alternativo.');

  await page.getByLabel('Buscar por OS, cliente ou placa').fill('BRA-1E23');
  await page.getByRole('button', { name: 'Buscar' }).click();
  await expect(page.getByRole('heading', { name: 'OS-000001', exact: true })).toBeVisible();
  await page.setViewportSize({ width: 320, height: 760 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
});
