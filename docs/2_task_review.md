# Revisão: tarefa 2 — Cadastro e autenticação do proprietário

Revisor: agente task_reviewer. Data: 19/09/2026.
Fonte: [tasks.md](tasks.md), seção 2.0; contratos: [techspec.md](techspec.md).
Status: APROVADO COM OBSERVAÇÕES na revisão estática; execução final dos testes acompanhada pelo agente principal.

## Resumo

Revisados cadastro transacional de oficina/proprietário, login, sessão JDBC, CSRF, recuperação e redefinição de senha, isolamento de identidade, proxy de mesma origem e telas Angular. As três correções solicitadas foram verificadas no código: identificação de IP pelo proxy confiável; resposta genérica na falha do e-mail; saída local quando a sessão já foi revogada.

Esta avaliação não equivale a prontidão de produção. SMTP externo, HTTPS e topologia real de proxies ainda exigem configuração e validação operacional.

## Arquivos revisados

| Grupo | Resultado |
| --- | --- |
| API: identidade/*.java | Autorização, sessões, tokens, erros e rate limit revisados |
| API: notificacoes/TransactionalEmail.java | Falha explícita interna, sem exposição de exceção do provedor |
| API: V1__identidade_e_sessoes.sql e application*.yaml | Unicidade, sessão persistida, timeout e perfil de produção revisados |
| API: AuthIntegrationTest.java e política de senha | Casos de isolamento, CSRF, consumo único, concorrência e expiração presentes |
| Angular: auth/*, rotas e configuração HTTP | Login, cadastro, recuperação, reset, guard e saída revisados |
| Angular: server.ts e proxy.conf.json | Encaminhamento de cookies/CSRF e destino fixo revisados |
| Docker Compose e dependências Maven/Angular | Mailpit local e configuração do proxy conferidos |

## Achados resolvidos

1. Limites antes agrupavam todos os clientes no IP do app. ClientAddress agora aceita o cabeçalho específico apenas de endereços do host confiável configurado; Express sobrescreve o cabeçalho com o endereço da conexão. Cabeçalho enviado diretamente à API por origem não confiável é ignorado.
2. Recuperação distinguia conta existente por HTTP 503 em falha SMTP. Controller agora retorna 202 genérico após rollback do serviço e registra aviso operacional sem destinatário/token. O teste cobre conta conhecida e desconhecida.
3. Logout após revogação mostrava erro e mantinha a tela. HomeComponent agora limpa o perfil e navega para login ao receber 401.
4. Foram acrescentados testes distintos para inatividade de 30 minutos e limite absoluto de 12 horas, antes ausentes.

## Problemas críticos e major

Nenhum bloqueador adicional identificado nesta revisão estática delimitada. Os resultados de testes em execução devem ser anexados à validação da tarefa antes de concluí-la.

## Observações minor

- AuthService.java, método recover: SMTP síncrono mantém diferença de tempo entre conta conhecida e inexistente e ocupa transação durante envio. Ao implementar outbox na tarefa 15, processar envio fora da requisição e manter retorno público genérico.
- AuthController.java: construtor e métodos concentram dependências, abreviações e várias instruções na mesma linha. Separar instruções, nomear limites e tornar nomes mais descritivos na manutenção. Nomenclatura portuguesa dos domínios segue a especificação existente; convenção Java de arquivos PascalCase prevalece sobre sugestão genérica de kebab-case da skill.
- server.ts: em implantação atrás de balanceador adicional, socket.remoteAddress representa esse balanceador. Definir explicitamente a cadeia confiável nesse ambiente; nunca aceitar cabeçalhos arbitrários do navegador.
- ApiErrors.java: observação sobre title/traceId/errors corrigida pelo agente principal; respostas dos filtros agora incluem esses campos.

## Destaques positivos

- BCrypt com limite de bytes e token de recuperação aleatório armazenado somente por hash.
- Consumo único do token e bloqueio transacional; authVersion impede reutilização de sessões anteriores após redefinição.
- Cookie HttpOnly, proteção CSRF inclusive no login, dados privados sem cache e renderização cliente.
- Oficina derivada da identidade autenticada, sem confiar em identificador fornecido pela URL.
- Evidência de testes distingue implementação presente de integrações externas ainda pendentes.

## Testes e tipos

O projeto usa Maven e Angular; os comandos genéricos Bun da skill não se aplicam. A execução foi centralizada no agente principal para evitar testes concorrentes alterando as mesmas fixtures. Esta revisão inspecionou os casos, mas não afirma execução independente.

Resultados executados pelo agente principal: 15 testes Maven/PostgreSQL aprovados; 5 testes Angular aprovados; build Angular/API no Docker aprovado; 2 jornadas E2E Chromium (desktop e viewport móvel) aprovadas com envio SMTP real ao Mailpit local. Evidências e limitações em [task-2-validacao.md](task-2-validacao.md). A auditoria de dependências identificou alertas na base Angular 19; atualização é requisito antes de produção.

## Veredito

Correções de revisão aceitas estaticamente e suítes executadas pelo agente principal aprovadas. Aprovação restrita ao escopo da tarefa 2 em desenvolvimento, com observações documentadas, sem declaração de ambiente de produção pronto.
