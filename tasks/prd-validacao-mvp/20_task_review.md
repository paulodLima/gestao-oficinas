# Revisão: Tarefa 20 — Validação móvel e entrega do MVP

**Revisor:** Codex, revisão independente no papel de task-reviewer

**Data:** 25/09/2026 — revisão incremental após correções e migração autorizada

**Arquivo da tarefa:** `20_task.md`

**Branch/base:** `codex/tarefa-20-validacao-mvp` / `7d1262b`

**Status da revisão de código:** **APROVADO COM OBSERVAÇÕES**

**Fechamento da revisão:** retestes E2E pós-migração aprovados e documentação final consolidada; sem bloqueador para o fechamento pelo implementador, observados os limites abaixo. Commit, merge e Trello não foram executados nem verificados pelo revisor.

## Resumo

Os três achados da revisão anterior foram corrigidos: R20-01 (metadados nas fotos), R20-02 (concorrência de reenvios) e R20-03 (recarga em OS encerrada). Não foi encontrado bloqueador de implementação nos diffs incrementais examinados. A migração autorizada para Angular 20 está implementada, o lockfile corresponde ao manifesto e a auditoria de produção fornecida registra zero vulnerabilidades.

Foram confirmados 144 testes Java, 111 testes Angular e build Angular 20 aprovados. O revisor executou também o typecheck dos E2Es com TypeScript 5.9.3, exit 0, e repetiu em memória a reprodução da fila: pico de três uploads, quatro concluídos e nenhuma vaga ou espera retida.

A aprovação refere-se ao código e às evidências indicadas neste relatório. Os novos logs confirmam também **44 E2E e oito cenários QA aprovados após a migração**, incluindo recuperação de fotos no histórico encerrado. O risco residual das dependências de desenvolvimento está separado dos resultados funcionais abaixo e registrado na documentação final. Não há aprovação irrestrita de produção.

## Escopo e procedimento

A primeira revisão seguiu a skill `task-review`, sua referência integral de padrões e seu modelo de relatório, com leitura dos documentos solicitados da tarefa e dos requisitos globais relevantes. A revisão incremental concentrou-se nos novos diffs, nos testes de regressão e na migração. As alterações ainda estavam no diretório de trabalho, com HEAD na base `7d1262b`.

Somente `tasks/prd-validacao-mvp/20_task_review.md` foi editado pelo revisor, usando `apply_patch`. Não foram alterados fontes, testes, configurações, dependências, Git, Docker, Trello ou ambiente principal. Não houve acesso/envio a e-mails nem criação de subagentes. Não foram executados Bun ou outra suíte completa. A auditoria de dependências foi conduzida pelo implementador; esta revisão leu os resultados locais existentes, sem duplicar a investigação.

## Arquivos revisados

Caminhos relativos à raiz. Os arquivos do primeiro ciclo continuam abrangidos pela revisão; a coluna indica onde houve rechecagem incremental.

