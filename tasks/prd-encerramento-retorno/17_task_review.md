# Review: Task 17 — Encerramento, cancelamento e retorno do veículo

**Revisor**: Codex — task-reviewer independente

**Data**: 2026-09-25

**Arquivo da task**: `17_task.md`

**Branch/base**: `codex/tarefa-17-encerramento` / `80d36e4`

**Status**: APROVADO — P2 resolvido na re-revisão focada; nenhum achado aberto.

## Resumo

O encerramento possui confirmação explícita, controle de versão e transação abrangendo status, pendências, auditoria, revogação e outbox. Os fluxos de escrita analisados respeitam a trava da OS ou o UPDATE condicional à OS ativa. Não identifiquei vulnerabilidade nova, perda de decisões ou inversão de locks nos caminhos revisados. O único achado inicial, P2 no retorno dependente da primeira página dos cadastros, foi corrigido: o fluxo consulta veículo e responsável atual por ID e inclui ambos nas opções.

A re-revisão de 2026-09-25 limitou-se ao delta de retorno, aos métodos individuais do serviço de cadastro, seus testes e à adaptação do mock E2E. Não repetiu a revisão integral nem executou novamente as suítes. O backend permanece sem alteração desde a revisão inicial, conforme informado pela execução principal.

A revisão seguiu integralmente `task-review/SKILL.md`, sua referência de padrões e template, e `java-springboot/SKILL.md`, contextualizados pelos padrões existentes e pelas instruções desta revisão. JDBC, DTOs em português e Maven/npm foram respeitados; não foram exigidas migração para JPA, adoção de Bun ou refatorações de estilo fora do escopo.

## Arquivos revisados

Caminhos relativos à raiz do workspace; todos os arquivos de produção alterados/novos foram lidos integralmente, além dos testes e dos contratos relacionados.

