import { test, expect } from '@playwright/test';

test('cadastro, busca e troca de responsável preservada', async ({ page }, info) => {
  const email = `cadastro-${Date.now()}-${info.project.name}@example.test`;
  await page.goto('/cadastro');
  await page.getByLabel('Seu nome', { exact: true }).fill('Dono Cadastro');
  await page.getByLabel('Nome da oficina').fill('Oficina Cadastro');
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill('Oficina-segura-123');
  await page.getByLabel('Confirmar senha').fill('Oficina-segura-123');
  await page.getByRole('button', { name: 'Criar minha conta' }).click();
  await page.getByRole('link', { name: /Ir para o login/ }).click();
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill('Oficina-segura-123');
  await page.getByRole('button', { name: 'Entrar na oficina' }).click();
  await page.getByRole('link', { name: 'Clientes', exact: true }).click();
  await expect(page.getByRole('heading', { name: /Clientes em ordem/ })).toBeVisible();

  await page.getByLabel('Nome completo *').fill('Ana Souza');
  await page.getByLabel('CPF *').fill('529.982.247-25');
  await page.getByLabel('Telefone', { exact: true }).fill('(61) 99999-0000');
  await page.getByLabel('E-mail', { exact: true }).fill('ana@example.test');
  await page.getByRole('button', { name: 'Cadastrar cliente' }).click();
  await expect(page.getByRole('status')).toContainText('Cliente cadastrado');

  await page.getByRole('button', { name: 'Novo cliente' }).click();
  await page.getByLabel('Nome completo *').fill('Bruno Lima');
  await page.getByLabel('CPF *').fill('168.995.350-09');
  await page.getByLabel('E-mail', { exact: true }).fill('bruno@example.test');
  await page.getByRole('button', { name: 'Cadastrar cliente' }).click();
  await expect(page.getByRole('status')).toContainText('Cliente cadastrado');

  await page.getByRole('link', { name: 'Veículos', exact: true }).click();
  await page.getByLabel('Placa antiga ou Mercosul *').fill('bra-1e23');
  await page.getByLabel('Marca *').fill('Volkswagen');
  await page.getByLabel('Modelo *').fill('T-Cross');
  await page.getByLabel('Ano').fill('2024');
  await page.getByLabel('Cor').fill('Cinza');
  await page.getByLabel('Responsável *').selectOption({ label: 'Ana Souza' });
  await page.getByRole('button', { name: 'Cadastrar veículo' }).click();
  await expect(page.getByRole('status')).toContainText('Veículo cadastrado');
  await expect(page.getByText('BRA-1E23')).toBeVisible();

  await page.getByLabel('Novo responsável').selectOption({ label: 'Bruno Lima' });
  await page.getByRole('button', { name: 'Registrar troca' }).click();
  await expect(page.getByRole('status')).toContainText('vínculo anterior foi preservado');
  await expect(page.getByText('Responsável: Bruno Lima')).toBeVisible();

  await page.getByLabel('Buscar por placa, veículo ou cliente').fill('BRA1E23');
  await page.getByRole('button', { name: 'Buscar' }).click();
  await expect(page.getByText('BRA-1E23')).toBeVisible();
  await page.setViewportSize({ width: 320, height: 760 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
});
