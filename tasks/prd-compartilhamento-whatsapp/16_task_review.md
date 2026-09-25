# Review: Task 16 — Compartilhamento manual pelo WhatsApp

**Revisor**: Codex, atuando como task-reviewer independente, sem delegação
**Data**: 2026-09-25
**Arquivo da task**: `16_task.md`
**Branch**: `codex/tarefa-16-whatsapp`
**Base/HEAD conferido**: `aec05615c2ac2ff2cd6bec6f4be93f6945913af5`
**Status final**: **APROVADO**
**Re-revisão focada**: 2026-09-25 — C1 e M1 corrigidos; evidências e parecer final ao término deste documento.

> Histórico preservado: as seções abaixo, até “Veredito inicial”, registram o estado anterior às correções. Seus apontamentos e números de testes não representam pendências atuais. A seção “Re-revisão focada de C1/M1” registra a conferência das correções e prevalece como parecer final.

## Resumo

O fluxo implementa mensagem mínima, abertura manual do WhatsApp, cópia com alternativa selecionável e revogação sem afirmar envio ou entrega. O backend preserva autorização por oficina/OS, hash do token, validade e revalidação das sessões; a emissão e a revogação adquirem o mesmo lock na OS.

Foi reproduzida uma falha funcional bloqueante no consumo do fragmento: abrir outro link na mesma aba de `/acompanhar` não inicializa novamente o componente. O novo token não é consumido nem retirado da barra, e a tela mantém a OS anterior ou o formulário de acesso. Os testes existentes aprovados não cobrem essa navegação. Há também uma observação formal de padrões, não bloqueante por si só.

A revisão seguiu a leitura integral de `.agents/skills/task-review/SKILL.md`, `references/code-standards.md`, `assets/review-artifact-template.md` e dos quatro documentos solicitados da tarefa. A skill `executar-task` foi consultada para situar a etapa obrigatória de revisão; nenhuma implementação, delegação ou atualização de Git/Trello foi realizada. Não foi encontrado `CLAUDE.md` aplicável. npm/Angular e Java 21/Maven prevaleceram sobre os comandos Bun genéricos da skill.

## Arquivos Revisados

Escopo: diff do workspace contra `aec0561`, incluindo todos os arquivos novos relevantes listados por `git ls-files --others --exclude-standard`. Os números abaixo referem-se ao conteúdo revisado no workspace, antes de eventuais correções. Referências de código são relativas à raiz do projeto.

| Arquivo | Status | Problemas |
|---------|--------|-----------|
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/ordem/ServiceOrderAccessController.java` | OK | 0 |
| `oficinas-api/src/main/java/br/com/gestao/oficinas_api/portal/PortalAccessController.java` | OK no diff | 0 |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/portal/PortalAccessIntegrationTest.java` | OK | 0 |
| `oficinas-app/src/app/ordem/manual-share.ts` | Observação de padrões | M1 |
| `oficinas-app/src/app/ordem/manual-share.spec.ts` | OK | 0 |
| `oficinas-app/src/app/ordem/order-share.component.ts` | Observação de padrões | M1 |
| `oficinas-app/src/app/ordem/order-share.component.html` | OK | 0 |
| `oficinas-app/src/app/ordem/order-share.component.css` | OK | 0 |
| `oficinas-app/src/app/ordem/order-share.component.spec.ts` | OK | 0 |
| `oficinas-app/src/app/ordem/service-order-page.component.ts` | OK no diff | 0 |
| `oficinas-app/src/app/ordem/service-order-page.component.html` | OK | 0 |
| `oficinas-app/src/app/portal/portal-access.component.ts` | Crítico funcional | C1 |
| `oficinas-app/src/app/portal/portal-access.component.spec.ts` | Falta regressão da navegação por fragmento | Vinculado a C1 |
| `oficinas-app/e2e/manual-share.spec.ts` | Falta regressão da navegação por fragmento | Vinculado a C1 |
| `oficinas-app/e2e/portal.spec.ts` | OK no diff | 0 |
| `README.md` | OK no diff e contexto do fluxo | 0 |
| `docs/tasks.md` | Dependência 10 e tarefa 16 conferidas | 0 |
| `docs/techspec.md` | Contratos de acesso e alteração da tarefa conferidos | 0 |
| `tasks/prd-compartilhamento-whatsapp/16_task.md` | Lido integralmente | 0 |
| `tasks/prd-compartilhamento-whatsapp/prd.md` | Lido integralmente | 0 |
| `tasks/prd-compartilhamento-whatsapp/techspec.md` | Lido integralmente | 0 |
| `tasks/prd-compartilhamento-whatsapp/validacao.md` | Lido integralmente; evidências diferenciadas abaixo | 0 |