| Arquivos/grupo | Resultado |
| --- | --- |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/PhotoStorage.java` | Rechecado integralmente; normalização, leitura legada, limites e limpeza em falha |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/ServicePhotoController.java`; `oficinas-api/src/main/java/br/com/gestao/oficinas_api/portal/PortalAccessController.java` | Diffs e caminhos de leitura/autorização rechecados |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/ordem/PhotoStorageTest.java`; `oficinas-api/src/test/java/br/com/gestao/oficinas_api/support/PhotoFixture.java`; `oficinas-api/src/test/java/br/com/gestao/oficinas_api/portal/PortalAccessIntegrationTest.java` | Novas regressões examinadas |
| `oficinas-api/pom.xml`; `oficinas-api/Dockerfile` | Dependências de imagem e correção do diretório de fotos |
| `oficinas-app/src/app/ordem/service-order-page.component.ts`, `.html`, `.spec.ts`; `service-order.service.ts` | Fila compartilhada, recuperação e preservação da intenção de publicação rechecadas |
| `oficinas-app/package.json`, `package-lock.json`, `angular.json` | Migração; versões resolvidas, consistência do manifesto/lock e alterações de configuração |
| `oficinas-app/src/app/app.config.server.ts`; `oficinas-app/src/app/ordem/order-share.component.ts` | Migrações de providers SSR e importação de DOCUMENT |
| `oficinas-app/src/app/ordem/inspection.css`, `additional-request.component.ts`; `oficinas-app/src/app/layout/office-sidebar.component.ts`, `.spec.ts` | Revisados no primeiro ciclo: foco/toque, labels e logout |
| `oficinas-app/src/app/portal/additional-decision.component.ts`, `.spec.ts`, `portal-access.component.ts`; `oficinas-app/src/app/auth/auth-page.component.css`; `oficinas-app/src/app/avaliacao/review.css`; `oficinas-app/src/styles.css` | Revisados no primeiro ciclo: erro recuperável, contraste e controles |
| `oficinas-app/e2e/auth.spec.ts`, `customer-vehicle.spec.ts`, `service-order.spec.ts`, `shop.spec.ts` | Seletores atualizados sem retirar asserções funcionais |
| `oficinas-app/e2e-qa/mvp.spec.ts`, `public-states.spec.ts`; `playwright.qa.config.ts`; `tsconfig.e2e.json` | Alcance real conferido; typecheck reexecutado com TS 5.9.3 |
| `docker/docker-compose.qa.yml`, `verify-qa-backup.ps1`, `QA.md`; `README.md`; `.gitignore`; `oficinas-app/.gitignore` | Isolamento, restauração e instruções revisados; documentação atualizada para a migração |
| `docs/tasks.md`; `tasks/prd-auditoria-autorizacao/19_task.md`; documentos e três imagens em `tasks/prd-validacao-mvp/` | Rastreabilidade e evidências do primeiro ciclo; sem validar estado remoto do Trello |

Contexto adicional da migração: `oficinas-app/src/main.server.ts`, `src/server.ts`, `src/app/app.routes.server.ts`, `tsconfig.json` e Dockerfile do frontend. Nenhum deles foi modificado pelo revisor.

## Problemas críticos

**Nenhum bloqueador de implementação permanece identificado no escopo incremental examinado.**

### R20-01 · P1 preexistente — CORRIGIDO

O problema original era a entrega dos bytes brutos da câmera na ampliação do portal, incluindo EXIF/GPS. A transformação anterior corrigia somente a miniatura.

**Implementação conferida:**

- `PhotoStorage.java:32`–`:44`: novos uploads são normalizados antes da gravação; chaves `view-*` identificam as representações produzidas pelo próprio armazenamento. A imagem de visualização tem limite de 2048px e a miniatura, 480px.
- `PhotoStorage.java:50`–`:66`: arquivos legados passam pela normalização na leitura, sem sobrescrever o original. Não existe retorno alternativo do original bruto se a normalização falhar.
- `PhotoStorage.java:68`–`:95`: dimensões são verificadas antes da decodificação, com limite de 40 MP; orientação ocorre após redução, e a reencodificação usa um BufferedImage novo sem copiar metadados.
- `ServicePhotoController.java:30` e `PortalAccessController.java:247`: ambos consomem `readDisplay()` e usam o tipo de conteúdo da representação retornada. Isso evita declarar WebP ao devolver PNG. A autorização existente ocorre antes da leitura.
- `pom.xml`: TwelveMonkeys `imageio-webp:3.12.0` fornece decodificação WebP; JPEG é reencodificado em JPEG, PNG/WebP em PNG.

**Evidência:** `PhotoStorageTest.java:38`, `:51`, `:60` e `:72` cobrem EXIF/GPS, arquivo legado preservado, WebP novo/legado e dimensões acima do limite. A fixture `PhotoFixture.cameraJpeg()` contém orientação 6, fabricante sintético e GPS; o teste verifica que os metadados existem na entrada e desaparecem na saída. Os oito casos de orientação continuam presentes.

`PortalAccessIntegrationTest.java:330` acrescenta teste HTTP real da ampliação com chave nova e legada: status 200, JPEG válido, dimensões/posição de cor corretas, ausência de EXIF/GPS e original legado intacto. Os testes anteriores de autorização e revogação permanecem. O conjunto Java terminou com 144 casos aprovados, incluindo 13 de PhotoStorage e 16 de PortalAccessIntegrationTest.

**Limite da conclusão:** não foi alegada sanitização física dos arquivos legados; a proteção é aplicada na resposta de visualização. A confiança no prefixo `view-` é compatível com o contrato atual: a chave é gerada pelo servidor, sem escolha pelo upload do usuário.

## Problemas major

**Nenhum major novo foi identificado na revisão incremental.**

### R20-02 · P2 — CORRIGIDO

`service-order-page.component.ts:300`–`:321` usa `uploadQueued()` tanto nas novas seleções quanto nos retries. As vagas são compartilhadas por componente e transferidas ao próximo waiter no `finally`; a fila não perde uma vaga ao ocorrer falha. A proteção contra retry duplicado do mesmo ID e a chave/publicação originais foram mantidas.

`service-order-page.component.spec.ts:109`–`:132` exercita quatro reenvios e uma nova seleção, libera vagas por sucesso e erro e verifica que a fila progride sem pendências. A reprodução independente do revisor executou os métodos atuais transpilados em memória com respostas RxJS controladas:

```json
{"initialSubscriptions":3,"peakSubscriptions":3,"totalSubscriptions":4,"remainingSlots":0,"remainingWaiters":0}
```

Trata-se de validação da coordenação local, não de teste de carga do servidor. O escopo do limite é o componente, como implementado; não é um limite global entre todas as abas ou usuários.

### R20-03 · P2 — CORRIGIDO

`service-order-page.component.html:129`–`:142` separa leitura/recarga das áreas de envio e edição desabilitadas. O botão de recarga fica habilitado no histórico, enquanto upload, edição da vistoria e remoção de fotos permanecem bloqueados para OS encerrada.

`service-order-page.component.spec.ts:135`–`:149` usa o template real com serviços controlados, verifica `:disabled` no DOM, clica em “Recarregar fotos” e confirma um segundo GET bem-sucedido. O teste de preservação das fotos já carregadas no encerramento também permanece.

`oficinas-app/e2e-qa/mvp.spec.ts:172`–`:182` acrescenta a regressão integrada: após encerrar a OS, aborta uma consulta de fotos, recarrega a página, verifica a recarga habilitada e o seletor de upload desabilitado; após clicar, recupera a foto, mantém a remoção desabilitada e registra `historico-encerrado.png`. As quatro jornadas da execução final passaram.

## Migração Angular e risco de dependências

A autorização do usuário para Angular 19 → 20 foi comunicada nesta conversa. A pendência anterior de autorização foi superada; não deve constar como decisão ainda aguardada na documentação final.

| Componente | Versão resolvida no lockfile |
| --- | --- |
| Angular core/common/compiler/forms/router/platforms | 20.3.32 |
| Angular CLI / SSR / build-angular | 20.3.37 |
| TypeScript | 5.9.3 |

O revisor verificou que as dependências diretas do manifesto coincidem com a raiz do lockfile e que os pacotes Angular resolvidos examinados pertencem à série 20. A atualização de `provideServerRendering(withRoutes(...))` e de `DOCUMENT` é coerente com as migrações registradas em `task20-angular-migration.log`. `main.server.ts` já fornece BootstrapContext, e as rotas continuam com RenderMode.Client; não foi introduzida renderização compartilhada de dados privados.

`task20-npm-prod-audit-after.log` registra **zero vulnerabilidades** na auditoria das dependências de produção. Esse resultado substitui, para a árvore atual, o registro anterior de 10 afetados em Angular 19. É um resultado da auditoria naquela execução, não prova de ausência absoluta de vulnerabilidades.

`task20-npm-audit-after.log` registra **cinco entradas moderadas na cadeia de desenvolvimento** build-angular/build-webpack → webpack-dev-server → sockjs → uuid. Não são cinco exploits independentes demonstrados neste produto. Segundo a análise comunicada pelo implementador, o advisory GHSA-w5hq-g745-h8pq afeta uuid <11.1.1 nos métodos v3/v5/v6 com buffer, enquanto o caminho examinado em `sockjs/lib/transport.js:9`, `:37` usa v4() sem buffer. Assim, não foi demonstrada essa exploração no caminho observado; as cinco entradas continuam presentes, não corrigidas. Essa análise de dependências não foi duplicada pelo revisor nem deve ser generalizada para toda ferramenta ou uso futuro.

O Dockerfile final copia somente o dist e executa o servidor de produção, sem copiar node_modules de desenvolvimento ou iniciar ng serve. Não foi atribuído ao runtime um achado que o audit classificou apenas nessa cadeia dev, nem exigida outra migração major ou troca de builder automaticamente. Manter o risco residual documentado e as ferramentas de desenvolvimento fora da exposição pública; audit produção zero não elimina riscos operacionais ou vulnerabilidades ainda desconhecidas.

## Evidências de validação

| Evidência | Resultado | Alcance |
| --- | --- | --- |
| `task20-api-final-security.log:856`, `:859` | **144 testes; zero falhas, erros ou skips; BUILD SUCCESS** | Pós-correção de fotos; inclui teste HTTP novo/legado |
| `task20-angular20-tests.log:120` | **111 SUCCESS** | Angular 20; inclui fila compartilhada e recuperação no DOM |
| `task20-angular20-build.log:63` | **Build concluído** | Artefatos browser/server; sem prerender de rotas privadas |
| Comando do revisor: `node node_modules/typescript/bin/tsc -p tsconfig.e2e.json --noEmit` | **Exit 0** | Typecheck de E2E/configurações com TS 5.9.3; nenhuma emissão de arquivos |
| Reprodução em memória do revisor | **Pico 3; quatro concluídos; fila esvaziada** | Revalidação específica de R20-02 |
| `task20-npm-prod-audit-after.log` | **0 vulnerabilidades** | Snapshot de produção obtido pelo implementador e lido pelo revisor |
| `task20-docker-final-security.log` | Imagens QA api/app construídas; API Healthy; app Started | Log pós-migração; inicialização não substitui smoke/E2E pós-build |
| Confirmação final do implementador e `qa-report.md` | Docker QA healthy; porta 14220 com HTTP 200 via Chrome; processos E2E exit 0 | Smoke informado pelo implementador; jornadas nas imagens finais corroboradas pelos logs abaixo; sem nova execução pelo revisor |
| `task20-regression-angular20.log:53` | **44 passed (49.5s)** | Pós-migração; conserva a distinção entre dez integrações reais e 34 testes com APIs simuladas |
| `task20-qa-angular20-final.log:21` | **8 passed (1.1m)** | Pós-migração; quatro jornadas integradas + quatro matrizes públicas; desktop, Android emulado, WebKit/iOS emulado e viewport estreito |
| `task20-qa-angular20.log:26`, `:44`, `:46` | Primeira execução: sete aprovados e uma falha do harness | Falha de leitura de corpo via CDP; preservada como evidência, superada pela execução final após ajuste do teste |
| `task20-backup-restore.log` | 36 tabelas e 22 arquivos iguais após restauração | Evidência anterior do ensaio sintético; não é teste completo da aplicação restaurada |

As suítes completas existentes não foram reexecutadas pelo revisor. O typecheck e a reprodução em memória foram os únicos testes incrementais próprios. O diff examinado não apresentou erro em `git diff --check`; avisos de conversão LF/CRLF não foram tratados como falhas funcionais.

O ajuste do harness em `oficinas-app/e2e-qa/mvp.spec.ts:71`–`:79` foi inspecionado: mantém a exigência de POST 201 e heading da OS, consulta a mesma API com a sessão autenticada e exige exatamente uma OS persistida. Remove apenas a dependência do corpo descartado de `response.json()` via CDP; não repete a criação nem substitui a aplicação por uma resposta simulada. O typecheck foi novamente executado após esse ajuste, exit 0. Os logs anteriores `task20-playwright-regression.log` e `task20-qa-final.log` permanecem históricos; a comprovação pós-migração vem dos novos logs acima.

## Observações não bloqueantes para o código

1. **Retestes concluídos:** 44 E2E e oito cenários QA aprovados nas execuções pós-migração, com exit 0 confirmado pelo implementador. Os logs finais foram incorporados ao fechamento, mantendo o registro da primeira falha do harness e de sua correção. Não há reteste completo pendente nas evidências lidas nesta revisão.
2. **Documentação consolidada:** a releitura final de `qa-report.md` e `bugfix-report.md` confirmou versões, contagens finais, resolução dos três achados e risco residual dev. README, techspec global, `docker/QA.md` e `20_task.md` já registram a migração autorizada; `bugs.md` registra também o ajuste do harness. Registros antigos permanecem históricos, sem atribuir execuções anteriores ao Angular 20. A publicação e a atualização administrativa de tarefa/merge/Trello cabem ao implementador e não integram as ações desta revisão.
3. **Cobertura de imagens:** os oito casos de transformação usam matriz 2×2; o JPEG EXIF/GPS real usa orientação 6. WebP usa uma amostra representativa. Esses testes não demonstram todas as variantes de arquivos/câmeras; o novo limite de dimensões e a sanitização têm regressões próprias.
4. **Acessibilidade e rede:** permanecem os limites do primeiro ciclo: axe e medidas de controles/estados selecionados, Tab entre seletores, latência/falha com imagem pequena. Isso não equivale a câmera física, leitor de tela, todos os links/lightbox ou transferência de foto grande sob banda limitada. A distinção de falha antes do envio no WebKit e resposta perdida após persistência no Chromium está correta.
5. **Padrões e orçamento:** classe de OS acima de 300 linhas, parâmetros posicionais e instruções compactadas são predominantemente legado. Não bloqueiam esta correção nem justificam refatoração geral. O build ainda avisa orçamento CSS de portal (7,40 kB) e OS (7,00 kB), sem falhar.
6. **Operação:** o backup prova igualdade dos dados sintéticos restaurados, não RPO/RTO nem uma jornada em aplicação conectada ao destino. HTTPS/proxy, SMTP externo, retenção e aparelhos físicos continuam fora da comprovação local.

## Destaques positivos

- Sanitização tanto para arquivos novos quanto legados, sem sobrescrita dos originais legados e sem caminho público alternativo para bytes brutos.
- Regressão HTTP confirma o contrato efetivamente entregue ao portal, além do teste isolado de armazenamento.
- Semáforo compartilhado cobre seleções/retries; teste inclui falha liberando vaga.
- Teste de recarga verifica a desabilitação herdada no DOM, que era a causa real de R20-03.
- Migração concentra mudanças automáticas necessárias, preserva renderização cliente das rotas e mantém o manifesto alinhado ao lock.
- Isolamento do QA, SMTP local e distinção entre testes reais, simulados e emulação permanecem documentados.

## Conformidade com padrões

| Critério | Avaliação |
| --- | --- |
| Código/arquitetura | Sem novo bloqueador; observações de estilo/legado |
| TypeScript/Angular | 111 testes, build e typecheck pós-migração aprovados |
| REST/HTTP e autorização | Contrato de conteúdo corrigido; autorização existente preservada |
| Privacidade de fotos | R20-01 resolvido para representações novas e legadas |
| Concorrência | R20-02 resolvido no componente |
| Recuperação de erro | R20-03 resolvido, com DOM testado |
| Dependências | Audit produção 0; cinco entradas moderadas dev permanecem, com análise de alcance comunicada pelo implementador |
| Logging/evidências | Dados sintéticos; sem achado de segredo real no material revisado |
| React | Não aplicável |
| Fechamento da revisão | E2E/QA pós-migração aprovados e documentação consolidada; publicação a cargo do implementador |

## Veredito

**APROVADO COM OBSERVAÇÕES para o escopo de código revisado. R20-01, R20-02 e R20-03 estão resolvidos; não foi identificado novo bloqueador de implementação.** A migração Angular 20 autorizada tem build, 144 testes Java, 111 Angular, typecheck, 44 E2E, oito cenários QA e audit de produção aprovados nas evidências acima.

**Revisão concluída, sem pendência bloqueante para o fechamento pelo implementador.** A documentação final incorpora os resultados e o risco residual dev; commit/merge/Trello permanecem sob responsabilidade do implementador. O resultado não é uma declaração irrestrita de aptidão para produção. Se houver nova mudança material, a conclusão deve ser reavaliada no respectivo escopo.
