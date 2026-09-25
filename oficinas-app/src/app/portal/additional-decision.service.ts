import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export type AdditionalDecision = 'APROVADO' | 'RECUSADO';
export interface AdditionalItem {
  id: string; tipo: string; descricao: string; quantidade: number;
  valorUnitario: number; total: number;
}
export interface AdditionalBlock {
  id: string; grupoDependencia: string | null; total: number; itens: AdditionalItem[];
  decisao: AdditionalDecision | null; decididaEm: string | null;
}
export interface AdditionalVersion {
  id: string; numero: number; estado: string; problema: string; justificativa: string;
  previsaoProposta: string | null; impactoPrazo: string; motivoSubstituicao: string | null;
  total: number; totalAprovado: number; fotoIds: string[]; blocos: AdditionalBlock[]; enviadaEm: string;
  substituidaEm: string | null;
}
export interface AdditionalRequest {
  id: string; ordemServicoId: string; estado: string; versao: number;
  versoes: AdditionalVersion[]; createdAt: string; updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdditionalDecisionService {
  private readonly http = inject(HttpClient);

  list(orderId: string) {
    return firstValueFrom(this.http.get<AdditionalRequest[]>(
      `/api/portal/ordens-servico/${orderId}/adicionais`));
  }

  async requestCode(orderId: string, requestId: string) {
    return firstValueFrom(this.http.post<{ desafioId: string; expiraEm: string }>(
      `/api/portal/ordens-servico/${orderId}/adicionais/${requestId}/codigo`, {},
      { headers: await this.csrfHeaders() }));
  }

  async confirm(orderId: string, request: AdditionalRequest, challengeId: string, code: string,
                decisions: Array<{ bloco: string; decisao: AdditionalDecision }>, comment: string,
                idempotencyKey: string) {
    return firstValueFrom(this.http.post<AdditionalRequest>(
      `/api/portal/ordens-servico/${orderId}/adicionais/${request.id}/decisoes`,
      { desafioId: challengeId, codigo: code, versao: request.versao,
        decisoes: decisions, comentario: comment.trim() || null },
      { headers: { ...(await this.csrfHeaders()), 'Idempotency-Key': idempotencyKey } }));
  }

  private async csrfHeaders() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
