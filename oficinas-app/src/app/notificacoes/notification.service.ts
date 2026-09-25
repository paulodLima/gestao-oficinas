import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface Notice {
  id: string; ordemServicoId: string; evento: string; titulo: string; mensagem: string;
  lida: boolean; createdAt: string; emailEstado: 'PENDENTE' | 'ENVIADO' | 'FALHOU' | 'CANCELADO' | 'SEM_EMAIL_VERIFICADO';
  tentativas: number; proximaTentativaEm: string | null; ultimoErro: string | null;
}
export interface NoticePage { items: Notice[]; page: number; size: number; totalElements: number; totalPages: number; }

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  list(page = 0, unread = false) {
    return firstValueFrom(this.http.get<NoticePage>('/api/notificacoes', {
      params: { page, size: 20, naoLidas: unread }
    }));
  }
  async read(id: string) {
    return firstValueFrom(this.http.patch<void>(`/api/notificacoes/${id}`, { lida: true }, { headers: await this.headers() }));
  }
  async retry(id: string) {
    return firstValueFrom(this.http.post<void>(`/api/notificacoes/${id}/reenvio`, {}, { headers: await this.headers() }));
  }
  private async headers() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
