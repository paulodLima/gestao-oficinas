import { DOCUMENT } from '@angular/common';
import { Component, Input, OnDestroy, computed, inject, signal } from '@angular/core';
import { ServiceOrder, ServiceOrderService } from './service-order.service';
import { buildTrackingMessage, buildTrackingUrl, buildWhatsAppUrl } from './manual-share';

@Component({
  selector: 'app-order-share',
  templateUrl: './order-share.component.html',
  styleUrl: './order-share.component.css'
})
export class OrderShareComponent implements OnDestroy {
  private readonly service = inject(ServiceOrderService);
  private readonly document = inject(DOCUMENT);
  private currentOrder!: ServiceOrder;
  private revision = 0;
  private expiryTimer?: ReturnType<typeof setTimeout>;
  readonly busy = signal(false);
  readonly url = signal('');
  readonly expires = signal('');
  readonly notice = signal('');
  readonly error = signal('');
  readonly message = computed(() => this.url() ? buildTrackingMessage(this.url()) : '');
  readonly destination = computed(() => buildWhatsAppUrl(this.message()));

  @Input({ required: true }) set order(value: ServiceOrder) {
    if (this.currentOrder?.id !== value.id || this.currentOrder.status !== value.status) {
      this.revision++;
      this.clearLink();
      this.busy.set(false);
      this.clearMessages();
    }
    this.currentOrder = value;
  }

  get active() { return !['ENTREGUE', 'CANCELADO'].includes(this.currentOrder.status); }

  async prepare() {
    if (this.busy() || !this.active) return;
    const revision = this.revision;
    const id = this.currentOrder.id;
    this.busy.set(true);
    this.clearMessages();
    this.clearLink();
    try {
      const link = await this.service.createCustomerAccess(id);
      if (revision !== this.revision) return;
      const remaining = Date.parse(link.expiraEm) - Date.now();
      if (!Number.isFinite(remaining) || remaining <= 0) throw new Error('Expired link');
      this.url.set(buildTrackingUrl(this.document.location.origin, link.token));
      this.expires.set(link.expiraEm);
      this.expiryTimer = setTimeout(() => this.expireLink(), remaining);
      this.notice.set('Link preparado. Links anteriores desta OS foram revogados. Nenhuma mensagem foi enviada.');
    } catch {
      if (revision === this.revision) this.error.set('Não foi possível preparar o link. Tente novamente; se necessário, revogue os links desta OS.');
    } finally {
      if (revision === this.revision) this.busy.set(false);
    }
  }

  async revoke() {
    if (this.busy()) return;
    const revision = this.revision;
    const id = this.currentOrder.id;
    this.busy.set(true);
    this.clearMessages();
    this.clearLink();
    try {
      await this.service.revokeCustomerAccess(id);
      if (revision === this.revision) this.notice.set('Links desta OS revogados. O acesso por esses links foi encerrado.');
    } catch {
      if (revision === this.revision) this.error.set('Não foi possível confirmar a revogação. O link pode continuar válido; tente revogar novamente.');
    } finally {
      if (revision === this.revision) this.busy.set(false);
    }
  }

  async copy() {
    if (this.url() && Date.parse(this.expires()) <= Date.now()) this.expireLink();
    if (this.busy() || !this.isLinkValid()) return;
    const revision = this.revision;
    this.busy.set(true);
    this.clearMessages();
    try {
      await this.document.defaultView!.navigator.clipboard.writeText(this.url());
      if (revision === this.revision && this.isLinkValid()) this.notice.set('Link copiado. Envie manualmente apenas ao cliente responsável.');
    } catch {
      if (revision === this.revision && this.isLinkValid()) this.error.set('Cópia indisponível. Selecione o campo do link e copie manualmente.');
    } finally {
      if (revision === this.revision) this.busy.set(false);
    }
  }

  openWhatsApp(event: Event) {
    if (this.url() && Date.parse(this.expires()) <= Date.now()) this.expireLink();
    if (this.busy() || !this.isLinkValid()) { event.preventDefault(); return; }
    this.clearMessages();
    this.notice.set('Continue no WhatsApp: escolha o destinatário e confirme o envio. O sistema não confirma envio nem entrega.');
  }

  formatExpiry() {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo'
    }).format(new Date(this.expires()));
  }

  ngOnDestroy() { this.revision++; this.clearLink(); }

  private isLinkValid() {
    return this.active && !!this.url() && Date.parse(this.expires()) > Date.now();
  }

  private expireLink() {
    this.clearLink();
    this.clearMessages();
    this.notice.set('Este link expirou. Gere um novo link para compartilhar.');
  }

  private clearLink() {
    clearTimeout(this.expiryTimer);
    this.url.set('');
    this.expires.set('');
  }

  private clearMessages() { this.notice.set(''); this.error.set(''); }
}
