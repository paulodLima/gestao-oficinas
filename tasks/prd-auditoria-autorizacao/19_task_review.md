# Review: Task 19 - Auditoria e testes de autorização

**Revisor**: task-reviewer independente (Codex)
**Data**: 2026-09-25
**Arquivo da task**: `19_task.md`
**Branch/base**: `codex/tarefa-19-auditoria`, `origin/master` = `1085f462d8341059efa1c0c6d6449794b716f81d`
**Status**: APROVADO — rechecagem final concluída; nenhum bloqueador pendente.

## Resumo

A implementação protege a pesquisa de CPF por POST com CSRF, rotaciona a sessão após autenticação, invalida desafios/sessões de login por versão do contato, limita a emissão de códigos de login e acrescenta auditoria dos links e restrições de configuração em produção. A matriz de downloads verifica bytes reais, isolamento entre oficinas/clientes e retirada de publicação.

Os dois P1 inicialmente encontrados em adicionais foram corrigidos pelo principal e encerrados nesta rechecagem da fonte final congelada. A rechecagem limitou-se às correções, testes associados e evidências finais; não reiniciou a auditoria global. A aprovação incorpora a revisão inicial do restante do diff, incluindo arquivos novos não rastreados.

## Arquivos Revisados

Prefixos das linhas abaixo: API = `oficinas-api/src/main/java/br/com/gestao/oficinas_api`; testes API = `oficinas-api/src/test/java/br/com/gestao/oficinas_api`; app = `oficinas-app`.

| Arquivo | Status | Problemas |
| --- | --- | --- |
| API `cadastro/CustomerController.java` | OK | Pesquisa no corpo; escopo derivado da identidade |
| API `cadastro/CustomerVehicleRepository.java` | OK | Versão de acesso integrada aos desafios de adicionais |
| API `cadastro/CustomerVerificationService.java` | OK | Lock do cliente antes da leitura e confirmação |
| API `identidade/AuthProperties.java`, novo `ProductionSecurity.java` | OK | Origem, segredo e cookie seguro |
| API `ordem/ServiceOrderAccessController.java` | OK | Auditoria transacional e locks por OS |
| API `portal/PortalAccessController.java` | OK | Rotação, revogação, isolamento e composição com adicionais |
| Novos API `portal/PortalChallengeIssuer.java`, `PortalCodeHash.java` | OK | Limites persistentes, cooldown, HMAC e rollback |
| Nova migração `oficinas-api/src/main/resources/db/migration/V17__revogacao_acesso_cliente.sql` | OK | Snapshot também em `adicional_desafio`, com padrão -1 para credenciais anteriores |
| Testes API `cadastro/CustomerVehicleIntegrationTest.java` | OK | Pesquisa e regressões de cadastro |
| Testes API `portal/PortalAccessIntegrationTest.java`, `PortalAccessTransactionTest.java` | OK | Incluem regressão HTTP ampliada dos dois P1 |
| Novo teste API `identidade/ProductionSecurityTest.java` | OK | Validação de configuração; não comprova TLS implantado |
| App `src/app/cadastro/customer-vehicle.service.ts` e `.spec.ts` | OK | CPF fora da URL e header CSRF |
| App `src/app/portal/portal-access.component.ts` e `.spec.ts` | OK | Token vazio não restaura autorização anterior |
| App `e2e/manual-share.spec.ts`, `playwright.config.ts` | OK | Origem configurável e expectativas sem porta fixa |
| `.gitignore`, `docs/tasks.md`, novos documentos `tasks/prd-auditoria-autorizacao/{prd.md,techspec.md,19_task.md,tasks.md,validacao.md}` | OK | Escopo e evidências conciliados; documentação final a cargo do principal |
| API `adicional/AdditionalDecisionService.java`, `portal/AdditionalDecisionController.java` | OK após correção | Achados 1 e 2 resolvidos |
| Testes API `adicional/AdditionalDecisionIntegrationTest.java`, `AdditionalDecisionServiceTest.java` | OK | Fixtures adaptadas à versão de acesso e ao rate limit |

