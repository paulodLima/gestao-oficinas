import { Component, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-auth-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './auth-page.component.html',
  styleUrl: './auth-page.component.css'
})
export class AuthPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly mode = inject(ActivatedRoute).snapshot.data['mode'] as 'login' | 'register' | 'recover' | 'reset';
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly showPassword = signal(false);
  private token = '';
  readonly form = this.fb.nonNullable.group({
    nome: [''], nomeOficina: [''], email: [''], senha: [''], confirmacao: ['']
  });
  get title() {
    return { login: 'Bom ter você de volta.', register: 'Sua oficina, mais próxima.', recover: 'Vamos recuperar seu acesso.', reset: 'Um novo começo.' }[this.mode];
  }
  get subtitle() {
    return {
      login: 'Entre para cuidar do próximo atendimento.',
      register: 'Crie seu acesso e dê o primeiro passo.',
      recover: 'Informe o e-mail usado no cadastro da sua oficina.',
      reset: 'Escolha uma nova senha para sua conta.'
    }[this.mode];
  }
  ngOnInit() {
    if (this.mode !== 'reset') this.form.controls.email.setValidators([Validators.required, Validators.email, Validators.maxLength(254)]);
    if (this.mode === 'register') {
      this.form.controls.nome.setValidators([Validators.required, Validators.maxLength(120)]);
      this.form.controls.nomeOficina.setValidators([Validators.required, Validators.maxLength(120)]);
    }
    if (this.mode !== 'recover') this.form.controls.senha.setValidators([Validators.required]);
    if (this.mode === 'register' || this.mode === 'reset') {
      this.form.controls.senha.addValidators(Validators.minLength(12));
      this.form.controls.confirmacao.setValidators(Validators.required);
    }
    Object.values(this.form.controls).forEach(control => control.updateValueAndValidity());
    if (this.mode === 'reset') {
      this.token = new URLSearchParams(window.location.hash.slice(1)).get('token') ?? '';
      window.history.replaceState(null, '', window.location.pathname);
      if (!this.token) this.error.set('Link ausente ou inválido. Solicite um novo e-mail de recuperação.');
    }
  }
  async submit() {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    this.error.set('');
    if (this.form.invalid) { this.error.set('Confira os campos obrigatórios e o e-mail informado.'); return; }
    const data = this.form.getRawValue();
    if (this.mode === 'register' || this.mode === 'reset') {
      if (data.senha !== data.confirmacao) { this.error.set('As senhas precisam ser iguais.'); return; }
      if (new TextEncoder().encode(data.senha).length > 72) { this.error.set('Sua senha deve ter no máximo 72 bytes. Use menos caracteres.'); return; }
    }
    if (this.mode === 'reset' && !this.token) { this.error.set('Solicite um novo link para redefinir sua senha.'); return; }
    this.busy.set(true);
    try {
      if (this.mode === 'login') {
        await this.auth.login(data.email, data.senha);
        await this.router.navigateByUrl('/painel');
      } else if (this.mode === 'register') {
        await this.auth.post('/api/auth/cadastro', { nome: data.nome, nomeOficina: data.nomeOficina, email: data.email, senha: data.senha });
        this.success.set('Conta criada! Agora você já pode entrar com seu e-mail e senha.');
      } else if (this.mode === 'recover') {
        await this.auth.post('/api/auth/recuperacao', { email: data.email });
        this.success.set('Se houver uma conta com esse e-mail, você receberá um link válido por 30 minutos. Confira também o spam.');
      } else {
        await this.auth.post('/api/auth/redefinicao', { token: this.token, novaSenha: data.senha });
        this.token = '';
        this.success.set('Senha atualizada. Entre novamente em seus dispositivos.');
      }
      this.form.controls.senha.reset();
      this.form.controls.confirmacao.reset();
    } catch (e) {
      const response = e as HttpErrorResponse;
      this.error.set(response.error?.detail ?? (response.status === 0 ? 'Não foi possível conectar. Confira sua conexão e tente novamente.' : 'Não foi possível concluir. Tente novamente.'));
    } finally { this.busy.set(false); }
  }
}
