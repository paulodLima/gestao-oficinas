import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Customer, CustomerVehicleService, Vehicle } from '../cadastro/customer-vehicle.service';
import { FuelLevel, Inspection, InspectionChecklist, ServiceOrder, ServiceOrderEvent, ServiceOrderForecast, ServiceOrderInput, ServiceOrderService, ServicePhoto,
  ServiceOrderStatus } from './service-order.service';
import { AdditionalRequestComponent } from './additional-request.component';
import { OrderShareComponent } from './order-share.component';
import { OrderClosureComponent } from './order-closure.component';

const ACTIVE_STATUSES: ServiceOrderStatus[] = ['RECEBIDO', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
  'AGUARDANDO_PECAS', 'EM_MANUTENCAO', 'FUNILARIA', 'PINTURA', 'EM_MONTAGEM', 'EM_TESTES',
  'PRONTO_PARA_RETIRADA'];
const STATUS_SEQUENCE: Record<ServiceOrderStatus, number> = {
  RECEBIDO: 0, EM_DIAGNOSTICO: 10, AGUARDANDO_APROVACAO: 20, AGUARDANDO_PECAS: 30,
  EM_MANUTENCAO: 40, FUNILARIA: 42, PINTURA: 44, EM_MONTAGEM: 50, EM_TESTES: 60,
  PRONTO_PARA_RETIRADA: 70, ENTREGUE: 100, CANCELADO: 100
};

