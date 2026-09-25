# Review: tarefa 15 — Notificações e e-mails transacionais

**Revisor:** task-reviewer independente

**Data:** 2026-09-25

**Arquivo da tarefa:** `tasks/prd-notificacoes/15_task.md`

**Base:** `master`, commit `4543b4a`; alterações locais rastreadas e não rastreadas

**Status:** APROVADO COM OBSERVAÇÕES

## Resumo

A implementação atende estruturalmente à outbox transacional, à deduplicação por evento/referência, ao isolamento por oficina e às tentativas SMTP limitadas. Os quatro gatilhos previstos estão conectados, e os eventos futuros permanecem limitados aos contratos, conforme o escopo das tarefas 17/18. Não identifiquei defeito concreto de isolamento ou duplicação concorrente no processamento examinado.

**Reconferência de C1/C2:** os dois bugs P2 da revisão inicial foram corrigidos e estão encerrados. O e-mail identifica a oficina e explica o acesso por placa/código; a troca de filtro invalida a página anterior antes de consultar a página zero. Os dois testes de regressão adicionados foram lidos. Não restam achados bloqueadores de código nesta revisão.

A reconferência foi restrita a `NotificationWorker.java`, `NotificationIntegrationTest.java`, `notification-page.component.ts` e `notification-page.component.spec.ts`. A avaliação dos demais arquivos permanece a da revisão inicial. Após as correções, o main informou sucesso em 91 testes Java, 49 Angular, build e 8 Playwright; esses resultados constam também de `tasks/prd-notificacoes/validacao.md`, lido nesta reconferência. Nenhum teste foi executado por este revisor.

Foram lidos integralmente a skill `task-review`, sua referência de padrões, seu template e os documentos PRD/Tech Spec/tarefa solicitados. As convenções existentes de Java/JDBC e DTOs em português foram consideradas; não foram transformadas em bloqueios de estilo.

## Arquivos revisados

Os caminhos desta seção são relativos a `C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas`. “Sem achado” significa ausência de problema concreto identificado na revisão, não certificação de cobertura de testes.

