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
  status: ServiceOrderStatus; previsaoEm: string | null; atrasada: boolean; aguardandoRetirada: boolean;
  versao: number; createdAt: string; updatedAt: string;
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
export interface ServiceOrderForecast {
  id: string; previsaoAnterior: string | null; previsaoNova: string | null;
  motivoPublico: string; proximaAcao: string; autorId: string; autorNome: string; createdAt: string;
}
export interface ForecastInput {
  previsao: string | null; motivoPublico: string; proximaAcao: string; expectedVersion: number;
}
export interface ServicePhoto {
  id: string; etapa: ServiceOrderStatus; legenda: string | null; publicada: boolean;
  tipoConteudo: string; tamanhoBytes: number; miniaturaDisponivel: boolean; createdAt: string;
}
export interface Inspection { id: string; numeroVersao: number; estado: 'RASCUNHO' | 'CONFIRMADA'; checklist: Record<string, unknown>; motivoCorrecao: string | null; createdAt: string; updatedAt: string; }

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
  forecasts(id: string) {
    return firstValueFrom(this.http.get<ServiceOrderForecast[]>(`/api/ordens-servico/${id}/previsoes`));
  }
  async updateForecast(id: string, input: ForecastInput) {
    return firstValueFrom(this.http.post<ServiceOrder>(`/api/ordens-servico/${id}/previsao`, input,
      { headers: await this.headers() }));
  }
  photos(id: string) { return firstValueFrom(this.http.get<ServicePhoto[]>(`/api/ordens-servico/${id}/fotos`)); }
  async uploadPhoto(id: string, file: File, stage: ServiceOrderStatus, uploadId: string) {
    const data = new FormData(); data.append('arquivo', file); data.append('etapa', stage);
    data.append('legenda', ''); data.append('publicada', 'false'); data.append('uploadId', uploadId);
    return this.http.post<ServicePhoto>(`/api/ordens-servico/${id}/fotos`, data, { headers: await this.headers(), observe: 'events', reportProgress: true });
  }
  photoUrl(orderId: string, photoId: string, thumbnail = true) { return `/api/ordens-servico/${orderId}/fotos/${photoId}/arquivo?miniatura=${thumbnail}`; }
  async deletePhoto(orderId: string, photoId: string) { return firstValueFrom(this.http.delete<void>(`/api/ordens-servico/${orderId}/fotos/${photoId}`, { headers: await this.headers() })); }
  inspection(id: string) { return firstValueFrom(this.http.get<Inspection[]>(`/api/ordens-servico/${id}/vistoria`)); }
  async saveInspection(id: string, checklist: Record<string, unknown>) { return firstValueFrom(this.http.put<Inspection>(`/api/ordens-servico/${id}/vistoria`, checklist, { headers: await this.headers() })); }
  async confirmInspection(id: string, expectedVersion: number) { return firstValueFrom(this.http.post<Inspection>(`/api/ordens-servico/${id}/vistoria/confirmacoes`, { expectedVersion }, { headers: await this.headers() })); }
  private async headers() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
