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
  styles: [`article{border:1px solid #c8ccc0;padding:32px;background:#fffdf8;overflow-wrap:anywhere}
    img,.monogram{width:96px;height:96px;object-fit:contain;margin-bottom:24px}.monogram{display:grid;place-items:center;background:#253e32;color:#f1a657;font:48px Georgia,serif}
    .eyebrow{font-size:11px;letter-spacing:2px;color:#53604c}h2{font:32px Georgia,serif;margin:12px 0 28px}
    dt{font-size:12px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;margin-top:24px;color:#53604c}dd{margin:8px 0;white-space:pre-line;line-height:1.6}
    @media(max-width:400px){article{padding:20px}h2{font-size:26px}}`]
})
export class ShopIdentityComponent {
  readonly profile = input.required<PublicProfile>();
  readonly imageFailed = signal(false);
}
