import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { PageResult } from '../cadastro/customer-vehicle.service';
import { ServiceOrderStatus } from '../ordem/service-order.service';

export type Situation = 'ATIVAS' | 'ATRASADAS' | 'APROVACAO' | 'PECAS' | 'PRONTAS';
export interface DashboardCard {
  id: string; numero: number; clienteNome: string; placa: string; veiculo: string; status: ServiceOrderStatus;
  previsaoEm: string | null; atrasada: boolean; ultimaAtualizacao: string; etapaDesde: string;
  tempoEtapaSegundos: number; versao: number;
}
export interface DashboardSnapshot {
  indicadores: { ativas: number; atrasadas: number; aprovacoes: number; pecas: number; prontas: number };
  ordens: PageResult<DashboardCard>; fuso: string; verificadoEm: string;
}
export interface DashboardFilters {
  q: string; situacao: string; status: string; sort: string; semAtualizacaoHoras: string; minHorasEtapa: string;
}
export const STAGES: { value: ServiceOrderStatus; label: string }[] = [
  { value: 'RECEBIDO', label: 'Recebido' }, { value: 'EM_DIAGNOSTICO', label: 'Em diagnóstico' },
  { value: 'AGUARDANDO_APROVACAO', label: 'Aguardando aprovação' }, { value: 'AGUARDANDO_PECAS', label: 'Aguardando peças' },
  { value: 'EM_MANUTENCAO', label: 'Em manutenção' }, { value: 'FUNILARIA', label: 'Funilaria' },
  { value: 'PINTURA', label: 'Pintura' }, { value: 'EM_MONTAGEM', label: 'Em montagem' },
  { value: 'EM_TESTES', label: 'Em testes' }, { value: 'PRONTO_PARA_RETIRADA', label: 'Pronto para retirada' }
];
export function groupCards(cards: DashboardCard[]) {
  return STAGES.map(stage => ({ ...stage, cards: cards.filter(card => card.status === stage.value) }));
}
export function durationLabel(seconds: number): string {
  const minutes = Math.floor(Math.max(0, seconds) / 60);
  if (minutes < 1) return 'menos de 1 min';
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  return hours < 24 ? `${hours} h ${minutes % 60} min` : `${Math.floor(hours / 24)} d ${hours % 24} h`;
}
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  get(filters: DashboardFilters, page: number) {
    return firstValueFrom(this.http.get<DashboardSnapshot>('/api/painel', { params: { ...filters, page, size: 20 } }));
  }
}
