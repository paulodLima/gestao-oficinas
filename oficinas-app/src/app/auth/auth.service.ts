import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface Owner { id: string; nome: string; email: string; oficina: { id: string; nome: string }; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  readonly owner = signal<Owner | null>(null);
  async me(): Promise<Owner> {
    const owner = await firstValueFrom(this.http.get<Owner>('/api/auth/me'));
    this.owner.set(owner);
    return owner;
  }
  async post<T>(path: string, body: unknown): Promise<T> {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return firstValueFrom(this.http.post<T>(path, body, { headers: { [csrf.headerName]: csrf.token } }));
  }
  async login(email: string, senha: string): Promise<void> {
    this.owner.set(await this.post<Owner>('/api/auth/login', { email, senha }));
  }
  async logout(): Promise<void> {
    await this.post('/api/auth/logout', {});
    this.owner.set(null);
  }
}

