import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardCard, durationLabel, STAGES } from './dashboard.service';

@Component({
  selector: 'app-dashboard-card', imports: [RouterLink],
  template: `<article [attr.data-order-id]="card.id" [class.late]="card.atrasada">
    <div class="heading"><a [routerLink]="['/ordens-servico']" [queryParams]="{id: card.id}" [attr.aria-label]="'Abrir OS-' + card.numero">OS-{{ card.numero.toString().padStart(6, '0') }}</a>
      @if(card.atrasada){<span class="alert">Em atraso</span>}</div>
    <h3>{{ card.placa.slice(0,3) }}-{{ card.placa.slice(3) }}</h3><p class="vehicle">{{ card.veiculo }}</p><p>{{ card.clienteNome }}</p>
    <span class="stage">{{ label() }}</span>
    <dl><div><dt>Previsão · estimativa</dt><dd>{{ date(card.previsaoEm) }}</dd></div>
      <div><dt>Última atualização</dt><dd>{{ date(card.ultimaAtualizacao) }}</dd></div>
      <div><dt>Tempo na etapa</dt><dd>{{ duration(card.tempoEtapaSegundos) }}</dd></div></dl>
    @if(card.status === 'PRONTO_PARA_RETIRADA'){<p class="ready">Aguardando retirada</p>}
  </article>`,
  styles: [`:host{display:block;min-width:0}article{background:var(--surface,#fffdf8);border:1px solid var(--line,#c8ccc0);border-top:3px solid var(--green,#254d38);padding:16px;height:100%;box-sizing:border-box;overflow-wrap:anywhere}article.late{border-top-color:var(--danger,#963b25)}.heading{display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap}a{min-height:44px;display:inline-flex;align-items:center;color:var(--green,#254d38);font-weight:bold}a:focus-visible{outline:3px solid #b75a18;outline-offset:3px}.alert{color:var(--danger,#963b25);font-size:13px;font-weight:bold}h3{font:700 23px 'Courier New',monospace;letter-spacing:.06em;margin:12px 0 8px}p{margin:6px 0;line-height:1.5}.vehicle{font-weight:bold}.stage{display:inline-block;background:#eceee5;color:#33452f;padding:6px 8px;font-size:13px;margin:12px 0}dl{margin:4px 0}dl>div{margin-top:12px}dt{font-size:12px;color:#53604c}dd{margin:4px 0;font-size:14px;font-variant-numeric:tabular-nums}.ready{color:#254d38;font-size:13px;font-weight:bold}`]
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
