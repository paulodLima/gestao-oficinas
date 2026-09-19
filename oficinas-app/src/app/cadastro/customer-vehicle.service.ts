import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface Customer {
  id: string; nome: string; cpf: string; telefone: string; email: string;
  emailVerificadoEm: string | null; ativo: boolean; versao: number;
}
export interface Vehicle {
  id: string; placa: string; marca: string; modelo: string; ano: number | null; cor: string;
  clienteId: string; clienteNome: string; vinculoDesde: string; versao: number;
}
export interface PageResult<T> { items: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface CustomerInput { nome: string; cpf: string; telefone: string; email: string; }
export interface VehicleInput { placa: string; marca: string; modelo: string; ano: number | null; cor: string; clienteId: string; }

@Injectable({ providedIn: 'root' })
export class CustomerVehicleService {
  private readonly http = inject(HttpClient);
  customers(q = '') {
    return firstValueFrom(this.http.get<PageResult<Customer>>('/api/clientes', { params: new HttpParams().set('q', q).set('size', 100) }));
  }
  vehicles(q = '') {
    return firstValueFrom(this.http.get<PageResult<Vehicle>>('/api/veiculos', { params: new HttpParams().set('q', q).set('size', 100) }));
  }
  async createCustomer(input: CustomerInput) {
    return firstValueFrom(this.http.post<Customer>('/api/clientes', input, { headers: await this.headers() }));
  }
  async updateCustomer(customer: Customer, input: CustomerInput) {
    return firstValueFrom(this.http.patch<Customer>('/api/clientes/' + customer.id,
      { ...input, versao: customer.versao }, { headers: await this.headers() }));
  }
  async requestVerification(customerId: string) {
    return firstValueFrom(this.http.post<{ desafioId: string; expiraEm: string }>(
      `/api/clientes/${customerId}/verificacao`, {}, { headers: await this.headers() }));
  }
  async confirmVerification(customerId: string, desafioId: string, codigo: string) {
    return firstValueFrom(this.http.post<Customer>(`/api/clientes/${customerId}/verificacao/confirmacao`,
      { desafioId, codigo }, { headers: await this.headers() }));
  }
  async createVehicle(input: VehicleInput) {
    return firstValueFrom(this.http.post<Vehicle>('/api/veiculos', input, { headers: await this.headers() }));
  }
  async updateVehicle(vehicle: Vehicle, input: Omit<VehicleInput, 'clienteId'>) {
    return firstValueFrom(this.http.patch<Vehicle>('/api/veiculos/' + vehicle.id,
      { ...input, versao: vehicle.versao }, { headers: await this.headers() }));
  }
  async transfer(vehicle: Vehicle, novoClienteId: string) {
    return firstValueFrom(this.http.post<Vehicle>(`/api/veiculos/${vehicle.id}/transferencias`,
      { novoClienteId, expectedVersion: vehicle.versao }, { headers: await this.headers() }));
  }
  private async headers() {
    const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
    return { [csrf.headerName]: csrf.token };
  }
}
