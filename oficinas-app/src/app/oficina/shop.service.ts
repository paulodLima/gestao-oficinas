import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface ShopProfile {
  id: string; slug: string; nome: string; telefone: string; emailContato: string;
  endereco: string; horario: string; fuso: string; perfilPublico: boolean; temLogo: boolean; versao: number;
}
export interface PublicProfile {
  nome: string; telefone: string; emailContato: string; endereco: string; horario: string; fuso: string; logoUrl: string | null;
}
export type ShopFields = Pick<ShopProfile, 'nome' | 'telefone' | 'emailContato' | 'endereco' | 'horario' | 'fuso' | 'perfilPublico' | 'versao'>;
@Injectable({ providedIn: 'root' })
export class ShopService {
  private readonly http = inject(HttpClient);
  get() { return firstValueFrom(this.http.get<ShopProfile>('/api/oficina')); }
  getPublic(slug: string) { return firstValueFrom(this.http.get<PublicProfile>('/api/publico/oficinas/' + encodeURIComponent(slug))); }
  async save(fields: ShopFields) {
    return firstValueFrom(this.http.patch<ShopProfile>('/api/oficina', fields, { headers: await this.headers() }));
  }
  async upload(file: File, version: number) {
    const body = new FormData();
    body.append('arquivo', file);
    return firstValueFrom(this.http.put<ShopProfile>('/api/oficina/logo?versao=' + version, body, { headers: await this.headers() }));
  }
  async remove(version: number) {
    return firstValueFrom(this.http.delete<ShopProfile>('/api/oficina/logo?versao=' + version, { headers: await this.headers() }));
  }
  private async headers() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
