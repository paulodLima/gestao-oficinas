import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface ShopProfile {
  id: string; slug: string; nome: string; telefone: string; emailContato: string;
  endereco: string; horario: string; fuso: string; perfilPublico: boolean; temLogo: boolean; versao: number;
  corMenu: string; corMenuAtivo: string; corDestaque: string;
}
export interface PublicProfile {
  nome: string; telefone: string; emailContato: string; endereco: string; horario: string; fuso: string; logoUrl: string | null;
  corMenu: string; corMenuAtivo: string; corDestaque: string;
}
export interface AddressSuggestion { endereco: string; }
export type ShopFields = Partial<Pick<ShopProfile, 'nome' | 'telefone' | 'emailContato' | 'endereco' | 'horario' | 'fuso' | 'perfilPublico' | 'corMenu' | 'corMenuAtivo' | 'corDestaque'>> & Pick<ShopProfile, 'versao'>;
@Injectable({ providedIn: 'root' })
export class ShopService {
  private readonly http = inject(HttpClient);
  get() { return firstValueFrom(this.http.get<ShopProfile>('/api/oficina')); }
  branding(slug: string) {
    return firstValueFrom(this.http.get<{ nome: string; temLogo: boolean; temCapa: boolean; corMenu: string; corMenuAtivo: string; corDestaque: string }>('/api/publico/oficinas/' + encodeURIComponent(slug) + '/identidade'));
  }
  async saveCover(file: File | null, version: number) {
    const url = '/api/oficina/capa?versao=' + version;
    const headers = await this.headers();
    if (!file) return firstValueFrom(this.http.delete<ShopProfile>(url, { headers }));
    const body = new FormData();
    body.append('arquivo', file);
    return firstValueFrom(this.http.put<ShopProfile>(url, body, { headers }));
  }
  getPublic(slug: string) { return firstValueFrom(this.http.get<PublicProfile>('/api/publico/oficinas/' + encodeURIComponent(slug))); }
  addressSuggestions(query: string) {
    return firstValueFrom(this.http.get<AddressSuggestion[]>('/api/oficina/endereco/sugestoes', {
      params: { q: query }
    }));
  }
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
