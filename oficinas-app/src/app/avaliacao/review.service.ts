import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface ReviewInput { nota: number; comentario: string | null; consentimentoPublicacao: boolean; }
export interface Review extends ReviewInput { createdAt: string; }
export interface ReviewSummary {
  contexto: string;
  atendimento: { numero: number; entregueEm: string; oficinaNome: string; telefone: string | null; email: string | null; googleUrl: string | null; veiculo: string };
  atualizacoes: { texto: string; createdAt: string }[];
  avaliacao: Review | null; expiraEm: string; textoConsentimento: string;
}
export interface OwnerReview extends Review { id: string; ordemServicoId: string; numero: number; }
export interface ReviewPage { items: OwnerReview[]; page: number; totalPages: number; totalElements: number; }
export interface ReviewInvitation { token: string; expiraEm: string; }

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);
  summary() { return firstValueFrom(this.http.get<ReviewSummary>('/api/portal/avaliacoes/resumo')); }
  async exchange(token: string) { await this.write('POST', '/api/portal/avaliacoes/acesso', { token }); }
  submit(input: ReviewInput & { contexto: string }) { return this.write<Review>('POST', '/api/portal/avaliacoes', input); }
  invitation(order: string) { return this.write<ReviewInvitation>('POST', `/api/ordens-servico/${order}/avaliacao/convite`, {}); }
  revoke(order: string) { return this.write<void>('DELETE', `/api/ordens-servico/${order}/avaliacao/convite`, null); }
  list(page = 0) { return firstValueFrom(this.http.get<ReviewPage>('/api/avaliacoes', { params: { page, size: 20 } })); }
  configuration() { return firstValueFrom(this.http.get<{ googleUrl: string | null }>('/api/avaliacoes/configuracao')); }
  configure(googleUrl: string) { return this.write<{ googleUrl: string | null }>('PATCH', '/api/avaliacoes/configuracao', { googleUrl }); }
  private async write<T>(method: string, url: string, body: unknown): Promise<T> {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return firstValueFrom(this.http.request<T>(method, url, { body, headers: { [csrf.headerName]: csrf.token } }));
  }
}
export function reviewError(error: unknown, fallback: string): string {
  return error instanceof HttpErrorResponse && typeof error.error?.detail === 'string' ? error.error.detail : fallback;
}
