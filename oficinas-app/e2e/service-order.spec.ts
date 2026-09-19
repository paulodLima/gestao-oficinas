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
  await page.getByRole('link', { name: 'Cadastrar clientes e veículos' }).click();

  await page.getByLabel('Nome completo *').fill('Ana Souza');
  await page.getByLabel('CPF *').fill('529.982.247-25');
  await page.getByRole('button', { name: 'Cadastrar cliente' }).click();
  await expect(page.getByRole('status')).toContainText('Cliente cadastrado');
  await page.getByRole('tab', { name: 'Veículos' }).click();
  await page.getByLabel('Placa antiga ou Mercosul *').fill('BRA-1E23');
  await page.getByLabel('Marca *').fill('Volkswagen');
  await page.getByLabel('Modelo *').fill('T-Cross');
  await page.getByLabel('Responsável *').selectOption({ label: 'Ana Souza' });
  await page.getByRole('button', { name: 'Cadastrar veículo' }).click();
  await expect(page.getByRole('status')).toContainText('Veículo cadastrado');

  await page.getByRole('link', { name: 'Voltar ao início' }).click();
  await page.getByRole('link', { name: 'Abrir e consultar ordens de serviço' }).click();
  await page.getByLabel('Cliente responsável *').selectOption({ label: 'Ana Souza' });
  await page.getByLabel('Veículo *').selectOption({ label: 'BRA-1E23 · Volkswagen T-Cross' });
  await page.getByLabel('Relato inicial *').fill('Ruído na suspensão dianteira ao passar em desníveis.');
  await page.getByLabel('Quilometragem *').fill('48210');
  await page.getByRole('button', { name: 'Abrir ordem de serviço' }).click();
  await expect(page.getByRole('status')).toContainText('Ordem de serviço aberta');
  await expect(page.getByRole('heading', { name: 'OS-000001' })).toBeVisible();
  await expect(page.getByText('48.210 km')).toBeVisible();

  await page.getByLabel('Buscar por OS, cliente ou placa').fill('BRA-1E23');
  await page.getByRole('button', { name: 'Buscar' }).click();
  await expect(page.getByRole('heading', { name: 'OS-000001', exact: true })).toBeVisible();
  await page.setViewportSize({ width: 320, height: 760 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
});