Também foram inspecionados os contratos de `ServiceOrderService`, segurança/CSRF/cookies, política e autorização do portal, autorização de adicionais, configuração de rotas/renderização e os relatórios Surefire. Não foram tratados como regressões desta entrega os desvios estruturais já existentes nesses arquivos.

## Problemas Encontrados

### Problemas Críticos

#### C1 — [P2] Consumir também tokens recebidos por mudança de fragmento na mesma aba

**Arquivo/linhas**: `oficinas-app/src/app/portal/portal-access.component.ts:182–186`.
**Severidade**: CRÍTICO na classificação da skill por funcionalidade quebrada; prioridade P2. **Bloqueia aprovação.**

O token é lido exclusivamente de `route.snapshot` em `ngOnInit()`. Depois de limpar a URL para `/acompanhar`, navegar na mesma aba para `/acompanhar#token=OUTRO_TOKEN` altera apenas o fragmento. O documento e o componente permanecem em uso; `ngOnInit()` não executa novamente. Portanto, a troca via POST e `replaceState()` não acontecem para esse segundo token.

**Reprodução executada**: Chromium headless com o frontend local existente, todas as APIs simuladas, tokens sintéticos e tráfego externo bloqueado. Nenhum WhatsApp ou backend real foi chamado.

1. Abrir `/acompanhar#token=A` e aguardar a OS A. A troca acontece e a URL fica `/acompanhar`.
2. Na mesma página/aba, navegar para `/acompanhar#token=B` sem recarregar.
3. Conferir POSTs de troca, URL, número de carregamentos do documento e título da OS.
4. Recarregar para comprovar que B funciona quando ocorre uma nova inicialização.

| Momento | POSTs de troca acumulados | Carregamentos do documento | Resultado observado |
|---------|---------------------------|---------------------------|---------------------|
| Primeiro link | A | 1 | OS A; URL limpa |
| Segundo link na mesma aba | Somente A | 1 | Continua OS A; `#token=B` permanece na barra |
| Após recarregar | A e B | 2 | OS B; URL limpa |

Foi reproduzida também a variante mobile, com emulação Pixel 7: entrar primeiro em `/acompanhar` sem sessão e depois abrir um link com fragmento na mesma aba deixou o formulário visível, o token na URL e **zero POSTs de troca**.

**Impacto**: o link recebido não abre o acompanhamento solicitado até um reload; quem já acompanhava outra OS continua vendo o contexto anterior. A remoção imediata do token prevista na especificação também deixa de ocorrer. Esta evidência não demonstra acesso a uma OS sem autorização nem vazamento do fragmento em requisição HTTP; demonstra falha de navegação, contexto exibido e limpeza da URL.

**Correção sugerida**: reagir às mudanças do fragmento/parâmetros durante a vida do componente, além da entrada inicial, e remover o token antes das operações assíncronas. Limpar o contexto exibido ao iniciar outra autenticação e proteger as respostas anteriores para que não restaurem a OS errada. Ao implementar a observação da rota, não interpretar a própria limpeza da URL como uma nova tentativa concorrente de restauração. Preservar o suporte à query legada e o reload com sessão HTTP.

**Regressão necessária**: acrescentar cenários de `/acompanhar` já aberto → link A e de A → B sem reload, em desktop/mobile. Asserir o POST com B, URL limpa, dados de B e descarte de respostas atrasadas de A. Os testes atuais em `portal-access.component.spec.ts:25–64` instanciam o componente para cada cenário; `e2e/manual-share.spec.ts:85–112` cobre primeira entrada e reload, sem a mudança de fragmento no mesmo documento.

### Problemas Major

#### M1 — [P3] Ajustar nomes e separar consulta de mutação nos auxiliares novos