Também foram lidos os contextos de autorização, rate limit, resolução de origem, serviço de cadastro, políticas do portal, download/armazenamento, avaliação, envio de e-mail, migrações de desafios, testes de adicionais e configuração Maven/npm/prod. A revisão considera arquivos não rastreados, além do diff contra a base. Os padrões e o template da skill `task-review` foram lidos integralmente.

## Problemas Encontrados

### Problemas Críticos

Nenhum problema crítico pendente.

### Problemas Major

Nenhum problema major pendente.

### Problemas Minor

Nenhuma correção minor exigida. Os avisos CSS preexistentes não bloqueiam a tarefa 19.

## Achados Corrigidos e Rechecados

### 1. [P1 — RESOLVIDO] Código de decisão do contato antigo permanecia válido

**Locais**: [AdditionalDecisionService.java:132](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-api/src/main/java/br/com/gestao/oficinas_api/adicional/AdditionalDecisionService.java:132>), emissão na linha 99, `lockContact()` na linha 291 e [V17__revogacao_acesso_cliente.sql:3](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-api/src/main/resources/db/migration/V17__revogacao_acesso_cliente.sql:3>).

Na versão inicial, uma sessão por link conseguia decidir adicionais com código enviado ao e-mail A após alteração do cadastro para B. A versão final persiste `cliente.acesso_versao` no desafio e compara o snapshot com a versão atual, exigindo cliente ativo e contato verificado. O padrão -1 invalida desafios anteriores à migração.

Emissão e confirmação bloqueiam o cliente com `FOR UPDATE` até o fim da transação, impedindo que a alteração de contato se intercale entre a leitura e a conclusão da operação. O lock da OS continua serializando operações legítimas sobre a mesma solicitação. A edição de cliente não passou a adquirir lock de OS em ordem inversa.

**Regressão verificada**: [PortalAccessIntegrationTest.java:96](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-api/src/test/java/br/com/gestao/oficinas_api/portal/PortalAccessIntegrationTest.java:96>) rejeita o código antigo antes e depois da verificação de B, após retorno a A e após desativação/reativação; confirma ausência de decisões indevidas e aceita um código novo. O cenário usa sessão por link e alterações de cadastro por HTTP; a verificação de e-mail é preparada no banco da fixture e o transporte é mockado.

### 2. [P1 — RESOLVIDO] Emissão de códigos de adicionais sem limitação

**Locais**: [AdditionalDecisionService.java:84](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-api/src/main/java/br/com/gestao/oficinas_api/adicional/AdditionalDecisionService.java:84>) e [AdditionalDecisionController.java:45](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-api/src/main/java/br/com/gestao/oficinas_api/portal/AdditionalDecisionController.java:45>).

A correção acrescenta limite persistente de cinco solicitações por contato na janela existente de 15 minutos, cooldown de 60 segundos por cliente e limites por origem de 30 emissões e 60 confirmações. A origem é resolvida por `ClientAddress`; `RateLimit` mantém os contadores em transação independente.

Os controles ocorrem antes de inutilizar o desafio anterior ou enviar e-mail. O lock do cliente serializa emissões para esse cliente, inclusive entre solicitações distintas.

**Regressão verificada**: o mesmo teste HTTP confirma 429 no reenvio imediato, primeiro desafio ainda com `usado_em=null` e exatamente um envio. A ligação dos limites por origem/contato e sua persistência independente também foram conferidas no código.

Os testes citados não exercitam isoladamente todos os limiares das janelas nem simulam concorrência de alteração do contato; não se atribui a eles essa cobertura. A revisão da implementação não identificou bloqueador remanescente nas duas correções.

## Destaques Positivos

