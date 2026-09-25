import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Notice, NoticePage, NotificationService } from './notification.service';

@Component({
  selector: 'app-notification-page',
  imports: [DatePipe],
  templateUrl: './notification-page.component.html',
  styleUrl: './notification-page.component.css'
})
export class NotificationPageComponent implements OnInit {
  private readonly service = inject(NotificationService);
  private request = 0;
  readonly data = signal<NoticePage | null>(null);
  readonly loading = signal(false);
  readonly busy = signal<string | null>(null);
  readonly unread = signal(false);
  readonly error = signal('');
  readonly feedback = signal('');
  ngOnInit() { void this.load(); }

  async load(page = 0) {
    const request = ++this.request;
    this.loading.set(true); this.error.set('');
    try {
      const data = await this.service.list(page, this.unread());
      if (request === this.request) this.data.set(data);
    } catch {
      if (request === this.request) this.error.set('Não foi possível carregar os avisos. Tente novamente.');
    } finally { if (request === this.request) this.loading.set(false); }
  }
  async filter(unread: boolean) {
    this.unread.set(unread); this.data.set(null); this.feedback.set('');
    await this.load(0);
  }
  async act(notice: Notice, retry: boolean) {
    if (this.busy()) return;
    this.busy.set(notice.id); this.error.set(''); this.feedback.set('');
    try {
      if (retry) await this.service.retry(notice.id); else await this.service.read(notice.id);
      this.feedback.set(retry ? 'Reenvio solicitado. O e-mail ainda não foi enviado.' : 'Aviso marcado como lido.');
      const current = this.data();
      const lastOnPage = this.unread() && !retry && current?.items.length === 1;
      await this.load(Math.max(0, (current?.page ?? 0) - (lastOnPage ? 1 : 0)));
    } catch {
      this.error.set(retry ? 'Não foi possível solicitar o reenvio. Atualize os avisos e aguarde um minuto antes de tentar novamente.'
        : 'Não foi possível marcar o aviso como lido. Tente novamente.');
    } finally { this.busy.set(null); }
  }
  status(notice: Notice) {
    const labels = { PENDENTE: 'Na fila de envio', ENVIADO: 'Aceito pelo servidor de e-mail',
      FALHOU: 'Falha no envio', CANCELADO: 'Cancelado: contato alterado', SEM_EMAIL_VERIFICADO: 'Sem e-mail verificado no evento' };
    return labels[notice.emailEstado];
  }
}