**Arquivos/linhas**: `oficinas-app/src/app/ordem/manual-share.ts:1–13` e `oficinas-app/src/app/ordem/order-share.component.ts:106–109`.
**Severidade**: MAJOR formal conforme a classificação de violações de padrões da skill; **não bloqueante isoladamente**.

Os auxiliares `trackingUrl`, `trackingMessage` e `whatsappUrl` não começam com verbo. Além disso, `validLink()` aparenta consultar validade, mas chama `expireLink()`, limpando URL, validade e mensagens. São divergências concretas das regras de nomes de funções e de separação entre consulta e mutação. Não foi identificado defeito funcional causado por elas nos chamadores atuais.

**Sugestão**: usar nomes como `buildTrackingUrl`, `buildTrackingMessage` e `buildWhatsAppUrl`. Tornar o predicado puro e deixar a expiração explícita nos comandos de copiar/abrir e no timer, preservando a mensagem ao usuário. Exemplo de consulta pura:

```typescript
private isLinkValid(): boolean {
  return this.active && !!this.url() && Date.parse(this.expires()) > Date.now();
}
```

### Problemas Minor

Nenhum problema minor adicional encontrado. Melhorias opcionais de cobertura estão nas recomendações, sem inferir falha do lock a partir da ausência de um cenário.

## Destaques Positivos

- `manual-share.ts:1–13`: composição fixa sem parâmetros de cliente/OS, token codificado no fragmento e destino fixo `https://wa.me/?text=...`, sem telefone predefinido.
- `order-share.component.html:16–25`: abertura por âncora com `noopener noreferrer`/`no-referrer`, aviso explícito de envio manual e ausência de confirmação de entrega. O fluxo não escreve na outbox.
- `order-share.component.ts:25–88,104–122`: revisão de estado por OS/status, descarte de respostas antigas, limpeza na destruição/expiração, bloqueio de ações simultâneas e tratamento honesto de falha na revogação.
- `order-share.component.html:12–18,25–27`: campo readonly selecionável, alternativa à Clipboard API, revogação disponível sem token local e mensagens de status/erro acessíveis.
- `ServiceOrderAccessController.java:41–83`: transações de criação/revogação usam o mesmo lock da OS filtrado por oficina, inclusive quando ainda não existe link. O token mantém 32 bytes aleatórios e somente seu hash é persistido.
- `PortalAccessController.java:274–289,351–369`: revalidação de prazo, revogação e encerramento em cada acesso derivado. A alteração do caso sem concessão evita invalidar a sessão do proprietário apenas por consultar o portal sem grant.
- `PortalAccessIntegrationTest.java:147–239`: cobertura real de emissão, escopo, revogação, expiração, reemissão, encerramento, sessão do proprietário e duas emissões concorrentes. A autorização dos adicionais continua passando por escopo da OS e código independente.

## Conformidade com Padrões

| Padrão | Status |
|--------|--------|
| Padrões de Código | Observação M1. Identificadores novos de produção em inglês, arquivos Angular em kebab-case, sem `any` introduzido, sem novos métodos de produção acima de 50 linhas ou componente novo acima de 300 linhas. Legado maior e nomes longos de testes não foram elevados a bloqueadores desta tarefa. |
| Parâmetros, condicionais e organização | Código novo de produção com até três parâmetros, sem flag booleana nova para selecionar comportamento, retornos antecipados e sem aprofundamento excessivo de condicionais. Comentários novos são pontuais e explicativos. |
| Constantes | Prazo do link centralizado em `LINK_DURATION`; nenhuma nova constante numérica de segurança dispersa na implementação de produção. |
| TypeScript/Angular | Compilador Angular e TypeScript dos testes aprovados; ciclo de vida do fragmento precisa da correção C1. React não se aplica. |
| REST/HTTP e isolamento | CSRF preservado; APIs de proprietário isoladas por oficina; concessão por link restrita à OS; falhas mantêm respostas de erro. |
| Token e logging | Sem persistência nova em storage do navegador, sem token em logs adicionados e com política no-referrer existente. Limpeza na entrada inicial correta; mudança posterior de fragmento falha em C1. |
| Concorrência | Mesmo lock por OS na emissão e revogação; teste PostgreSQL confirma uma única emissão não revogada após dois POSTs concorrentes. |
| Restauração de sessão | Reload sem token coberto; visitante sem grant recebe formulário sem destruir a sessão do proprietário. Navegação no mesmo documento permanece descoberta em C1. |
| Testes | Evidências existentes aprovadas e typechecks independentes aprovados; reprodução adicional encontrou C1. |