@Component({
  selector: 'app-service-order-page',
  imports: [ReactiveFormsModule, AdditionalRequestComponent, OrderShareComponent, OrderClosureComponent],
  templateUrl: './service-order-page.component.html',
  styleUrls: ['./service-order-page.component.css', './inspection.css']
})
export class ServiceOrderPageComponent implements OnInit {
  private readonly service = inject(ServiceOrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly registrations = inject(CustomerVehicleService);
  private readonly builder = inject(FormBuilder);
  readonly orders = signal<ServiceOrder[]>([]);
  readonly customers = signal<Customer[]>([]);
  readonly vehicles = signal<Vehicle[]>([]);
  readonly selected = signal<ServiceOrder | null>(null);
  readonly timeline = signal<ServiceOrderEvent[]>([]);
  readonly forecasts = signal<ServiceOrderForecast[]>([]);
  readonly photos = signal<ServicePhoto[]>([]);
  readonly uploads = signal<{ name: string; progress: number; error: string }[]>([]);
  readonly inspections = signal<Inspection[]>([]);
  readonly correctingInspection = signal(false);
  readonly hasInspectionDraft = computed(() => this.inspections().some(item => item.estado === 'RASCUNHO'));
  readonly hasConfirmedInspection = computed(() => this.inspections().some(item => item.estado === 'CONFIRMADA'));
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly search = this.builder.nonNullable.control('', Validators.maxLength(100));
  readonly statusOptions = ACTIVE_STATUSES;
  readonly activeCount = computed(() => this.orders().filter(item => !['ENTREGUE', 'CANCELADO'].includes(item.status)).length);
  availableVehicles() {
    const customerId = this.form.controls.clienteId.value;
    return customerId ? this.vehicles().filter(item => item.clienteId === customerId) : this.vehicles();
  }
  readonly form = this.builder.nonNullable.group({
    clienteId: ['', Validators.required],
    veiculoId: ['', Validators.required],
    relatoInicial: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    entradaEm: [this.localDateTime(new Date()), Validators.required],
    kmEntrada: ['', [Validators.required, Validators.pattern(/^\d{1,7}$/)]],
    previsaoEm: ['']
  });
  readonly statusForm = this.builder.nonNullable.group({
    status: ['' as ServiceOrderStatus | '', Validators.required],
    motivo: ['', Validators.maxLength(1000)],
    textoPublico: ['', Validators.maxLength(2000)],
    textoInterno: ['', Validators.maxLength(2000)]
  });
  readonly updateForm = this.builder.nonNullable.group({
    textoPublico: ['', Validators.maxLength(2000)],
    textoInterno: ['', Validators.maxLength(2000)],
    publicada: [false]
  });
  readonly forecastForm = this.builder.nonNullable.group({
    previsaoEm: [''],
    semPrevisao: [false],
    motivoPublico: ['', [Validators.required, Validators.maxLength(1000)]],
    proximaAcao: ['', [Validators.required, Validators.maxLength(1000)]]
  });
  readonly inspectionForm = this.builder.nonNullable.group({
    quilometragem: ['', Validators.pattern(/^\d{0,7}$/)], combustivel: ['' as FuelLevel | ''],
    objetos: ['', Validators.maxLength(1000)], avarias: ['', Validators.maxLength(2000)],
    observacoes: ['', Validators.maxLength(2000)], frente: [''], traseira: [''],
    lateralEsquerda: [''], lateralDireita: [''], painel: [''], detalhes: ['']
  });
  readonly correctionReason = this.builder.nonNullable.control('', [Validators.required, Validators.maxLength(1000)]);

  ngOnInit() {
    this.inspectionForm.valueChanges.subscribe(() => this.persistInspection());
    void this.load();
  }
  async load() {
    this.loading.set(true); this.clearMessages();
    try {
      const [orders, customers, vehicles] = await Promise.all([
        this.service.orders(), this.registrations.customers(), this.registrations.vehicles()
      ]);
      this.orders.set(orders.items); this.customers.set(customers.items); this.vehicles.set(vehicles.items);
      const requestedId = this.route.snapshot.queryParamMap.get('id');
      const first = requestedId ? await this.service.order(requestedId) : orders.items[0] ?? null;
      this.selected.set(first);
      if (first) { await this.loadHistory(first.id); await this.loadPhotos(first.id); await this.loadInspection(first.id); }
    } catch (error) { this.showError(error, 'Não foi possível carregar as ordens de serviço.'); }
    finally { this.loading.set(false); }
  }
  newOrder() {
    this.selected.set(null); this.timeline.set([]); this.forecasts.set([]); this.clearMessages();
    this.form.reset({ clienteId: '', veiculoId: '', relatoInicial: '',
      entradaEm: this.localDateTime(new Date()), kmEntrada: '', previsaoEm: '' });
  }
  syncVehicle() {
    const vehicle = this.vehicles().find(item => item.id === this.form.controls.veiculoId.value);
    if (vehicle?.clienteId !== this.form.controls.clienteId.value) this.form.controls.veiculoId.reset('');
  }
  isActive(order: ServiceOrder) { return !['ENTREGUE', 'CANCELADO'].includes(order.status); }
  async orderClosed(order: ServiceOrder) {
    this.orders.update(items => items.map(item => item.id === order.id ? order : item));
    if (this.selected()?.id !== order.id) return;
    this.selected.set(order); this.resetWorkflowForms();
    sessionStorage.removeItem(this.inspectionKey(order.id));
    this.success.set('OS encerrada. Histórico preservado e acessos antigos revogados.');
    await this.loadHistory(order.id);
  }
  async returnVisit(order: ServiceOrder) {
    if (this.isActive(order) || this.busy()) return;
    this.busy.set(true); this.clearMessages();
    try {
      const vehicle = await this.registrations.vehicle(order.veiculoId);
      const customer = await this.registrations.customer(vehicle.clienteId);
      if (this.selected()?.id !== order.id) return;
      if (!customer.ativo) {
        this.error.set('O responsável atual está inativo. Regularize o cadastro antes de abrir uma nova OS.'); return;
      }
      this.customers.update(items => [customer, ...items.filter(item => item.id !== customer.id)]);
      this.vehicles.update(items => [vehicle, ...items.filter(item => item.id !== vehicle.id)]);
      this.newOrder();
      this.form.patchValue({ clienteId: customer.id, veiculoId: vehicle.id });
      this.success.set('Nova visita: confira o responsável atual e informe relato e quilometragem novos.');
    } catch (error) {
      if (this.selected()?.id !== order.id) return;
      if (error instanceof HttpErrorResponse && error.status === 404) {
        this.error.set('Veículo ou responsável atual indisponível. Confira o cadastro antes de abrir uma nova OS.');
      } else {
        this.showError(error, 'Não foi possível conferir o vínculo atual do veículo. Tente novamente.');
      }
    } finally { this.busy.set(false); }
  }
  async searchOrders() {
    if (this.search.invalid || this.busy()) return;
    await this.perform(async () => {
      const result = await this.service.orders(this.search.value);
      this.orders.set(result.items); this.selected.set(result.items[0] ?? null);
      this.resetWorkflowForms(); this.timeline.set([]); this.forecasts.set([]);
      if (this.selected()) { await this.loadHistory(this.selected()!.id); await this.loadPhotos(this.selected()!.id); }
    }, 'Busca atualizada.');
  }
  async selectOrder(order: ServiceOrder) {
    await this.perform(async () => {
      const detail = await this.service.order(order.id);
      this.selected.set(detail); this.resetWorkflowForms(); await this.loadHistory(detail.id); await this.loadPhotos(detail.id); await this.loadInspection(detail.id);
    }, 'Detalhes atualizados.');
  }
  async createOrder() {
    this.form.markAllAsTouched(); this.clearMessages();
    if (this.form.invalid) { this.error.set('Confira cliente, veículo, relato, entrada e quilometragem.'); return; }
    const value = this.form.getRawValue();
    const input: ServiceOrderInput = {
      clienteId: value.clienteId, veiculoId: value.veiculoId, relatoInicial: value.relatoInicial,
      entradaEm: new Date(value.entradaEm).toISOString(), kmEntrada: Number(value.kmEntrada),
      previsaoEm: value.previsaoEm ? new Date(value.previsaoEm).toISOString() : null
    };
    await this.perform(async () => {
      const opened = await this.service.create(input);
      this.orders.update(items => [opened, ...items]); this.selected.set(opened);
      this.resetWorkflowForms(); await this.loadHistory(opened.id); await this.loadPhotos(opened.id); await this.loadInspection(opened.id);
    }, 'Ordem de serviço aberta com sucesso.');
  }
  async changeStatus() {
    const order = this.selected();
    this.statusForm.markAllAsTouched(); this.clearMessages();
    if (!order || this.statusForm.invalid) { this.error.set('Selecione a nova etapa.'); return; }
    if (this.requiresReason() && !this.statusForm.controls.motivo.value.trim()) {
      this.error.set('Informe o motivo para retornar ou colocar uma etapa em espera.'); return;
    }
    const value = this.statusForm.getRawValue();
    await this.perform(async () => {
      const updated = await this.service.changeStatus(order.id, { ...value,
        status: value.status as ServiceOrderStatus, expectedVersion: order.versao });
      this.replaceOrder(updated); this.statusForm.reset({ status: '', motivo: '', textoPublico: '', textoInterno: '' });
      await this.loadHistory(order.id);
    }, 'Etapa atualizada e registrada na linha do tempo.');
  }
  async publishUpdate() {
    const order = this.selected();
    this.updateForm.markAllAsTouched(); this.clearMessages();
    const value = this.updateForm.getRawValue();
    if (!order || this.updateForm.invalid || (!value.textoPublico.trim() && !value.textoInterno.trim())) {
      this.error.set('Escreva um texto público ou uma observação interna.'); return;
    }
    if (value.publicada && !value.textoPublico.trim()) {
      this.error.set('Escreva o texto público antes de publicar para o cliente.'); return;
    }
    await this.perform(async () => {
      await this.service.publish(order.id, { ...value, expectedVersion: order.versao });
      const updated = await this.service.order(order.id);
      this.replaceOrder(updated); this.updateForm.reset({ textoPublico: '', textoInterno: '', publicada: false });
      await this.loadHistory(order.id);
    }, value.publicada ? 'Atualização publicada na linha do tempo.' : 'Observação interna registrada.');
  }
  async updateForecast() {
    const order = this.selected();
    this.forecastForm.markAllAsTouched(); this.clearMessages();
    const value = this.forecastForm.getRawValue();
    if (!order || this.forecastForm.invalid || (!value.semPrevisao && !value.previsaoEm)) {
      this.error.set('Informe a nova previsão ou marque a opção sem nova previsão.'); return;
    }
    await this.perform(async () => {
      const updated = await this.service.updateForecast(order.id, {
        previsao: value.semPrevisao ? null : new Date(value.previsaoEm).toISOString(),
        motivoPublico: value.motivoPublico, proximaAcao: value.proximaAcao,
        expectedVersion: order.versao
      });
      this.replaceOrder(updated); this.forecastForm.reset({
        previsaoEm: '', semPrevisao: false, motivoPublico: '', proximaAcao: ''
      });
      await this.loadHistory(order.id);
    }, 'Previsão atualizada e registrada no histórico.');
  }
  async saveInspection() {
    const order = this.selected();
    this.inspectionForm.markAllAsTouched();
    if (!order || this.inspectionForm.invalid) { this.error.set('Confira os campos da vistoria.'); return; }
    this.persistInspection();
    await this.perform(async () => {
      await this.service.saveInspection(order.id, this.inspectionData());
      await this.loadInspection(order.id);
    }, 'Rascunho da vistoria salvo.');
  }
  async confirmInspection() {
    const order = this.selected();
    this.inspectionForm.markAllAsTouched();
    if (!order || this.inspectionForm.invalid) { this.error.set('Confira os campos da vistoria.'); return; }
    this.persistInspection();
    await this.perform(async () => {
      await this.service.saveInspection(order.id, this.inspectionData());
      await this.service.confirmInspection(order.id, order.versao);
      sessionStorage.removeItem(this.inspectionKey(order.id));
      await this.loadInspection(order.id);
      this.replaceOrder(await this.service.order(order.id));
    }, 'Vistoria confirmada e preservada no histórico.');
  }
  beginCorrection() {
    const latest = this.inspections().find(item => item.estado === 'CONFIRMADA');
    if (!latest) return;
    this.applyInspection(latest.checklist);
    this.correctionReason.setValue('');
    this.correctingInspection.set(true);
  }
  async correctInspection() {
    const order = this.selected();
    this.inspectionForm.markAllAsTouched(); this.correctionReason.markAsTouched();
    if (!order || this.inspectionForm.invalid || this.correctionReason.invalid) {
      this.error.set('Informe os dados e o motivo da correção.'); return;
    }
    await this.perform(async () => {
      await this.service.correctInspection(order.id, order.versao,
        this.correctionReason.value, this.inspectionData());
      sessionStorage.removeItem(this.inspectionKey(order.id));
      this.correctingInspection.set(false);
      await this.loadInspection(order.id);
      this.replaceOrder(await this.service.order(order.id));
    }, 'Correção registrada como uma nova versão da vistoria.');
  }
  selectPhotos(event: Event) {
    const order = this.selected(); const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []).slice(0, 20); input.value = '';
    if (!order || !files.length) return;
    if (files.some(file => !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 10 * 1024 * 1024)) { this.error.set('Selecione JPEG, PNG ou WebP de até 10 MiB por foto.'); return; }
    this.uploads.set(files.map(file => ({ name: file.name, progress: 0, error: '' }))); void this.uploadQueue(order, files);
  }
  private async uploadQueue(order: ServiceOrder, files: File[]) {
    let next = 0; const worker = async () => { while (next < files.length) await this.uploadOne(order, files[next++]); };
    await Promise.all(Array.from({ length: Math.min(3, files.length) }, worker)); await this.loadPhotos(order.id);
  }
  private async uploadOne(order: ServiceOrder, file: File) {
    try { const request = await this.service.uploadPhoto(order.id, file, order.status, crypto.randomUUID()); await new Promise<void>((resolve, reject) => request.subscribe({
      next: event => { if (event.type === HttpEventType.UploadProgress) this.setUpload(file.name, Math.round(100 * event.loaded / (event.total || file.size))); if (event.type === HttpEventType.Response) { this.setUpload(file.name, 100); resolve(); } },
      error: error => { this.setUpload(file.name, 0, error?.error?.detail ?? 'Falha ao enviar.'); reject(error); }
    })); } catch { /* falhas ficam visíveis sem reenviar fotos concluídas */ }
  }
  async removePhoto(photo: ServicePhoto) { const order = this.selected(); if (!order || !confirm('Remover esta foto do acompanhamento?')) return; await this.perform(async () => { await this.service.deletePhoto(order.id, photo.id); await this.loadPhotos(order.id); }, 'Foto removida e registrada na auditoria.'); }
  photoUrl(photo: ServicePhoto) { const order = this.selected(); return order ? this.service.photoUrl(order.id, photo.id, photo.miniaturaDisponivel) : ''; }
  requiresReason() {
    const current = this.selected()?.status;
    const target = this.statusForm.controls.status.value as ServiceOrderStatus | '';
    if (!current || !target) return false;
    const returning = STATUS_SEQUENCE[target] < STATUS_SEQUENCE[current];
    const waiting = ['AGUARDANDO_APROVACAO', 'AGUARDANDO_PECAS'].includes(target);
    const executing = !['RECEBIDO', 'AGUARDANDO_APROVACAO', 'AGUARDANDO_PECAS',
      'PRONTO_PARA_RETIRADA', 'ENTREGUE', 'CANCELADO'].includes(current);
    return returning || waiting && executing;
  }
  eventTitle(event: ServiceOrderEvent) {
    return event.tipo === 'STATUS' ? (event.statusAnterior ?
      `${this.statusLabel(event.statusAnterior)} → ${this.statusLabel(event.statusNovo!)}` :
      `Ordem recebida · ${this.statusLabel(event.statusNovo!)}`) : 'Atualização registrada';
  }
  formatNumber(value: number) { return `OS-${value.toString().padStart(6, '0')}`; }
  formatPlate(value: string) { return value.length === 7 ? value.slice(0, 3) + '-' + value.slice(3) : value; }
  formatDate(value: string | null) {
    return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : 'Não informada';
  }
  statusLabel(value: ServiceOrderStatus) {
    return value.toLowerCase().replaceAll('_', ' ').replace(/^./, letter => letter.toUpperCase());
  }
  deadlineLabel(order: ServiceOrder) {
    if (!this.isActive(order)) return 'Encerrada · histórico interno';
    if (order.aguardandoRetirada) return 'Pronto · aguardando retirada';
    if (order.atrasada) return 'Previsão ultrapassada';
    if (order.previsaoEm) return 'Dentro da previsão';
    return 'Sem previsão';
  }
  private async loadHistory(id: string) {
    const [timeline, forecasts] = await Promise.all([this.service.timeline(id), this.service.forecasts(id)]);
    this.timeline.set(timeline); this.forecasts.set(forecasts);
  }
  private async loadPhotos(id: string) {
    try { this.photos.set(await this.service.photos(id)); }
    catch { this.photos.set([]); }
  }
  private async loadInspection(id: string) {
    this.inspectionForm.reset({ quilometragem: '', combustivel: '', objetos: '', avarias: '', observacoes: '',
      frente: '', traseira: '', lateralEsquerda: '', lateralDireita: '', painel: '', detalhes: '' }, { emitEvent: false });
    this.correctingInspection.set(false);
    try {
      const items = await this.service.inspection(id); this.inspections.set(items);
      const draft = items.find(item => item.estado === 'RASCUNHO');
      const confirmed = items.find(item => item.estado === 'CONFIRMADA');
      const stored = sessionStorage.getItem(this.inspectionKey(id));
      let source: InspectionChecklist | undefined;
      if (stored && !confirmed) { try { source = JSON.parse(stored); } catch { sessionStorage.removeItem(this.inspectionKey(id)); } }
      source ??= draft?.checklist ?? confirmed?.checklist;
      if (source) this.applyInspection(source);
    } catch { this.inspections.set([]); }
  }
  private inspectionData(): InspectionChecklist {
    const value = this.inspectionForm.getRawValue();
    const fotos = Object.fromEntries(['frente', 'traseira', 'lateralEsquerda', 'lateralDireita', 'painel', 'detalhes']
      .map(key => [key, value[key as keyof typeof value]]).filter(([, photo]) => !!photo)) as Record<string, string>;
    return { quilometragem: value.quilometragem ? Number(value.quilometragem) : null,
      combustivel: value.combustivel || null, objetos: value.objetos.trim(), avarias: value.avarias.trim(),
      observacoes: value.observacoes.trim(), fotos };
  }
  private applyInspection(checklist: InspectionChecklist) {
    this.inspectionForm.patchValue({ quilometragem: checklist.quilometragem?.toString() ?? '',
      combustivel: checklist.combustivel ?? '', objetos: checklist.objetos ?? '', avarias: checklist.avarias ?? '',
      observacoes: checklist.observacoes ?? '', frente: checklist.fotos?.['frente'] ?? '',
      traseira: checklist.fotos?.['traseira'] ?? '', lateralEsquerda: checklist.fotos?.['lateralEsquerda'] ?? '',
      lateralDireita: checklist.fotos?.['lateralDireita'] ?? '', painel: checklist.fotos?.['painel'] ?? '',
      detalhes: checklist.fotos?.['detalhes'] ?? '' }, { emitEvent: false });
  }
  private persistInspection() {
    const order = this.selected();
    if (order) sessionStorage.setItem(this.inspectionKey(order.id), JSON.stringify(this.inspectionData()));
  }
  private inspectionKey(id: string) { return `vistoria:${id}`; }
  private setUpload(name: string, progress: number, error = '') { this.uploads.update(items => items.map(item => item.name === name ? { ...item, progress, error } : item)); }
  private replaceOrder(order: ServiceOrder) {
    this.selected.set(order);
    this.orders.update(items => items.map(item => item.id === order.id ? order : item));
  }
  private resetWorkflowForms() {
    this.statusForm.reset({ status: '', motivo: '', textoPublico: '', textoInterno: '' });
    this.updateForm.reset({ textoPublico: '', textoInterno: '', publicada: false });
    this.forecastForm.reset({ previsaoEm: '', semPrevisao: false, motivoPublico: '', proximaAcao: '' });
  }
  private localDateTime(value: Date) {
    const local = new Date(value.getTime() - value.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 16);
  }
  private async perform(action: () => Promise<void>, message: string) {
    if (this.busy()) return;
    this.busy.set(true); this.clearMessages();
    try { await action(); this.success.set(message); }
    catch (error) { this.showError(error, 'Não foi possível concluir. Confira a conexão e tente novamente.'); }
    finally { this.busy.set(false); }
  }
  private clearMessages() { this.error.set(''); this.success.set(''); }
  private showError(error: unknown, fallback: string) {
    this.error.set(error instanceof HttpErrorResponse ? error.error?.detail ?? fallback : fallback);
  }
}
