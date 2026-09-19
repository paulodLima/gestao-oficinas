# Validação — tarefa 2: cadastro e autenticação do proprietário

Data: 19/09/2026. Escopo: seção 2.0 de [tasks.md](tasks.md). Revisão independente: [2_task_review.md](2_task_review.md).

## Entrega

Cadastro atômico de oficina/proprietário; e-mail normalizado e único; BCrypt; login/logout; sessão JDBC com rotação, 30 minutos de inatividade e limite absoluto de 12 horas; CSRF inclusive login; token de recuperação aleatório, hash no banco, validade de 30 minutos e consumo único; revogação das sessões na troca de senha. Consulta de oficina usa identidade autenticada, nunca ID fornecido pelo navegador.

Telas Angular responsivas de cadastro, login, recuperação e redefinição; página protegida mínima; estados de erro/carregamento/sucesso; senha não persiste no navegador. Proxy /api de mesma origem no desenvolvimento e Docker. Flyway gerencia esquema, Mailpit captura e-mail local. Serviço SMTP reutilizável para a futura tarefa 10.

## Evidências executadas

| Verificação | Resultado |
| --- | --- |
| `mvn -B -ntp test` | 15 testes aprovados, zero falhas/erros/ignorados; 12 HTTP/PostgreSQL e 3 unitários |
| `npm test -- --watch=false --browsers=ChromeHeadless` | 5 testes aprovados; usado CHROME_BIN do Chromium headless instalado pelo Playwright |
| `docker compose -f docker/docker-compose.yml up --build -d` | Build API Java 21 e Angular aprovado; postgres/api/app/mailpit iniciados; nenhuma remoção de volume |
| `npm run test:e2e` | 2 jornadas completas aprovadas: Chromium desktop e emulação Pixel 7 |
| `git diff --check` | Sem erro de whitespace; avisos locais LF/CRLF apenas |

Maven no host usou Java 25 com release 21; imagem final foi compilada com Java 21. Testcontainers criou PostgreSQL 17 temporário. A primeira tentativa de testes Angular com Chrome completo não conectou ao Karma; a execução final com chrome-headless-shell concluiu os cinco casos.

### Cobertura da API

Isolamento entre duas oficinas, identidade incompatível, rotação de sessão, acesso anônimo, CSRF ausente, senha inválida/limite UTF-8, e-mail duplicado normalizado, login errado, logout, recuperação para conta inexistente, token expirado/substituído/usado, duas redefinições concorrentes (apenas uma vence), revogação em dois dispositivos, proprietário desativado, expiração por inatividade e limite absoluto, limitação de tentativas, falha SMTP com rollback e resposta pública genérica. IP encaminhado é aceito apenas do proxy confiável e diferencia dois clientes; cabeçalho de origem não confiável é ignorado.

### Jornada real pelo navegador

Rota protegida redireciona → cadastro → login incorreto → login válido → atualização da página mantém acesso → recuperação → mensagem capturada no Mailpit → link abre e remove token da URL → nova senha → aba antiga consegue sair após revogação → login com nova senha → logout → rota protegida negada. Verificado cookie HttpOnly/SameSite=Lax e ausência de overflow horizontal na viewport testada.

As capturas `oficinas-app/test-results/login-desktop.png` e `login-mobile.png` são geradas pelos testes e ignoradas no Git. Houve inspeção visual e correção de espaçamento no título móvel. Emulação de tela não equivale a testes em aparelhos físicos ou Safari/iOS.

## Ajustes de revisão

Corrigidos agrupamento indevido de IP no proxy, diferença de status HTTP na falha SMTP e saída da interface após sessão revogada. Acrescentados testes de expiração. Respostas dos filtros padronizadas com type/title/status/detail/code/traceId/errors. Removida geração de usuário padrão pelo Spring Security.

## Limites e pendências explícitos

- Mailpit valida SMTP local, não entrega externa. Produção requer remetente/domínio verificado, credenciais SMTP, HTTPS, perfil prod, rede privada e topologia de proxy confiável.
- Envio síncrono é a base desta tarefa. Falha gera rollback e aviso `PASSWORD_RECOVERY_EMAIL_UNAVAILABLE`, sem e-mail/token nos logs. Resposta pública permanece 202; não há retries duráveis. Outbox/retries e redução de diferença temporal entre contas conhecidas/desconhecidas serão tratados na tarefa 15.
- `npm audit --omit=dev` identificou 10 pacotes afetados (5 severidade alta, 5 moderada) na base Angular 19; a auditoria completa apontou 34 (incluindo dependências de desenvolvimento). Isso não prova exploração nestas telas. Migração para versão principal corrigida exige tarefa própria e é pendência antes de produção; não foi aplicado `npm audit fix --force`.
- Os E2E deixam contas sintéticas `@example.test` e mensagens de teste no ambiente local; nenhum dado existente foi apagado.
- Identidade usa JDBC transacional, conforme decisão registrada na techspec. Configuração completa da oficina, clientes, OS e portal permanecem nas próximas tarefas.

## Reproduzir

1. Suba o Compose com build e aguarde a inicialização da API.
2. Em oficinas-api, execute `mvn test` com Docker disponível.
3. Em oficinas-app, execute `npm ci`, os testes Angular e `npm run build`.
4. Instale Chromium com `npx playwright install chromium` e execute `npm run test:e2e`, usando as portas padrão 4200 e 8025.
5. Para experimentar manualmente, abra http://localhost:4200 e crie sua conta; consulte recuperação em http://localhost:8025. Não há conta padrão.
