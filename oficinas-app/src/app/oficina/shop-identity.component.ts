import { Component, input, signal } from '@angular/core';
import { PublicProfile } from './shop.service';

@Component({
  selector: 'app-shop-identity',
  template: `<article aria-label="Identidade da oficina">
    @if (profile().logoUrl && !imageFailed()) {
      <img [src]="profile().logoUrl" [alt]="'Logo de ' + profile().nome" (error)="imageFailed.set(true)">
    } @else { <div class="monogram" aria-label="Oficina sem logo">{{ profile().nome.slice(0, 1).toUpperCase() }}</div> }
    <p class="eyebrow">SUA OFICINA, POR PERTO</p><h2>{{ profile().nome }}</h2>
    <dl><dt>Contato</dt><dd>{{ profile().telefone || 'Telefone não informado' }}</dd>
      @if (profile().emailContato) { <dd>{{ profile().emailContato }}</dd> }
      <dt>Onde estamos</dt><dd>{{ profile().endereco || 'Endereço não informado' }}</dd>
      <dt>Atendimento</dt><dd>{{ profile().horario || 'Consulte a oficina sobre os horários' }}</dd>
      <dt>Fuso horário</dt><dd>{{ profile().fuso }}</dd></dl>
    </article>`,
  styles: [`article{overflow-wrap:anywhere;padding:28px;border:1px solid var(--line);border-radius:12px;background:var(--surface)}img,.monogram{width:76px;height:76px;margin-bottom:20px;border-radius:12px;object-fit:contain}.monogram{display:grid;place-items:center;background:var(--brand-soft);color:var(--brand);font-size:32px;font-weight:800}.eyebrow{margin:0;color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.1em}h2{margin:8px 0 24px;color:var(--ink);font-size:30px;letter-spacing:-.035em}dl{margin:0;border-top:1px solid var(--line)}dt{margin-top:18px;color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.08em;text-transform:uppercase}dd{margin:5px 0;white-space:pre-line;line-height:1.55}@media(max-width:400px){article{padding:20px}h2{font-size:25px}}`]
})
export class ShopIdentityComponent {
  readonly profile = input.required<PublicProfile>();
  readonly imageFailed = signal(false);
}
