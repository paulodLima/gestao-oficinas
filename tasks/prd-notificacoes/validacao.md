# Validação da tarefa 15 — 25/09/2026

## Evidências executadas

- `mvn -Dtest.database=embedded test`: 91 testes Java, zero falhas, erros ou skips.
  PostgreSQL 17.6 real, com migrações V1–V14 em banco efêmero. Logs locais:
  `oficinas-api/target/task15-tests.log` e `target/surefire-reports`.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 49 testes aprovados.
- `npm run build`: aprovado. Dois avisos CSS preexistentes no portal e na OS;
  nenhum aumento de limites para esconder os avisos.
- Playwright `notifications.spec.ts` + `portal.spec.ts`: 8 cenários aprovados,
  desktop e mobile. Repetição dos 4 cenários da central também aprovada.
- Capturas desktop/mobile inspecionadas em
  `oficinas-app/test-results/notificacoes-{desktop,mobile}.png`; sem rolagem
  horizontal do documento a 320 px. Evidências locais ignoradas pelo Git.
- `git diff --check`: sem erros de whitespace.

## Cobertura de comportamento

Transação OS+aviso+outbox, rollback de negócio sem e-mail, worker não lê operação
não commitada, repetição idempotente de abertura e decisão, destinatário ativo e
verificado, contato alterado/desativado, dois workers concorrentes, falha do provedor
sem perda da OS, cinco tentativas, histórico preservado no reenvio, isolamento de
oficinas, leitura idempotente, paginação, autenticação e CSRF via HTTP real.
Gatilhos de abertura, adicional, decisão e previsão exercitados nos serviços reais.
Textos menores não disparam envio e templates não incluem notas internas/CPF/placa.
Frontend cobre erro recuperável, filtro, leitura, reenvio pendente e respostas fora
de ordem. Transporte testado sem remetente/provedor: falha, nunca sucesso falso.

## Correções durante a validação

- A nova rota foi adicionada à autorização de proprietário após teste HTTP revelar 403.
- Template Angular corrigido após compilação rejeitar alias em `@else if`.
- Testes antigos de decisão passaram a converter `Instant` para `Timestamp` JDBC;
  fixture de portal consulta slug em `/api/oficina`, não no DTO de `/api/auth/me`.
- Adicionado runner PostgreSQL nativo opt-in para permitir regressão sem Docker;
  o padrão dos testes integrados continua sendo Testcontainers.
- Revisão independente identificou falta do identificador da oficina no e-mail e
  dados antigos após falha na troca de filtro. Ambos corrigidos, com regressões:
  e-mails de duas oficinas mantêm nome/slug corretos sem token de acesso; erro ao
  mudar filtro limpa a página anterior e recupera a consulta desde a página zero.
  Após os ajustes, repetidos 91 Java, 49 Angular, build e 8 Playwright com sucesso.

## Limites explícitos

Nenhum e-mail real foi enviado. SMTP externo/domínio de produção não foram
validados. E2E legados de cadastro/cadastro de veículos exigem API+Mailpit ativos:
a tentativa de executar toda a suíte Playwright foi interrompida por ausência desses
serviços; o resultado de 8 cenários não representa essa suíte completa. Os fluxos
backend correspondentes passaram nos testes integrados Java.

SMTP é at-least-once: queda entre aceitação remota e commit pode repetir uma entrega;
deduplicação persistente cobre replay de eventos, concorrência e reenvios normais.
Pronto, encerramento e avaliação só possuem contratos nesta tarefa, conforme escopo
das tarefas 17/18. Códigos e recuperação mantêm os fluxos síncronos existentes;
esta fila não armazena OTP nem tokens em claro.
