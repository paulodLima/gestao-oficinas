import { test, expect } from '@playwright/test';

test('salva cliente, segue para veículo e abre OS com vínculo atual', async ({ page }) => {
  const customer = { id: 'c-flow', nome: 'Ana Fluxo', cpf: '52998224725', telefone: '', email: '', ativo: true, versao: 0, emailVerificadoEm: null };
  const vehicle = { id: 'v-flow', placa: 'BRA1E23', marca: 'Volkswagen', modelo: 'Polo', ano: null, cor: '', clienteId: customer.id, clienteNome: customer.nome, versao: 0 };
  let rejectSave = true;
  let customerWrites = 0;
  let vehicleWrites = 0;
  await page.route('**/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/api/clientes' && request.method() === 'POST') {
      if (rejectSave) return route.fulfill({ status: 409, json: { detail: 'Confira o cadastro.' } });
      customerWrites++;
      return route.fulfill({ status: 201, json: customer });
    }
    if (path === '/api/veiculos' && request.method() === 'POST') {
      expect(request.postDataJSON().clienteId).toBe(customer.id);
      vehicleWrites++;
      return route.fulfill({ status: 201, json: vehicle });
    }
    let data: unknown = { items: [], totalElements: 0, totalPages: 0 };
    if (path === '/api/auth/me') data = { id: 'owner', nome: 'Dono', oficina: { id: 'office', nome: 'Oficina' } };
    if (path === '/api/auth/csrf') data = { token: 'test', headerName: 'X-CSRF-TOKEN' };
    if (path === '/api/oficina') data = { nome: 'Oficina', temLogo: false };
    if (path === '/api/clientes/' + customer.id) data = customer;
    if (path === '/api/veiculos/' + vehicle.id) data = vehicle;
    await route.fulfill({ json: data });
  });
  await page.goto('/clientes');
  await page.getByRole('button', { name: 'Novo cliente', exact: true }).click();
  await page.getByLabel('Nome completo').fill(customer.nome);
  await page.getByLabel('CPF', { exact: false }).fill(customer.cpf);
  await page.getByRole('button', { name: 'Cadastrar veículo →', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Confira o cadastro.');
  await expect(page).toHaveURL(/clientes$/);
  rejectSave = false;
  await page.getByRole('button', { name: 'Cadastrar veículo →', exact: true }).click();
  await expect(page).toHaveURL(/veiculos\?novo=1&clienteId=c-flow/);
  await expect(page.getByLabel('Responsável *', { exact: true })).toHaveValue(customer.id);
  await page.getByLabel('Placa antiga ou Mercosul').fill(vehicle.placa);
  await page.locator('#vehicle-brand').click();
  await page.getByRole('option', { name: 'Volkswagen', exact: true }).click();
  await page.locator('select#vehicle-model').selectOption('Polo');
  await page.getByRole('button', { name: 'Abrir ordem de serviço →', exact: true }).click();
  await expect(page).toHaveURL(/abrir-ordem\?novo=1&veiculoId=v-flow/);
  await expect(page.getByLabel('Cliente responsável')).toHaveValue(customer.id);
  await expect(page.getByLabel('Veículo *', { exact: true })).toHaveValue(vehicle.id);
  expect(customerWrites).toBe(1);
  expect(vehicleWrites).toBe(1);
});