| Arquivo | Resultado |
|---------|-----------|
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/OrderClosureController.java` | Sem achado |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/OrderClosurePolicy.java` | Sem achado |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/OrderClosureService.java` | Sem achado |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/ServiceOrderRepository.java` | Sem achado novo |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/ServiceOrderService.java` | Sem achado novo |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/InspectionService.java` | Sem achado novo |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/ServicePhotoController.java` | Sem achado novo |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/adicional/AdditionalDecisionService.java` | Sem achado novo |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/adicional/AdditionalRequestService.java` | Sem achado novo |
| `oficinas-api/src/main/resources/db/migration/V15__encerramento_ordem_servico.sql` | Sem achado |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/ordem/OrderClosurePolicyTest.java` | Revisado |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/ordem/OrderClosureIntegrationTest.java` | Revisado |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/adicional/AdditionalRequestServiceTest.java` | Revisado |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/portal/PortalAccessIntegrationTest.java` | Revisado |
| `oficinas-app/src/app/ordem/order-closure.component.ts` | Sem achado |
| `oficinas-app/src/app/ordem/order-closure.component.html` | Sem achado |
| `oficinas-app/src/app/ordem/order-closure.component.css` | Sem achado |
| `oficinas-app/src/app/ordem/order-closure.component.spec.ts` | Revisado |
| `oficinas-app/src/app/ordem/service-order-page.component.ts` | P2 resolvido; delta de retorno aprovado |
| `oficinas-app/src/app/ordem/service-order-page.component.html` | Revisado em conjunto com os componentes filhos |
| `oficinas-app/src/app/ordem/service-order-page.component.spec.ts` | Regressões de paginação, vínculo atual, 404, responsável inativo e ausência de criação automática revisadas |
| `oficinas-app/src/app/ordem/service-order.service.ts` | Sem achado |
| `oficinas-app/src/app/cadastro/customer-vehicle.service.ts` | Novos métodos `customer(id)`/`vehicle(id)` revisados; sem achado |
| `oficinas-app/src/app/cadastro/customer-vehicle.service.spec.ts` | Novo arquivo: dois testes HTTP dos GETs individuais revisados |
| `oficinas-app/e2e/closure.spec.ts` | Mock adaptado aos GETs individuais; API simulada |
| `README.md`, `docs/techspec.md`, `docs/tasks.md` | Diffs da tarefa revisados |
| `tasks/prd-encerramento-retorno/{17_task.md,prd.md,techspec.md,tasks.md,validacao.md}` | Requisitos e evidências conferidos |

Também foram conferidos, em contexto, autorização/CSRF e sessões do portal, emissão/revogação de links, repositório de adicionais, notificações, fotos, cadastro/vínculo atual, configurações Maven/npm/Playwright e o suporte a PostgreSQL embarcado. A documentação geral de requisitos da tarefa 17 foi confrontada com o escopo local.

## Achado inicial e resolução

### [P2 — RESOLVIDO] Consultar veículo e responsável por ID no retorno

**Classificação original**: MAJOR funcional no requisito 17.4; sem implicação de segurança. Não há pendência após a correção.

**Local corrigido**: `oficinas-app/src/app/ordem/service-order-page.component.ts:124–146`; consultas individuais em `oficinas-app/src/app/cadastro/customer-vehicle.service.ts:20–24`.

**Defeito original**: `returnVisit()` procurava veículo e responsável somente nos primeiros 100 registros das listagens. Um vínculo válido fora dessa página deixava o formulário vazio e sem as opções necessárias para concluir o retorno.

**Correção conferida**: consulta sequencial `vehicle(order.veiculoId)` → `customer(vehicle.clienteId)`, usando os endpoints GET existentes. Os registros retornados substituem eventuais cópias antigas nas opções, sem depender de `items` da página zero. O preenchimento ocorre somente depois de conferir a seleção e a situação ativa do responsável; 404 mantém a OS selecionada e exibe mensagem específica. A nova OS continua exigindo preenchimento e envio explícitos, com relato e quilometragem vazios.

**Regressões conferidas**: `service-order-page.component.spec.ts:184–203` cobre as duas combinações solicitadas, com `totalElements=101` e `totalPages=2`: ambos fora da primeira página e somente o cliente fora dela. Os testes verificam GET por ID, inclusão nas opções, formulário sem dados operacionais antigos e nenhuma criação automática. Também foram conferidos os testes de responsável atual, 404 e inatividade, além dos dois testes HTTP em `customer-vehicle.service.spec.ts:9–23`. O guard de seleção no retorno foi inspecionado no código; o teste existente de seleção diferente refere-se a `orderClosed()`.

**Resultado**: P2 encerrado. Nenhum novo defeito identificado no delta revisado.

### Demais categorias

Nenhum achado aberto, crítico, major ou minor. Os avisos CSS preexistentes e convenções já estabelecidas não foram classificados como defeitos desta tarefa.

## Destaques positivos

- A trava da OS antecede solicitação/versão/desafio tanto na aprovação quanto na emissão de código; adicionais, vistorias e upload/remoção de fotos revalidam o estado sob a mesma trava.
- A transação de encerramento preserva versões e decisões anteriores, exige consentimento para cancelar pendências e grava o encerramento imutável com autor/data, evento, auditoria e notificação.
- Os testes concorrentes cobrem as duas ordens relevantes: encerramento vence e rejeita a aprovação em andamento; aprovação vence e sua decisão permanece após cancelar as pendências restantes. Há teste de rollback abrangendo links, desafios e notificação.
- O teste HTTP confirma isolamento entre oficinas, revogação da sessão de link já aberta, ausência de serviço ativo após fechar, manutenção da sessão por identidade e isolamento do link antigo na nova visita.
- A interface exige nova revisão após conflito, não repete silenciosamente o encerramento e ignora respostas atrasadas após troca de OS. O POST usa o mecanismo CSRF existente.

## Conformidade e requisitos

| Área | Avaliação |
|------|-----------|
| Spring/JDBC, transações e migração | Adequados ao projeto; V15 aditiva e fechamento atômico |
| REST/HTTP, segurança e isolamento | Sem problema novo identificado nos fluxos revisados |
| Angular/TypeScript | Compilação aprovada; P2 de retorno corrigido e coberto por regressões |
| 17.1 — confirmação e encerramento | Atendido nos cenários revisados |
| 17.2 — pendências e bloqueio de escrita | Atendido nos cenários revisados |
| 17.3 — revogação e preservação interna | Preservação/revogação conferidas no código e nos testes |
| 17.4 — retorno e isolamento do link | Atendido nos cenários revisados; retorno independente da paginação |
| 17.5 — notificações | Pronto/encerramento conectados à outbox existente |
| 17.6 — testes e revisão | Suítes aprovadas; revisão independente concluída com o P2 resolvido |

## Evidências de validação

- `oficinas-api/target/task17-evidence/task17-final.log`: **103 testes**, zero falhas, erros ou ignorados, `BUILD SUCCESS`. PostgreSQL real embarcado, sem Docker.
- `oficinas-api/target/task17-evidence/task17-focused.log`: **20 testes aprovados**. A falha antiga de mocks em `task17-tests.log`, preservado no mesmo diretório, está superada pela execução focada e pela regressão final.
- `oficinas-api/target/task17-evidence/task17-angular-review.log`: **81/81 testes aprovados**, `TOTAL: 81 SUCCESS`, após a correção do P2. O resultado anterior de 76 testes permanece preservado em `task17-angular.log`.
- `oficinas-api/target/task17-evidence/task17-build-review.log`: build novamente aprovado; geração do bundle concluída, com somente os dois avisos CSS preexistentes (portal 7,35 kB e página de OS 7,00 kB, orçamento 6 kB).
- E2E após a correção: **22/22 aprovados**, 11 desktop e 11 mobile, em 14 segundos, conforme resultado final informado pela execução principal. `oficinas-app/test-results/.last-run.json`, atualizado depois dos arquivos corrigidos, registra `passed`, sem falhas. Conferido o mock dos GETs individuais; o revisor não executou navegador nem repetiu E2E.
- Backend: mantida a evidência de **103 aprovados**; não houve alteração de backend na correção, conforme informado pela execução principal.
- `git diff --check` conferido novamente para os arquivos rastreados do delta, sem erros de whitespace; mensagens sobre normalização LF/CRLF não são falhas do check.

As suítes não foram duplicadas. A resolução foi validada por inspeção do delta, testes e resultados da execução principal, sem alegação de reprodução em navegador pelo revisor. Os testes de decisão usam transporte SMTP simulado; esta revisão não enviou mensagens nem alterou dados externos.

## Recomendação e veredito

**APROVADO**. A correção elimina a dependência de paginação e cobre os dois cenários do P2 original. Nenhuma correção obrigatória permanece identificada. O veredito combina a revisão integral anterior com esta re-revisão focada do retorno, sem reabrir o escopo de backend já validado.

Único arquivo escrito pelo revisor: `tasks/prd-encerramento-retorno/17_task_review.md`, via `apply_patch`. Nenhum código, outro documento, estado Git, navegador ou Trello foi alterado pelo revisor.
