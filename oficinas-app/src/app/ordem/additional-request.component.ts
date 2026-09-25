import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ServiceOrder, ServicePhoto } from './service-order.service';
import { AdditionalDraftInput, AdditionalRequest, AdditionalRequestService, AdditionalVersion } from './additional-request.service';

@Component({
  selector: 'app-additional-request',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="additional" aria-labelledby="additional-title">
      <div class="heading"><div><p class="step">06 / ADICIONAIS</p><h3 id="additional-title">Serviços adicionais</h3></div><span>{{ requests().length }} solicitação(ões)</span></div>
      <p class="hint">Prepare peças e mão de obra com valores transparentes. O envio congela a versão apresentada ao cliente.</p>
      @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
      @if (success()) { <p class="notice success" role="status">{{ success() }}</p> }

      @if (requests().length) {
        <div class="request-list">
          @for (request of requests(); track request.id) {
            <article class="request-card">
              <div><b>{{ currentVersion(request).problema }}</b><small>Versão {{ currentVersion(request).numero }} · {{ statusLabel(request.estado) }}</small></div>
              <strong>{{ money(currentVersion(request).total) }}</strong>
              <div class="request-actions">
                @if (request.estado === 'RASCUNHO') { <button type="button" (click)="edit(request)">Editar rascunho</button><button type="button" class="primary" (click)="send(request)">Enviar ao cliente</button> }
                @if (request.estado === 'ENVIADA') { <button type="button" (click)="beginReplacement(request)">Criar nova versão</button> }
                @if (!['CANCELADA','DECIDIDA'].includes(request.estado)) { <button type="button" class="danger" (click)="beginCancellation(request.id)">Cancelar</button> }
              </div>
              @if (cancelingId() === request.id) {
                <div class="inline-action"><label [for]="'cancel-reason-' + request.id">Motivo do cancelamento</label><input [id]="'cancel-reason-' + request.id" [formControl]="cancelReason" maxlength="1000"><button type="button" class="danger" [disabled]="cancelReason.invalid" (click)="cancel(request)">Confirmar cancelamento</button></div>
              }
              <details><summary>Histórico de versões</summary>
                @for (version of request.versoes; track version.id) {
                  <div class="version"><span>v{{ version.numero }} · {{ versionLabel(version.estado) }}</span><b>{{ money(version.total) }}</b><small>{{ version.itens.length }} item(ns) · {{ formatDate(version.enviadaEm || version.createdAt) }}</small>@if (version.motivoSubstituicao) { <small>Substituída: {{ version.motivoSubstituicao }}</small> }</div>
                }
              </details>
            </article>
          }
        </div>
      }

      @if (!editing()) { <button type="button" class="new-request" (click)="startNew()">+ Nova solicitação adicional</button> }
      @if (editing()) {
        <form [formGroup]="form" (ngSubmit)="save()" class="editor">
          <div class="editor-title"><div><strong>{{ replacingId() ? 'Nova versão substituta' : editingId() ? 'Editar rascunho' : 'Nova solicitação' }}</strong><small>Total estimado: {{ money(previewTotal()) }}</small></div><button type="button" (click)="closeEditor()">Fechar</button></div>
          @if (replacingId()) { <label for="replacement-reason">Motivo da substituição *</label><textarea id="replacement-reason" [formControl]="replacementReason" rows="2" maxlength="1000"></textarea> }
          <label for="additional-problem">Problema encontrado *</label><textarea id="additional-problem" formControlName="problema" rows="3" maxlength="2000"></textarea>
          <label for="additional-justification">Justificativa *</label><textarea id="additional-justification" formControlName="justificativa" rows="3" maxlength="2000"></textarea>
          <div class="pair"><div><label for="additional-forecast">Previsão proposta</label><input id="additional-forecast" type="datetime-local" formControlName="previsaoProposta"></div><div><label for="additional-impact">Impacto no prazo *</label><input id="additional-impact" formControlName="impactoPrazo" maxlength="1000" placeholder="Ex.: acrescenta 2 dias úteis"></div></div>

          <fieldset class="items"><legend>Itens da solicitação</legend>
            <div formArrayName="itens">
              @for (item of items.controls; track $index; let index = $index) {
                <div class="item" [formGroupName]="index">
                  <div class="item-title"><b>Item {{ index + 1 }}</b>@if (items.length > 1) { <button type="button" (click)="removeItem(index)">Remover</button> }</div>
                  <div class="pair"><div><label [for]="'item-type-' + index">Tipo</label><select [id]="'item-type-' + index" formControlName="tipo"><option value="PECA">Peça</option><option value="MAO_DE_OBRA">Mão de obra</option></select></div><div><label [for]="'item-description-' + index">Descrição *</label><input [id]="'item-description-' + index" formControlName="descricao" maxlength="500"></div></div>
                  <div class="triple"><div><label [for]="'item-quantity-' + index">Quantidade *</label><input [id]="'item-quantity-' + index" formControlName="quantidade" inputmode="decimal"></div><div><label [for]="'item-value-' + index">Valor unitário *</label><input [id]="'item-value-' + index" formControlName="valorUnitario" inputmode="decimal"></div><div><label [for]="'item-group-' + index">Grupo dependente</label><input [id]="'item-group-' + index" formControlName="grupoDependencia" maxlength="60" placeholder="Ex.: kit-freio"></div></div>
                </div>
              }
            </div>
            <button type="button" (click)="addItem()">+ Adicionar item</button>
          </fieldset>

          @if (publicPhotos().length) {
            <fieldset class="photo-options"><legend>Fotos públicas opcionais</legend>
              @for (photo of publicPhotos(); track photo.id) { <label><input type="checkbox" [checked]="selectedPhotos().includes(photo.id)" (change)="togglePhoto(photo.id, $event)"> {{ photo.legenda || 'Foto ' + photo.etapa }}</label> }
            </fieldset>
          } @else { <p class="hint">Publique fotos da OS para anexá-las ao envio.</p> }
          <button type="submit" class="primary save" [disabled]="busy()">{{ busy() ? 'Salvando…' : replacingId() ? 'Criar versão em rascunho' : 'Salvar rascunho' }}</button>
        </form>
      }
    </section>
  `,
  styles: `
    :host{display:block}.additional{margin-top:22px;padding-top:18px;border-top:1px solid var(--line)}.heading,.editor-title,.item-title{display:flex;align-items:center;justify-content:space-between;gap:12px}.heading span,.hint,.request-card small,.editor-title small,.version small{color:var(--muted);font-size:12px}.step{margin:0 0 7px;color:var(--muted);font-size:11px;font-weight:750;letter-spacing:.08em}h3{margin:0;font-size:15px}.notice{padding:10px;border-radius:7px;font-size:12px}.notice.error{color:#963737;background:#fff1f1}.notice.success{color:#1d704e;background:#eaf8f0}.request-list{display:grid;gap:9px;margin-top:14px}.request-card{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:10px;padding:13px;border:1px solid var(--line);border-radius:9px;background:#fbfcff}.request-card>div:first-child{display:grid;gap:3px}.request-actions{grid-column:1/-1;display:flex;flex-wrap:wrap;gap:7px}.request-card button,.editor button{min-height:40px;padding:8px 11px;border:1px solid var(--line);border-radius:7px;background:#fff;color:var(--ink);font-weight:700;cursor:pointer}.request-card .primary,.editor .primary{border-color:var(--brand);background:var(--brand);color:#fff}.request-card .danger,.inline-action .danger{color:var(--danger)}details{grid-column:1/-1}summary{cursor:pointer;color:var(--brand);font-size:12px;font-weight:750}.version{display:grid;grid-template-columns:1fr auto;gap:3px 8px;padding:9px 0;border-bottom:1px solid var(--line);font-size:12px}.version small{grid-column:1/-1}.inline-action{grid-column:1/-1;padding:10px;border-radius:7px;background:#fff8e8}.inline-action button{margin-top:7px}.new-request{width:100%;min-height:44px;margin-top:14px;border:1px dashed #aebbd0;border-radius:8px;background:#f8faff;color:var(--brand);font-weight:750;cursor:pointer}.editor{margin-top:14px;padding:15px;border:1px solid #b7c6df;border-radius:10px;background:#f8faff}.editor-title{padding-bottom:10px;border-bottom:1px solid var(--line)}.editor-title>div{display:grid;gap:3px}label{display:block;margin:13px 0 5px;color:#4f5b6d;font-size:12px;font-weight:750}input,select,textarea{display:block;width:100%;min-width:0;min-height:42px;padding:9px 10px;border:1px solid var(--line);border-radius:7px;background:#fff;color:var(--ink)}textarea{resize:vertical}.pair,.triple{display:grid;grid-template-columns:1fr 1fr;gap:9px}.triple{grid-template-columns:.7fr 1fr 1fr}.items,.photo-options{margin:16px 0 0;padding:12px;border:1px solid var(--line);border-radius:8px}.items legend,.photo-options legend{padding:0 5px;font-size:12px;font-weight:800}.item{margin-bottom:12px;padding-bottom:12px;border-bottom:1px solid var(--line)}.item-title button{min-height:30px;padding:3px 7px;color:var(--danger)}.photo-options label{display:flex;align-items:center;gap:8px;min-height:40px;margin:0}.photo-options input{width:18px;min-height:18px}.save{width:100%;margin-top:15px}@media(max-width:620px){.pair,.triple{grid-template-columns:1fr}.request-card{grid-template-columns:1fr}.request-card>strong{grid-row:2}.request-actions{grid-column:1}.editor{padding:12px}}
  `
})
export class AdditionalRequestComponent implements OnChanges {
  @Input({ required: true }) order!: ServiceOrder;
  @Input() photos: ServicePhoto[] = [];
  private readonly service = inject(AdditionalRequestService);
  private readonly builder = inject(FormBuilder);
  readonly requests = signal<AdditionalRequest[]>([]);
  readonly busy = signal(false);
  readonly editing = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly replacingId = signal<string | null>(null);
  readonly cancelingId = signal<string | null>(null);
  readonly selectedPhotos = signal<string[]>([]);
  readonly error = signal('');
  readonly success = signal('');
  readonly replacementReason = this.builder.nonNullable.control('', [Validators.required, Validators.maxLength(1000)]);
  readonly cancelReason = this.builder.nonNullable.control('', [Validators.required, Validators.maxLength(1000)]);
  readonly form = this.builder.nonNullable.group({
    problema: ['', [Validators.required, Validators.maxLength(2000)]],
    justificativa: ['', [Validators.required, Validators.maxLength(2000)]],
    previsaoProposta: [''], impactoPrazo: ['', [Validators.required, Validators.maxLength(1000)]],
    itens: this.builder.array([this.itemForm()])
  });

  get items() { return this.form.controls.itens as FormArray; }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['order']) void this.load();
  }

  async load() {
    try { this.requests.set(await this.service.list(this.order.id)); }
    catch (error) { this.showError(error); }
  }

  startNew() { this.resetEditor(); this.editing.set(true); }
  closeEditor() { this.editing.set(false); this.editingId.set(null); this.replacingId.set(null); }
  addItem() { this.items.push(this.itemForm()); }
  removeItem(index: number) { if (this.items.length > 1) this.items.removeAt(index); }

  edit(request: AdditionalRequest) {
    this.fill(this.currentVersion(request));
    this.editingId.set(request.id);
    this.replacingId.set(null);
    this.editing.set(true);
  }

  beginReplacement(request: AdditionalRequest) {
    this.fill(this.currentVersion(request));
    this.editingId.set(null);
    this.replacingId.set(request.id);
    this.replacementReason.setValue('');
    this.editing.set(true);
  }

  beginCancellation(id: string) { this.cancelingId.set(id); this.cancelReason.setValue(''); }

  async save() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.replacingId() && this.replacementReason.invalid) {
      this.error.set('Confira os dados, itens e valores da solicitação.'); return;
    }
    const current = this.requests().find(item => item.id === (this.editingId() || this.replacingId()));
    const input = this.input();
    const message = this.replacingId() ? 'Nova versão criada em rascunho.' : 'Rascunho salvo.';
    await this.perform(async () => {
      if (this.replacingId() && current) {
        await this.service.replace(this.order.id, current.id, input,
          this.replacementReason.value, current.versao);
      } else if (this.editingId() && current) {
        await this.service.edit(this.order.id, current.id, { ...input, expectedVersion: current.versao });
      } else {
        await this.service.create(this.order.id, input);
      }
      await this.load(); this.closeEditor();
    }, message);
  }

  async send(request: AdditionalRequest) {
    await this.perform(async () => {
      await this.service.send(this.order.id, request.id, request.versao);
      await this.load();
    }, 'Versão enviada e congelada para o cliente.');
  }

  async cancel(request: AdditionalRequest) {
    this.cancelReason.markAsTouched();
    if (this.cancelReason.invalid) return;
    await this.perform(async () => {
      await this.service.cancel(this.order.id, request.id, this.cancelReason.value, request.versao);
      await this.load(); this.cancelingId.set(null);
    }, 'Solicitação cancelada com histórico preservado.');
  }

  togglePhoto(id: string, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedPhotos.update(items => checked ? [...items, id] : items.filter(item => item !== id));
  }

  previewTotal() {
    return this.items.controls.reduce((total, control) => {
      const value = control.value as { quantidade?: string; valorUnitario?: string };
      return total + this.decimal(value.quantidade) * this.decimal(value.valorUnitario);
    }, 0);
  }

  publicPhotos() { return this.photos.filter(photo => photo.publicada); }

  currentVersion(request: AdditionalRequest) { return request.versoes.at(-1)!; }
  money(value: number) { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value); }
  formatDate(value: string | null) { return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : 'Não enviada'; }
  statusLabel(value: string) { return ({ RASCUNHO: 'Rascunho', ENVIADA: 'Enviada', PARCIALMENTE_DECIDIDA: 'Parcialmente decidida', DECIDIDA: 'Decidida', CANCELADA: 'Cancelada' } as Record<string, string>)[value] ?? value; }
  versionLabel(value: string) { return ({ RASCUNHO: 'Rascunho', ENVIADA: 'Enviada e congelada', SUBSTITUIDA: 'Substituída' } as Record<string, string>)[value] ?? value; }

  private itemForm() {
    return this.builder.nonNullable.group({ tipo: ['PECA' as const, Validators.required],
      descricao: ['', [Validators.required, Validators.maxLength(500)]],
      quantidade: ['1.000', [Validators.required, Validators.pattern(/^\d+(?:[.,]\d{1,3})?$/)]],
      valorUnitario: ['0.00', [Validators.required, Validators.pattern(/^\d+(?:[.,]\d{1,2})?$/)]],
      grupoDependencia: ['', Validators.maxLength(60)] });
  }

  private input(): AdditionalDraftInput {
    const value = this.form.getRawValue();
    return { problema: value.problema.trim(), justificativa: value.justificativa.trim(),
      previsaoProposta: value.previsaoProposta ? new Date(value.previsaoProposta).toISOString() : null,
      impactoPrazo: value.impactoPrazo.trim(), fotoIds: this.selectedPhotos(),
      itens: value.itens.map(item => ({ tipo: item.tipo, descricao: item.descricao.trim(),
        quantidade: this.decimal(item.quantidade),
        valorUnitario: this.decimal(item.valorUnitario),
        grupoDependencia: item.grupoDependencia.trim() })) };
  }

  private fill(version: AdditionalVersion) {
    this.items.clear();
    version.itens.forEach(item => this.items.push(this.builder.nonNullable.group({ tipo: [item.tipo, Validators.required],
      descricao: [item.descricao, [Validators.required, Validators.maxLength(500)]],
      quantidade: [item.quantidade.toFixed(3), [Validators.required, Validators.pattern(/^\d+(?:[.,]\d{1,3})?$/)]],
      valorUnitario: [item.valorUnitario.toFixed(2), [Validators.required, Validators.pattern(/^\d+(?:[.,]\d{1,2})?$/)]],
      grupoDependencia: [item.grupoDependencia ?? '', Validators.maxLength(60)] })));
    this.form.patchValue({ problema: version.problema, justificativa: version.justificativa,
      previsaoProposta: version.previsaoProposta ? this.localDateTime(version.previsaoProposta) : '',
      impactoPrazo: version.impactoPrazo });
    this.selectedPhotos.set([...version.fotoIds]);
  }

  private resetEditor() {
    this.items.clear(); this.items.push(this.itemForm()); this.form.reset({ problema: '', justificativa: '',
      previsaoProposta: '', impactoPrazo: '' }); this.selectedPhotos.set([]); this.error.set(''); this.success.set('');
  }

  private localDateTime(value: string) {
    const date = new Date(value); return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
  }

  private decimal(value?: string) { return Number((value ?? '').replace(',', '.')) || 0; }

  private async perform(action: () => Promise<void>, message: string) {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.success.set('');
    try { await action(); this.success.set(message); }
    catch (error) { this.showError(error); }
    finally { this.busy.set(false); }
  }

  private showError(error: unknown) {
    this.error.set(error instanceof HttpErrorResponse ? error.error?.detail ?? 'Não foi possível concluir.' : 'Não foi possível concluir.');
  }
}
