import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from './notification.service';

@Component({
  selector: 'app-notification-bell',
  imports: [RouterLink],
  template: `
    <a routerLink="/notificacoes" class="bell" [attr.aria-label]="count() === null ? 'Avisos' : 'Avisos: ' + count() + ' não lidos'" title="Central de avisos">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4M12 2V1"/></svg>
      @if (count(); as total) { <span class="count" aria-hidden="true">{{ total > 99 ? '99+' : total }}</span> }
    </a>
  `,
  styles: `
    :host{display:inline-flex;flex:none}.bell{position:relative;display:grid;place-items:center;width:46px;height:46px;border:1px solid var(--line);border-radius:50%;background:var(--surface);color:var(--ink);text-decoration:none}
    .bell:hover{background:var(--brand-soft)}svg{width:25px;height:25px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}
    .count{position:absolute;top:-5px;right:-5px;display:grid;place-items:center;min-width:22px;height:22px;padding:0 5px;border:2px solid var(--surface);border-radius:99px;background:#c72e61;color:white;font-size:11px;font-weight:750;line-height:1}
  `
})
export class NotificationBellComponent implements OnInit {
  private readonly service = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  readonly count = signal<number | null>(null);
  private loading = false;
  ngOnInit() {
    void this.refresh();
    const timer = window.setInterval(() => { if (!document.hidden) void this.refresh(); }, 30000);
    const focus = () => { void this.refresh(); };
    window.addEventListener('focus', focus);
    this.destroyRef.onDestroy(() => { window.clearInterval(timer); window.removeEventListener('focus', focus); });
  }
  private async refresh() {
    if (this.loading) return;
    this.loading = true;
    try {
      const page = await this.service.list(0, true);
      if (!this.destroyRef.destroyed) this.count.set(page.totalElements);
    } catch { /* Keep the last confirmed count; the link remains available. */ }
    finally { this.loading = false; }
  }
}