- Rotação de sessão via Servlet somente após credencial válida; link inválido e expiração operacional removem apenas `PORTAL_CLIENTE`, preservando grants independentes.
- Versão monotônica de acesso evita reativar sessões/desafios de login ao retornar ao mesmo e-mail ou reativar o cliente; migração invalida os desafios anteriores por padrão.
- Emissão de login usa HMAC, resposta genérica, limites persistidos em transação independente, cooldown e rollback do desafio se a entrega falhar.
- Downloads conferem oficina, OS, vínculo e estado de publicação antes de ler o arquivo; respostas usam `no-store`.
- Pesquisa privada não inclui CPF na URL gerada pelo app; autorização e CSRF continuam aplicados pelo servidor.
- Auditoria de emissão/revogação de links registra ator, recurso e ação na mesma transação, sem gravar token/código/CPF.
- Produção rejeita configuração de origem/cookie insegura e segredo padrão; o guia distingue configuração testada de implantação HTTPS real.

## Conformidade com Padrões

| Padrão | Status |
| --- | --- |
| Spring Boot/Java/JDBC existentes | Adequado; injeção por construtor, SQL parametrizado e transações mantidos |
| Angular/TypeScript/npm | Adequado; testes e build conferidos nas evidências |
| REST/HTTP/CSRF/isolamento | Sem bloqueador identificado; correções de adicionais rechecadas |
| Logging/auditoria | Sem exposição nova identificada; teste de falha SMTP e trilha de links presentes |
| Testes | Suíte completa com implementação corrigida e teste focado ampliado aprovados |
| Regras genéricas de estilo da skill | Adaptadas ao projeto por instrução expressa; sem impor JPA, Bun, renomeação de contratos em português ou refatoração global |

## Validação Conferida

| Evidência | Resultado |
| --- | --- |
| `task19-api-full.log`, 15:27:04 | 130 testes, zero falhas/erros/pulados; BUILD SUCCESS; inclui a implementação corrigida e a primeira versão do teste de adicionais |
| `task19-additional-final.log`, 15:28:22 | 1 teste focado, zero falhas/erros/pulados; BUILD SUCCESS; asserções ampliadas de contato e reenvio |
| XML Surefire de `PortalAccessIntegrationTest` | Nome do teste focado, execução e ausência de falhas confirmados |
| `task19-angular.log` | 99/99 aprovados |
| `task19-playwright.log` | 34/34 aprovados; desktop e mobile emulado, API mockada |
| `task19-build.log` | Build Angular/SSR aprovado; dois avisos CSS preexistentes, portal 7,35 kB e OS 7,00 kB |
| `task19-package.log`, 15:29:42 | JAR Spring Boot empacotado; BUILD SUCCESS |
| `git diff --check` | Executado pelo revisor, sem erros |

Os logs da API confirmam PostgreSQL 17.11 via Docker/Testcontainers. A execução focada complementa a suíte completa após ampliação das asserções; não representa um 131º teste distinto. As execuções foram realizadas pelo principal e verificadas pelo revisor nos logs/relatórios, sem duplicar suítes.

Playwright mockado não comprova E2E da nova API real. Não houve deploy, envio a clientes reais nem validação de TLS em domínio de produção nesta revisão. SMTP local real, imagens isoladas e consolidação móvel permanecem na tarefa 20.

## Recomendações

1. Consolidar as contagens finais e esta aprovação na documentação da tarefa, sob responsabilidade do principal.
2. Prosseguir com a publicação da tarefa 19 e a tarefa 20 conforme o fluxo autorizado; não há correção de implementação pendente desta rechecagem.

## Veredito

**APROVADO.** Ambos os P1 foram corrigidos e rechecados na fonte final; a suíte completa e a regressão ampliada passaram. Não restam bloqueadores para a conclusão/publicação da tarefa 19 pelo principal.

A única escrita deste revisor foi `tasks/prd-auditoria-autorizacao/19_task_review.md`, via `apply_patch`. Não foram alterados arquivos de implementação, publicações Git/Trello ou criados outros agentes.