| Arquivo | Resultado |
| --- | --- |
| `README.md` | Sem achado no diff; descreve operação, limites e configuração |
| `oficinas-api/pom.xml` | Sem achado; dependências PostgreSQL embarcado restritas a testes |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/notificacoes/NotificationController.java` | Sem achado; identidade autenticada, validação e códigos HTTP |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/notificacoes/NotificationEvent.java` | Sem achado; quatro eventos atuais e três contratos futuros |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/notificacoes/NotificationSchedule.java` | Sem achado; lote limitado e chamada externa ao worker transacional |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/notificacoes/NotificationService.java` | Sem achado; gravação, listagem, leitura e reenvio |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/notificacoes/NotificationWorker.java` | C1 encerrado: nome, slug e instruções de acesso incluídos |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/adicional/AdditionalDecisionService.java` | Sem achado no diff; notificação por operação de decisão |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/adicional/AdditionalRequestService.java` | Sem achado no diff; notificação por versão enviada |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/ServiceOrderService.java` | Sem achado no diff; abertura e alteração de previsão |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/identidade/SecurityConfig.java` | Correção atual contempla `/api/notificacoes/**` |
| `oficinas-api/src/main/resources/application.yaml` | Sem achado no diff; ativação e intervalo do worker |
| `oficinas-api/src/main/resources/db/migration/V14__notificacoes_outbox.sql` | Sem achado; FKs compostas, unicidade e estados |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/notificacoes/NotificationIntegrationTest.java` | Novo teste de C1 com duas oficinas inspecionado; 12 testes da classe aprovados segundo o main |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/notificacoes/NotificationPolicyTest.java` | Elegibilidade, backoff, transporte e conteúdo inspecionados |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/support/TestPostgres.java` | Sem achado; Docker padrão e alternativa embarcada por propriedade |
| `oficinas-api/src/test/resources/application.properties` | Sem achado; worker e limpeza agendada desativados nos testes |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/adicional/AdditionalDecisionIntegrationTest.java` | Adaptação de banco e correção `Timestamp` verificadas |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/adicional/AdditionalDecisionServiceTest.java` | Adaptação de dependência inspecionada |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/adicional/AdditionalRequestIntegrationTest.java` | Adaptação de banco inspecionada |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/adicional/AdditionalRequestServiceTest.java` | Asserção do evento enviado inspecionada |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/cadastro/CustomerVehicleIntegrationTest.java` | Adaptação de banco inspecionada |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/identidade/AuthIntegrationTest.java` | Adaptação de banco inspecionada |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/oficina/ShopIntegrationTest.java` | Adaptação de banco inspecionada |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/ordem/ServiceOrderIntegrationTest.java` | Adaptação de banco inspecionada |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/portal/PortalAccessIntegrationTest.java` | Adaptação de banco e obtenção atual do slug verificadas |
| `oficinas-app/src/app/app.component.ts` | Sem achado no diff; navegação da oficina na nova rota |
| `oficinas-app/src/app/app.routes.ts` | Sem achado no diff; rota protegida e carregamento sob demanda |
| `oficinas-app/src/app/layout/office-sidebar.component.ts` | Sem achado no diff; acesso à central e nome acessível |
| `oficinas-app/src/app/notificacoes/notification.service.ts` | Sem achado; paginação, leitura/reenvio e CSRF |
| `oficinas-app/src/app/notificacoes/notification.service.spec.ts` | Contratos HTTP inspecionados |
| `oficinas-app/src/app/notificacoes/notification-page.component.ts` | C2 encerrado: dados invalidados e carga iniciada na página zero |
| `oficinas-app/src/app/notificacoes/notification-page.component.html` | C2 resolvido pelo estado do componente; sem alteração de template necessária |
| `oficinas-app/src/app/notificacoes/notification-page.component.css` | Sem achado concreto na inspeção estática |
| `oficinas-app/src/app/notificacoes/notification-page.component.spec.ts` | Nova regressão de erro/recuperação na página zero inspecionada |
| `oficinas-app/e2e/notifications.spec.ts` | Fluxos com API simulada inspecionados; execução delegada ao main |
| `tasks/prd-notificacoes/prd.md` | Lido integralmente |
| `tasks/prd-notificacoes/techspec.md` | Lido integralmente |
| `tasks/prd-notificacoes/15_task.md` | Lido integralmente |

Também foram consultados, como contexto e sem lhes atribuir defeitos fora do diff: `docs/prompt.md`, a seção 15 de `docs/tasks.md`, `TransactionalEmail`, repositórios de OS/adicionais/clientes, configuração de segurança e SMTP, `PageResult` e o fluxo de entrada do portal.

## Problemas encontrados

### Problemas Críticos

Nenhum problema crítico aberto. Os dois achados funcionais P2 da revisão inicial foram encerrados nesta reconferência.

### Problemas Major

Nenhum problema major aberto nos trechos reconferidos.

### Problemas Minor

Sem apontamentos obrigatórios de estilo. Os padrões Java/JDBC existentes e os DTOs em português foram respeitados na avaliação.

Como melhoria não bloqueadora de cobertura, considerar revogação somente de `email_verificado_em`, nova verificação do mesmo endereço, atualização de contato concorrente com o worker e validação dos instantes reais de backoff. Os testes examinados verificam troca de endereço, inativação e exclusão mútua entre workers; não demonstram todos esses cenários adicionais. Isso é um limite da evidência, não um defeito comprovado.

## Achados encerrados nesta reconferência

### C1 — [P2] Identificador da oficina no e-mail — SOLUCIONADO

**Problema anterior:** o aviso direcionava o cliente a `/acompanhar` sem fornecer o slug obrigatório para iniciar o acesso por código.

**Correção conferida:** [NotificationWorker.java:26](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-api/src/main/java/br/com/gestao/oficinas_api/notificacoes/NotificationWorker.java:26>) associa a notificação à sua oficina por `JOIN oficina o ON o.id=n.oficina_id` e carrega nome/slug. A composição em [NotificationWorker.java:48](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-api/src/main/java/br/com/gestao/oficinas_api/notificacoes/NotificationWorker.java:48>) inclui ambos e orienta informar identificador e placa para receber o código. A URL continua sem token e o envio não concede autorização de serviços. Os bloqueios da outbox e a revalidação do contato continuam presentes.

**Regressão inspecionada:** [NotificationIntegrationTest.java:124](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-api/src/test/java/br/com/gestao/oficinas_api/notificacoes/NotificationIntegrationTest.java:124>), `emailProvidesTheCorrectShopIdentifierRequiredByThePortal`, cria duas oficinas, processa dois avisos e captura os corpos. Exige exatamente uma mensagem com cada slug sem o slug da outra oficina e verifica nome, URL do portal, instrução sobre placa e ausência de `token=`.

**Conclusão:** C1 encerrado pela inspeção da correção e da regressão adicionada. O main informou que a execução posterior passou, incluindo os 12 testes de `NotificationIntegrationTest`.

### C2 — [P2] Dados anteriores após falha na troca de filtro — SOLUCIONADO

**Problema anterior:** uma falha ao consultar “Não lidos” podia reapresentar itens e paginação de “Todos”, porque o filtro mudava sem invalidar `data`.

**Correção conferida:** [notification-page.component.ts:32](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-app/src/app/notificacoes/notification-page.component.ts:32>) limpa `data` e o feedback e aguarda `load(0)`. Em caso de erro, a página anterior permanece invalidada; o botão de atualização existente usa o fallback zero. O contador de requisições de `load()` continua impedindo que uma resposta anterior sobreponha o filtro atual.

**Regressão inspecionada:** [notification-page.component.spec.ts:51](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/oficinas-app/src/app/notificacoes/notification-page.component.spec.ts:51>), `clears the previous filter after failure and retries from page zero`, prepara uma página anterior de índice 2 com aviso lido, rejeita a consulta após `filter(true)`, verifica `data=null` e erro e simula a recuperação usando o mesmo cálculo de página do botão de atualização. Confirma consulta/página zero e limpeza do erro.

**Conclusão:** C2 encerrado pela inspeção da correção e da regressão adicionada. O main informou sucesso nos 49 testes Angular após essa alteração.

## Destaques positivos

- `NotificationService.record()` exige transação existente; os quatro gatilhos registram o evento dentro da transação de negócio. O SMTP fica no worker, depois da publicação dos dados pelo commit.
- A chave única `(oficina_id, evento, referencia)` e o retorno após conflito impedem outbox adicional em replays, inclusive quando um contato inicialmente não verificado passa a ser verificado depois.
- As referências são específicas do evento: ID da OS na abertura, ID da versão adicional no envio, ID da operação na decisão e OS/versão na previsão. Substituir adicional gera rascunho; o próximo envio recebe referência própria.
- As FKs compostas garantem coerência de oficina entre aviso, OS, cliente e outbox. Lista/leitura/reenvio usam a oficina da identidade autenticada; IDs de outra oficina não habilitam mutação.
- O worker usa `REQUIRES_NEW` por linha e `FOR UPDATE OF e SKIP LOCKED`. O `FOR SHARE` no contato mantém a validação de endereço, atividade e instante de verificação durante a entrega.
- Falhas SMTP geram apenas código estável, cinco tentativas por ciclo e intervalos de 1/5/15/60 minutos. O reenvio exige `FALHOU`, preserva identidade e histórico e aplica o intervalo mínimo por atualização condicional.
- Templates não interpolam notas internas, CPF, fotos ou segredos. A interface distingue aceitação SMTP de leitura e solicitação de reenvio de envio concluído.
- A documentação reconhece corretamente a janela de duplicação após aceitação remota e antes do commit. Isso é a semântica at-least-once expressamente aceita pela Tech Spec, não um bug deste review.

## Conformidade com padrões

| Critério | Avaliação |
| --- | --- |
| Java/JDBC e contratos em português | Compatíveis com as convenções do projeto; sem bloqueio de estilo |
| Transações, deduplicação e isolamento | Sem defeito concreto identificado na inspeção e nos testes disponíveis |
| REST/HTTP e CSRF | Rota atual autorizada ao proprietário; mutações obtêm CSRF e não aceitam tenant do navegador |
| Logs e dados sensíveis | Erros persistidos como códigos; DTO da central não expõe destinatário/payload interno |
| Angular/TypeScript | Serviços tipados; estado de C2 corrigido e regressão adicionada |
| Templates e acesso ao portal | C1 corrigido; identificação da oficina e autenticação por código preservadas |
| Testes | Evidências e limitações discriminadas abaixo; sem execução duplicada pelo revisor |

## Evidências de validação e limitações

Por instrução explícita do solicitante, **não executei Maven, Angular, build nem Playwright**, tanto na revisão inicial quanto nesta reconferência. A etapa de execução da skill foi atendida pela análise dos testes e das evidências produzidas pelo main.

As evidências anteriores eram 90 testes Java, 48 Angular e 8 Playwright. Elas não foram usadas para presumir aprovação dos novos testes. Depois de C1/C2, o main comunicou a conclusão da nova execução e atualizou [validacao.md](<C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas/tasks/prd-notificacoes/validacao.md>), que foi lido pelo revisor.

| Evidência após C1/C2 | Origem e alcance |
| --- | --- |
| Java: 91 testes, zero falhas, erros ou skips | Resultado comunicado pelo main e registrado em `validacao.md`; execução `mvn -Dtest.database=embedded test`, PostgreSQL 17.6 efêmero e migrações V1–V14. Log indicado pelo main: `oficinas-api/target/task15-tests.log` |
| `NotificationIntegrationTest`: 12 testes aprovados | Quantidade e sucesso comunicados pelo main; inclui a nova regressão de identificação de duas oficinas |
| Angular: 49 testes aprovados | Resultado comunicado pelo main e registrado em `validacao.md`; inclui a nova regressão de falha/recuperação ao trocar o filtro |
| Build de produção aprovado | Comunicado pelo main e registrado em `validacao.md`; dois avisos CSS preexistentes de portal/OS, não atribuídos ao diff |
| Playwright central/portal: 8 aprovados | Comunicado pelo main e registrado em `validacao.md`; API simulada, desktop/mobile. O registro também informa aprovação na repetição dos quatro cenários da central |
| Capturas desktop/mobile e largura de 320 px | Inspecionadas pelo main, conforme seu registro. O revisor não executou sessão visual independente |
| Playwright completo com serviços reais | A tentativa anterior foi interrompida por ausência de backend/Mailpit. Os oito cenários aprovados não representam aprovação dessa suíte completa |

Na revisão inicial, foram lidos diretamente o log Maven de 90 testes e os XML Surefire então existentes. Nesta reconferência, os novos resultados acima são atribuídos ao main e ao documento de validação; não se afirma nova execução ou inspeção dos logs pelo revisor.

A ausência anterior de `/api/notificacoes/**` na segurança, as fixtures `Instant`/JDBC e a obtenção de slug por `/api/auth/me` já estavam corrigidas na revisão inicial e não são achados abertos. Não foi reaberta investigação integral nem foram atribuídas falhas preexistentes à tarefa.

O SMTP é mockado nos testes; entrega em provedor externo e domínio remetente de produção não foram validados. Aceitação SMTP não comprova leitura. A janela at-least-once entre aceitação remota e commit permanece a limitação expressamente aceita pela Tech Spec. Pronto/encerramento/avaliação continuam limitados aos contratos desta tarefa.

## Recomendações

1. Manter no registro de entrega as evidências posteriores a C1/C2 e os limites de SMTP/E2E descritos acima.
2. Considerar futuramente os cenários adicionais de cobertura indicados em “Problemas Minor”; eles não bloqueiam este parecer.

## Veredito

**APROVADO COM OBSERVAÇÕES.** C1 e C2 estão solucionados e encerrados. Não há solicitação de alteração de código pendente deste revisor. A análise das correções e dos novos testes é compatível com os resultados posteriores comunicados pelo main: 91 Java, 49 Angular, build e 8 Playwright aprovados.

As observações restantes são os limites documentados de SMTP externo e E2E com serviços reais, além das sugestões não bloqueadoras de cobertura. Não há bloqueio remanescente de review para prosseguir com o fluxo de publicação já autorizado.

Único arquivo alterado por este revisor nesta reconferência: `tasks/prd-notificacoes/15_task_review.md`. Nenhum código, fixture, configuração ou outro documento foi editado.