## Validação realizada

| Verificação | Evidência e resultado |
|-------------|----------------------|
| Java 21/Maven | Conferidos os 22 relatórios `oficinas-api/target/surefire-reports/TEST-*.xml`: **94 testes, 0 falhas, 0 erros, 0 skips**. A suíte `PortalAccessIntegrationTest` contém sete casos, incluindo concorrência e preservação da sessão do proprietário. Maven completo não repetido. |
| Compilação Angular/template | Executado em `oficinas-app`: `node node_modules/@angular/compiler-cli/bundles/src/bin/ngc.js -p tsconfig.app.json --noEmit`. Exit code 0. |
| Typecheck dos testes | Executado em `oficinas-app`: `node node_modules/typescript/bin/tsc -p tsconfig.spec.json --noEmit`. Exit code 0. |
| Angular unitários | **65 aprovados**, conforme execução já informada e `validacao.md`; suíte completa não repetida por este revisor. |
| E2E existentes | **14 aprovados, 7 desktop/7 mobile**, conforme execução informada e `validacao.md`; `.last-run.json` também indica `passed`, mas não comprova sozinho a contagem. São APIs simuladas, como documentado. |
| Build | Aprovação e dois avisos CSS preexistentes conforme evidência fornecida; build completo não repetido. |
| Diagnóstico adicional | Duas execuções isoladas de Chromium/Playwright com mocks, desktop e mobile, confirmaram C1. Scripts em memória; nenhum teste-fonte, screenshot ou relatório de teste foi gravado pelo revisor. |
| Diff | `git -c safe.directory='C:/Users/rafaelp/Documents/ChatGPT/Projetos Oficinas' diff --check aec0561`: exit code 0; apenas avisos de conversão LF/CRLF. |

A revisão não substitui as evidências visuais em andamento. Não houve deployment, envio real, manipulação de contatos, modificação de banco real, commit, merge ou atualização no Trello. A única edição de arquivo realizada pelo revisor é este artefato, via `apply_patch`.

## Recomendações

1. Corrigir C1 e acrescentar regressão de mudança de fragmento na mesma aba, protegendo também respostas assíncronas do contexto anterior. Reexecutar os testes afetados e a compilação Angular antes da nova revisão.
2. Resolver M1 como ajuste de padrões, sem alterar a semântica de expiração/cópia/abertura.
3. Como reforço opcional, acrescentar emissão versus revogação concorrentes em PostgreSQL e verificar os tokens retornados, além da contagem de linhas. O teste existente cobre duas emissões; a inspeção não encontrou violação de exclusão mútua no código atual.

## Veredito inicial

**MUDANÇAS SOLICITADAS.** C1 é reproduzível e impede aprovar a tarefa 16 no estado revisado. Os testes aprovados sustentam os demais fluxos, mas não exercitam a navegação por fragmento sem recriar o componente. Após corrigir e comprovar esse cenário, solicitar nova revisão do diff correspondente. M1 é uma observação formal não bloqueante isoladamente. Nenhuma correção foi aplicada ao código, conforme a restrição de escrita desta revisão.

## Re-revisão focada de C1/M1 — 2026-09-25

**Escopo autorizado**: somente as correções de C1/M1, seus testes associados e as evidências atualizadas em `validacao.md`. A revisão integral não foi repetida. Branch e HEAD permanecem `codex/tarefa-16-whatsapp` e `aec05615c2ac2ff2cd6bec6f4be93f6945913af5`; foram avaliadas as correções presentes no workspace. Nenhuma ação de Git/Trello foi executada além das consultas locais de Git.

### Encerramento dos achados

| Achado | Situação final | Evidência da correção |
|--------|----------------|----------------------|
| C1 — P2, bloqueante na revisão inicial | **CORRIGIDO** | `portal-access.component.ts:188–253,292–333`; novos testes unitários em `portal-access.component.spec.ts:71–122` e E2E em `e2e/manual-share.spec.ts:115–179`. Reprodução independente aprovada em desktop/mobile. |
| M1 — P3, observação formal | **CORRIGIDO** | `manual-share.ts:1–13` e `order-share.component.ts:77–109`; importações, chamadas e testes de composição atualizados. |

