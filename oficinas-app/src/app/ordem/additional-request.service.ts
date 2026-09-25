import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type AdditionalStatus = 'RASCUNHO' | 'ENVIADA' | 'PARCIALMENTE_DECIDIDA' | 'DECIDIDA' | 'CANCELADA';
export type AdditionalVersionStatus = 'RASCUNHO' | 'ENVIADA' | 'SUBSTITUIDA';
export type AdditionalItemType = 'PECA' | 'MAO_DE_OBRA';

export interface AdditionalItem {
  id: string; tipo: AdditionalItemType; descricao: string; quantidade: number;
  valorUnitario: number; total: number; grupoDependencia: string | null; ordem: number;
}
export interface AdditionalVersion {
  id: string; numero: number; estado: AdditionalVersionStatus; problema: string;
  justificativa: string; previsaoProposta: string | null; impactoPrazo: string;
  motivoSubstituicao: string | null; total: number; fotoIds: string[]; itens: AdditionalItem[];
  enviadaEm: string | null; substituidaEm: string | null; createdAt: string; updatedAt: string;
}
export interface AdditionalRequest {
  id: string; ordemServicoId: string; estado: AdditionalStatus; versao: number;
  motivoCancelamento: string | null; versoes: AdditionalVersion[]; createdAt: string; updatedAt: string;
}
export interface AdditionalItemInput {
  tipo: AdditionalItemType; descricao: string; quantidade: number;
  valorUnitario: number; grupoDependencia: string;
}
export interface AdditionalDraftInput {
  problema: string; justificativa: string; previsaoProposta: string | null;
  impactoPrazo: string; fotoIds: string[]; itens: AdditionalItemInput[]; expectedVersion?: number;
}
@Injectable({ providedIn: 'root' })
export class AdditionalRequestService {
  private readonly http = inject(HttpClient);

  list(orderId: string) {
    return firstValueFrom(this.http.get<AdditionalRequest[]>(this.base(orderId)));
  }

  async create(orderId: string, input: AdditionalDraftInput) {
    return firstValueFrom(this.http.post<AdditionalRequest>(this.base(orderId), input,
      { headers: await this.headers() }));
  }

  async edit(orderId: string, requestId: string, input: AdditionalDraftInput) {
    return firstValueFrom(this.http.patch<AdditionalRequest>(`${this.base(orderId)}/${requestId}`, input,
      { headers: await this.headers() }));
  }

  async send(orderId: string, requestId: string, expectedVersion: number) {
    return firstValueFrom(this.http.post<AdditionalRequest>(`${this.base(orderId)}/${requestId}/envio`,
      { expectedVersion }, { headers: await this.headers() }));
  }

  async replace(orderId: string, requestId: string, input: AdditionalDraftInput,
                reason: string, expectedVersion: number) {
    return firstValueFrom(this.http.post<AdditionalRequest>(`${this.base(orderId)}/${requestId}/substituicoes`,
      { ...input, motivoSubstituicao: reason, expectedVersion }, { headers: await this.headers() }));
  }

  async cancel(orderId: string, requestId: string, reason: string, expectedVersion: number) {
    return firstValueFrom(this.http.post<AdditionalRequest>(`${this.base(orderId)}/${requestId}/cancelamento`,
      { motivo: reason, expectedVersion }, { headers: await this.headers() }));
  }

  private base(orderId: string) { return `/api/ordens-servico/${orderId}/adicionais`; }

  private async headers() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
