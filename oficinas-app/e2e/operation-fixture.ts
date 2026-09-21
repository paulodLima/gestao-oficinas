import { APIRequestContext, expect } from '@playwright/test';
import { ServiceOrder } from '../src/app/ordem/service-order.service';

export async function post(request: APIRequestContext, path: string, data: unknown) {
  const csrf = await (await request.get('/api/auth/csrf')).json();
  const response = await request.post(path, { data, headers: { [csrf.headerName]: csrf.token } });
  expect(response.ok(), `${path}: ${response.status()}`).toBeTruthy();
  return response.json();
}

export async function operationFixture(request: APIRequestContext): Promise<ServiceOrder> {
  const email = `operacao-${Date.now()}-${Math.random().toString(36).slice(2)}@example.test`;
  await post(request, '/api/auth/cadastro', { nome: 'Dona Operação', nomeOficina: 'Oficina de Teste', email, senha: 'Oficina-segura-123' });
  await post(request, '/api/auth/login', { email, senha: 'Oficina-segura-123' });
  const customer = await post(request, '/api/clientes', { nome: 'Cliente Prazo', cpf: '52998224725', telefone: '61999990000', email: '' });
  const vehicle = await post(request, '/api/veiculos', { clienteId: customer.id, placa: 'BRA1E23', marca: 'Fiat', modelo: 'Uno', ano: 2020, cor: 'Branco' });
  return post(request, '/api/ordens-servico', { clienteId: customer.id, veiculoId: vehicle.id,
    relatoInicial: 'Conferir ruído no motor.', entradaEm: new Date(Date.now() - 86400000).toISOString(),
    kmEntrada: 25000, previsaoEm: new Date(Date.now() - 3600000).toISOString() });
}
