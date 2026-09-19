import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PageResult } from '../cadastro/customer-vehicle.service';

export type ServiceOrderStatus = 'RECEBIDO' | 'EM_DIAGNOSTICO' | 'AGUARDANDO_APROVACAO' |
  'AGUARDANDO_PECAS' | 'EM_MANUTENCAO' | 'EM_MONTAGEM' | 'EM_TESTES' |
  'PRONTO_PARA_RETIRADA' | 'ENTREGUE' | 'CANCELADO' | 'FUNILARIA' | 'PINTURA';

export interface ServiceOrder {
  id: string; numero: number; clienteId: string; clienteNome: string; veiculoId: string;
  placa: string; veiculo: string; relatoInicial: string; entradaEm: string; kmEntrada: number;
  status: ServiceOrderStatus; previsaoEm: string | null; versao: number; createdAt: string;
}
export interface ServiceOrderInput {
  clienteId: string; veiculoId: string; relatoInicial: string; entradaEm: string;
  kmEntrada: number; previsaoEm: string | null;
}
export interface ServiceOrderEvent {
  id: string; tipo: 'STATUS' | 'ATUALIZACAO'; statusAnterior: ServiceOrderStatus | null;
  statusNovo: ServiceOrderStatus | null; motivo: string | null; textoPublico: string | null;
  textoInterno: string | null; publicada: boolean; autorId: string; autorNome: string; createdAt: string;
}
export interface StatusInput {
  status: ServiceOrderStatus; motivo: string; textoPublico: string;
  textoInterno: string; expectedVersion: number;
}
export interface UpdateInput {
  textoPublico: string; textoInterno: string; publicada: boolean; expectedVersion: number;
}

@Injectable({ providedIn: 'root' })
export class ServiceOrderService {
  private readonly http = inject(HttpClient);
  orders(q = '') {
    const params = new HttpParams().set('q', q).set('size', 100);
    return firstValueFrom(this.http.get<PageResult<ServiceOrder>>('/api/ordens-servico', { params }));
  }
  order(id: string) {
    return firstValueFrom(this.http.get<ServiceOrder>(`/api/ordens-servico/${id}`));
  }
  async create(input: ServiceOrderInput) {
    return firstValueFrom(this.http.post<ServiceOrder>('/api/ordens-servico', input,
      { headers: { ...await this.headers(), 'Idempotency-Key': crypto.randomUUID() } }));
  }
  timeline(id: string) {
    return firstValueFrom(this.http.get<ServiceOrderEvent[]>(`/api/ordens-servico/${id}/atualizacoes`));
  }
  async changeStatus(id: string, input: StatusInput) {
    return firstValueFrom(this.http.post<ServiceOrder>(`/api/ordens-servico/${id}/status`, input,
      { headers: await this.headers() }));
  }
  async publish(id: string, input: UpdateInput) {
    return firstValueFrom(this.http.post<ServiceOrderEvent>(`/api/ordens-servico/${id}/atualizacoes`, input,
      { headers: await this.headers() }));
  }
  private async headers() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
