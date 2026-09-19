import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  template: `<main><header><a class="brand" href="/inicio">/ gestão oficinas</a><button (click)="logout()" [disabled]="busy()">Sair da conta</button></header>
    <section><p class="eyebrow">SUA OFICINA</p><h1>{{ auth.owner()?.oficina?.nome }}</h1>
    <p>Bem-vindo, {{ auth.owner()?.nome }}.</p>
    <a routerLink="/configuracoes/oficina">Configurar dados e logo da oficina</a>
    <div class="ready"><span aria-hidden="true">✓</span><div><h2>Seu acesso está pronto.</h2><p>Você está na área protegida da sua oficina. Os recursos de clientes e serviços serão disponibilizados nas próximas etapas.</p></div></div>
    @if(error()){<p role="alert">{{ error() }}</p>}
    <p class="account">Conectado como {{ auth.owner()?.email }}</p></section></main>`,
  styles: [`main{min-height:100dvh;background:#f6f3ec;color:#202522}header{background:#202522;padding:24px 6%;display:flex;align-items:center;justify-content:space-between;gap:20px;flex-wrap:wrap}.brand{font-size:22px;color:#f1a657;text-decoration:none}button{min-height:44px;background:transparent;color:white;border:1px solid #8e9987;padding:10px 20px;border-radius:4px;cursor:pointer}section{max-width:850px;margin:auto;padding:80px 24px}.eyebrow{letter-spacing:2px;font-size:12px;color:#53604c}h1{font-family:Georgia,serif;font-weight:400;font-size:46px;overflow-wrap:anywhere}.ready{display:flex;gap:24px;border:1px solid #c8ccc0;padding:28px;margin:40px 0;background:#fffdf8}.ready>span{color:#254d38;font-size:30px}h2{margin-top:0;font-size:22px}p{line-height:1.7}.account{font-size:13px;color:#64675f;overflow-wrap:anywhere}button:focus-visible,a:focus-visible{outline:3px solid #f1a657;outline-offset:4px}@media(max-width:480px){section{padding-top:40px}h1{font-size:34px}.ready{padding:20px;gap:12px}}`]
})
export class HomeComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly busy = signal(false);
  readonly error = signal('');
  async logout() {
    this.busy.set(true);
    try { await this.auth.logout(); await this.router.navigateByUrl('/entrar'); }
    catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.auth.owner.set(null);
        await this.router.navigateByUrl('/entrar');
      } else this.error.set('Não foi possível sair. Confira a conexão e tente novamente.');
    }
    finally { this.busy.set(false); }
  }
}