**C1:** a inscrição em `NavigationEnd` e `NavigationSkipped` lê a URL efetiva por `Location.path(true)`, permitindo consumir também um token repetido depois da limpeza da barra. `consumeLink()` incrementa a revisão, remove o token e limpa imediatamente autenticação, oficina, OS, veículos, fotos, atualizações e estado do desafio anterior, antes de enfileirar a troca. A fila serializa autenticações por link; `contextRevision` protege restauração/autenticação e, em conjunto com `serviceRevision`, impede que cargas anteriores substituam os dados atuais. A destruição encerra a inscrição e invalida a revisão pendente. A limpeza da URL sem token não inicia outra restauração pelo observador.

Os novos testes unitários verificam uma carga antiga respondendo depois do novo contexto e uma autenticação por link pendente antes da troca seguinte. Os E2E verificam a sequência formulário → A → B → B, com apenas um carregamento do documento, e a navegação para B enquanto a carga de A está retida. As asserções conferem URL limpa, POSTs de troca, contexto exibido e ausência dos dados antigos.

**M1:** os auxiliares agora se chamam `buildTrackingUrl`, `buildTrackingMessage` e `buildWhatsAppUrl`. `isLinkValid()` é uma consulta pura. A expiração ocorre explicitamente nos comandos de copiar/abrir e no timer; permanecem os testes de expiração, prevenção da abertura inválida e mensagens sem confirmação fictícia de envio.

### Verificações desta rodada

| Verificação | Resultado e origem da evidência |
|-------------|--------------------------------|
| Reprodução independente de C1 — desktop | **PASS**. Formulário → A → B → B na mesma aba: três trocas, um documento carregado, URL final `/acompanhar` e OS correspondente ao último link. |
| Reprodução independente de C1 — mobile | **PASS**, emulação Pixel 7. Mesma sequência e mesmas asserções, sem reload. |
| Compilador Angular/template | Executado novamente: `node node_modules/@angular/compiler-cli/bundles/src/bin/ngc.js -p tsconfig.app.json --noEmit`. Exit code 0. |
| Typecheck dos testes | Executado novamente: `node node_modules/typescript/bin/tsc -p tsconfig.spec.json --noEmit`. Exit code 0. |
| Angular unitários | **67 aprovados**, conforme evidência atualizada em `validacao.md` e execução informada pelo implementador; suíte completa não repetida pelo revisor. |
| E2E existentes e regressões | **18 aprovados, 9 desktop/9 mobile**, conforme evidência atualizada. `.last-run.json` conferido com `passed` e nenhuma falha, sem atribuir a esse arquivo isolado a comprovação da contagem. |
| Build | Aprovação com dois avisos CSS preexistentes, conforme evidência atualizada; build completo não repetido pelo revisor. |
| Java | Mantida a evidência anterior de **94 testes, zero falhas/erros/skips**. Backend fora do escopo das correções; Maven não repetido. |

As reproduções independentes usaram Chromium/Playwright em memória, frontend local, APIs simuladas e tokens sintéticos, com tráfego externo bloqueado. Nenhum teste-fonte, screenshot ou relatório de execução foi gravado pelo revisor. Não houve impedimento de ambiente nesta rodada.

O incidente intermediário com overlay Vite/HMR foi explicitamente registrado em `validacao.md`, seguido de uma rodada completa 18/18 aprovada, sem ignorar cenário nem forçar clique sobre o overlay. Ele não foi reproduzido nas verificações focadas desta rodada e não constitui uma pendência funcional demonstrada.

### Parecer final

**APROVADO.** C1 e M1 estão encerrados. A reprodução que sustentou o bloqueio inicial agora passa em desktop e mobile, e os novos testes cobrem troca de fragmento sem reload e respostas atrasadas. Nenhum novo achado bloqueante foi identificado no escopo das correções. Não há pendência desta revisão para o merge; este parecer não executa merge, publicação ou envio real.

A única alteração feita pelo revisor nesta rodada foi a atualização deste artefato via `apply_patch`, preservando integralmente os achados e o veredito inicial como histórico.
