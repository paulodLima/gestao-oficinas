import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardCard, durationLabel, STAGES } from './dashboard.service';

@Component({
  selector: 'app-dashboard-card', imports: [RouterLink],
  template: `<article [attr.data-order-id]="card.id" [class.late]="card.atrasada">
    <div class="heading"><a [routerLink]="['/abrir-ordem']" [queryParams]="{id: card.id}" [attr.aria-label]="'Abrir OS-' + card.numero">OS-{{ card.numero.toString().padStart(6, '0') }}</a>
      @if(card.atrasada){<span class="alert">Em atraso</span>}</div>
    <h3>{{ card.placa.slice(0,3) }}-{{ card.placa.slice(3) }}</h3><p class="vehicle">{{ card.veiculo }}</p><p>{{ card.clienteNome }}</p>
    <span class="stage">{{ label() }}</span>
    <dl><div><dt>Previsão · estimativa</dt><dd>{{ date(card.previsaoEm) }}</dd></div>
      <div><dt>Última atualização</dt><dd>{{ date(card.ultimaAtualizacao) }}</dd></div>
      <div><dt>Tempo na etapa</dt><dd>{{ duration(card.tempoEtapaSegundos) }}</dd></div></dl>
    @if(card.status === 'PRONTO_PARA_RETIRADA'){<p class="ready">Aguardando retirada</p>}
  </article>`,
  styles: [`:host{display:block;min-width:0}article{height:100%;padding:16px;border:1px solid var(--line);border-top:3px solid var(--brand);border-radius:8px;background:var(--surface);overflow-wrap:anywhere}article.late{border-top-color:var(--danger)}.heading{display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap}a{min-height:36px;display:inline-flex;align-items:center;color:var(--brand);font-size:13px;font-weight:800;text-decoration:none}.alert{color:var(--danger);font-size:12px;font-weight:750}h3{margin:10px 0 4px;font:750 19px ui-monospace,SFMono-Regular,Menlo,monospace;letter-spacing:.04em}p{margin:4px 0;color:var(--muted);font-size:13px;line-height:1.45}.vehicle{color:var(--ink);font-weight:700}.stage{display:inline-block;margin:12px 0 4px;padding:4px 7px;border-radius:5px;background:var(--brand-soft);color:var(--brand);font-size:11px;font-weight:750}dl{margin:8px 0 0}dl>div{margin-top:9px}dt{color:var(--muted);font-size:11px}dd{margin:2px 0;color:var(--ink);font-size:12px;font-variant-numeric:tabular-nums}.ready{color:var(--success);font-size:12px;font-weight:750}`]
})
export class DashboardCardComponent {
  @Input({ required: true }) card!: DashboardCard;
  @Input() timezone = 'America/Sao_Paulo';
  readonly duration = durationLabel;
  date(value: string | null) {
    return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: this.timezone }).format(new Date(value)) : 'Sem previsão';
  }
  label() { return STAGES.find(stage => stage.value === this.card.status)?.label ?? this.card.status; }
}
