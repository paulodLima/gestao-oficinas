# Review: Task 18 — Resumo de entrega e avaliação

**Revisor**: task-reviewer independente (skill `task-review`)
**Data**: 2026-09-25
**Arquivo da task**: `18_task.md`
**Base**: alterações não commitadas contra `90ffa0e`, incluindo arquivos novos
**Status**: APROVADO — após revisão focada de R1/R2

## Resumo

A implementação cobre convite independente, validade de sete dias, resumo com projeção restrita, avaliação única, consentimento separado, consulta privada e Google sem filtro por nota. As transações, restrições PostgreSQL, CSRF, separação do acompanhamento e proteção do destino Google estão bem direcionadas. A revisão inicial identificou dois problemas de troca de contexto; ambos foram corrigidos e revalidados. A revisão focada não encontrou novos problemas críticos ou major nas correções e alterações relacionadas.

Revisão limitada a leitura e diagnóstico. Nenhum código, teste, Git ou Trello foi alterado pelo revisor; este relatório é o único arquivo escrito. As convenções genéricas foram adaptadas ao projeto Spring/JDBC/Angular e aos DTOs em português existentes, sem exigir migração para JPA/Bun ou refatorações fora do escopo.

## Resultado da revisão focada

- **R1 resolvido:** `ReviewService.java:38,92` devolve `contexto` com o UUID do convite, sem torná-lo credencial. `ReviewController.java:31–36,45` exige o eco e compara com o grant capturado da sessão antes de chamar o serviço. Contexto ausente/divergente responde `409 AVALIACAO_CONTEXTO_ALTERADO`, sem gravação de avaliação ou auditoria. O identificador recebido não escolhe a OS: `service.submit` usa o mesmo UUID capturado, revalidado no banco. Assim, uma troca concorrente não pode redirecionar o envio para B depois da checagem de A.
- **UI de R1 resolvida:** `review-page.component.ts:63,72` captura o contexto do resumo exibido e remove esse resumo em conflito; `review.service.ts:8,23` tipa e transporta o campo. Não foi introduzido token em armazenamento persistente ou URL. O teste HTTP `ReviewIntegrationTest.java:189` usa o mesmo cookie entre duas oficinas, verifica ausência de avaliação/auditoria para ambas após envio obsoleto ou sem contexto e confirma que B continua funcional. O E2E `reviews.spec.ts:113` abre duas páginas no mesmo browser context e verifica conflito, remoção do formulário e envio correto em B.
- **R2 resolvido:** `review-page.component.ts:33,47` diferencia `null` de `''`; `#token=` e `#token` chegam à troca inválida, removem o fragmento e não recuperam o resumo anterior. `ReviewIntegrationTest.java:208` confirma que token vazio apaga o grant e impede leitura/envio posteriores. Os novos unitários cobrem inicialização e navegação na mesma instância; `reviews.spec.ts:127` cobre navegação por fragmento e nova página. A recarga sem parâmetro continua funcionando.
- **Validação final conferida nos logs:** API **121/121**, Angular **97/97**, E2E **34/34**, build Angular e empacotamento Maven aprovados após as correções. Nenhuma suíte longa foi reexecutada pelo revisor.

## Arquivos Revisados

Prefixo Java: `oficinas-api/src/main/java/br/com/gestao/oficinas_api/`.
Prefixo Angular: `oficinas-app/src/app/`.

| Arquivo | Status | Problemas |
|---------|--------|-----------|
| Java `avaliacao/ReviewController.java` | ✅ OK | R1 resolvido |
| Java `avaliacao/ReviewAccessService.java` | ✅ OK | — |
| Java `avaliacao/ReviewService.java` | ✅ OK | Contexto não autorizador acrescentado ao resumo |
| Java `avaliacao/ReviewPolicy.java` | ✅ OK | — |
| Java `avaliacao/ReviewOwnerController.java` | ✅ OK | — |
| Java `identidade/SecurityConfig.java` | ✅ OK | — |
| Java `notificacoes/NotificationWorker.java` | ✅ OK | — |
| Java `ordem/OrderClosureService.java` | ✅ OK | — |
| `oficinas-api/src/main/resources/db/migration/V16__avaliacao_entrega.sql` | ✅ OK | — |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/avaliacao/ReviewIntegrationTest.java` | ✅ OK | Regressões HTTP de R1/R2 acrescentadas e aprovadas |
| `oficinas-api/src/test/java/br/com/gestao/oficinas_api/avaliacao/ReviewPolicyTest.java` | ✅ OK | — |
| Angular `avaliacao/review-page.component.ts` | ✅ OK | R1/R2 resolvidos |
| Angular `avaliacao/review-page.component.html` | ✅ OK | — |
| Angular `avaliacao/review.service.ts` | ✅ OK | Contexto obrigatório no envio |
| Angular `avaliacao/review-page.component.spec.ts` | ✅ OK | Quatro regressões de R1/R2 acrescentadas e aprovadas |
| Angular `avaliacao/review.service.spec.ts` | ✅ OK | — |
| Angular `avaliacao/review-owner.component.ts` | ✅ OK | — |
| Angular `avaliacao/review-invitation.component.ts` | ✅ OK | — |
| Angular `avaliacao/review-invitation.component.spec.ts` | ✅ OK | — |
| Angular `avaliacao/review.css` | ✅ OK | — |
| Angular `app.component.ts`, `app.routes.ts`, `layout/office-sidebar.component.ts` | ✅ OK | — |
| Angular `ordem/service-order-page.component.ts`, `.html` | ✅ OK | — |
| `oficinas-app/e2e/reviews.spec.ts` | ✅ OK | Duas abas e token vazio cobertos em desktop/mobile |
| `README.md`, `docs/techspec.md` | ✅ OK | Fluxo e limitações documentados na revisão inicial |
| `tasks/prd-avaliacao-entrega/{prd.md,techspec.md,18_task.md,tasks.md}` | ✅ OK | Lidos integralmente; conclusão corretamente pendente |
| `tasks/prd-avaliacao-entrega/validacao.md` | ✅ OK | Evidências e limites documentados; atualizar parecer após correções |

## Problemas Encontrados

### 🔴 Problemas Críticos

Nenhum problema crítico aberto após a revisão focada. Os registros abaixo preservam os achados da versão anterior à correção; suas descrições, diagnósticos e linhas originais são históricos, não o comportamento atual.

#### R1 — [P1] RESOLVIDO — Vincular o envio ao contexto do resumo, não somente ao último convite da sessão

- **Local principal:** `oficinas-api/src/main/java/br/com/gestao/oficinas_api/avaliacao/ReviewController.java:30–33`.
- **Locais relacionados:** mesmo arquivo, linhas 26–27; `oficinas-app/src/app/avaliacao/review-page.component.ts:62–69`; `review.service.ts:5,22`.
- **Cenário:** abrir convite A na aba 1 e carregar o formulário; abrir convite B na aba 2 do mesmo navegador; voltar à aba 1 e enviar nota/comentário/consentimento. Ambas compartilham `OFICINAS_SESSION`. A segunda troca substitui `AVALIACAO_ENTREGA` por B, mas o formulário da aba 1 continua exibindo A. O corpo enviado não identifica o contexto esperado. O controller lê B da sessão e grava a resposta em B; a aba 1 ainda apresenta a confirmação junto do resumo de A.
- **Impacto:** atribuição incorreta de nota, comentário e consentimento ao cliente/OS/oficina de B, além de consumir sua única avaliação. Não exige requisições simultâneas: a sequência acima é suficiente. CSRF não impede o caso, pois ambas as páginas legítimas usam a mesma sessão. `revision`/`queue` só coordenam uma instância do componente, não outras abas ou instâncias após recriação da página.
- **Evidência:** leitura do controller e diagnóstico em memória executando o `ReviewPageComponent` real, transpilado com dependências e serviço simulados conforme o contrato atual: `{"displayedOrder":"A","writtenOrder":"B","consent":true}`. O diagnóstico não foi um teste HTTP real e não alterou arquivos. Os testes existentes cobrem troca na mesma instância, não duas abas.
- **Correção sugerida:** devolver no resumo um identificador de contexto/convite não autorizador e exigir seu eco no envio. Compará-lo ao grant atual no servidor antes de gravar, mantendo a autorização derivada da sessão e a revalidação no banco. Divergência deve falhar sem escrever, pedir reabertura do convite e remover o resumo obsoleto da UI. Não confiar em `ordemServicoId` enviado pelo cliente para selecionar livremente a OS; não substituir a checagem por uma segunda consulta prévia no frontend, que mantém a corrida.
- **Regressão necessária:** com o mesmo cookie, obter resumo A, trocar para B e enviar o contexto de A: retornar erro e deixar ambas as avaliações/auditorias vazias; o envio com contexto B continua válido. Cobrir também duas páginas no mesmo browser context e a mensagem/limpeza do formulário antigo. Preservar testes de replay idêntico e isolamento entre oficinas.

#### R2 — [P2] RESOLVIDO — Rejeitar convite explicitamente vazio em vez de restaurar sessão anterior

- **Local principal:** `oficinas-app/src/app/avaliacao/review-page.component.ts:47–50`.
- **Local relacionado:** mesmo arquivo, linhas 32–35.
- **Cenário:** com uma avaliação A já autenticada, abrir `/avaliar#token=` (ou `#token`) por link incompleto. `URLSearchParams.get('token')` retorna `''`; `if (token)` ignora a troca e busca o resumo da sessão anterior. Em uma navegação na mesma página, o outro `if (token)` nem reinicializa o componente. O servidor já sabe rejeitar uma string vazia e limpar o grant, mas não recebe a chamada.
- **Impacto:** um convite inválido é silenciosamente associado ao atendimento anterior, contrariando a regra de não restaurar o acesso anterior depois de receber outro convite inválido. Pode induzir avaliação/consentimento no atendimento errado, inclusive sem duas abas.
- **Evidência:** diagnóstico com o componente real e `route.snapshot.fragment = 'token='`: `{"exchangeCalls":0,"restoredOrder":"A"}`. O teste atual usa apenas a string não vazia `invalid`, que passa pelo caminho correto.
- **Correção sugerida:** distinguir ausência do parâmetro (`null`, recarga legítima de `/avaliar`) de parâmetro presente porém vazio (`''`, tentativa inválida). Usar verificação explícita `token !== null` nos dois pontos e encaminhar a string vazia ao fluxo que invalida o grant. Remover o fragmento e não restaurar o resumo anterior após a falha.
- **Regressão necessária:** início da página e navegação na mesma aba com `#token=`/`#token` devem tentar a troca, apresentar erro e não consultar/exibir A. Recarga sem parâmetro deve continuar restaurando a sessão válida. Conferir que a falha realmente limpa o grant no backend.

### 🟡 Problemas Major

Nenhum problema major aberto. As lacunas de teste associadas a R1 e R2 foram cobertas pelas regressões verificadas.

### 🟢 Problemas Minor

Nenhuma sugestão estética é condição de aprovação. Os avisos CSS preexistentes não foram classificados como regressão da tarefa 18.

## ✅ Destaques Positivos

- Convite criado na transação da entrega, não no cancelamento, sem estender a janela original por repetição; rollback integrado testado.
- Credencial com finalidade exclusiva, hash persistido, fragmento removido do endereço, ausência de token no payload persistido das notificações; reconstrução confere o hash emitido.
- Revalidação de expiração/revogação/estado/cliente/contato por requisição, sem conceder `PORTAL_CLIENTE` ou papel de proprietário. CSRF permanece ativo.
- Projeção explícita do resumo sem custos, CPF, placa, dados do cliente, fotos ou observações internas. Comentários são renderizados como texto, não HTML.
- `UNIQUE` por OS e lock do convite serializam envios concorrentes; replay normalizado idêntico retorna a primeira resposta, enquanto divergência não sobrescreve dados.
- Consentimento independente, falso por padrão, texto e instante registrados; não há publicação automática ou endpoint público de depoimentos.
- Destinos Google limitados a URLs HTTPS dedicadas, sem redirecionador genérico, parâmetros de nota/comentário ou credencial. Link fora da condição da nota e protegido com `noopener noreferrer`/`no-referrer`.
- Outbox preserva o fluxo de entrega em falha SMTP, revalida contato e cancela convites indisponíveis. Testes usam remetente simulado, sem mensagem real.

## Conformidade com Padrões

| Padrão | Status |
|--------|--------|
| Spring/JDBC/DTOs e convenções existentes | ✅ Compatíveis; sem demanda de refatoração cosmética |
| TypeScript/Angular | ✅ Configurações estritas; build aprovado confirmado no log |
| REST/HTTP, CSRF e isolamento entre oficinas | ✅ Contexto esperado comparado ao grant; sessão permanece a fonte de autorização |
| Logging e proteção de credenciais | ✅ Sem novos logs de token/comentário/contato |
| Expiração e revogação | ✅ Validade original/revalidação preservadas; token vazio invalida o grant |
| Privacidade/Google | ✅ Consentimento vinculado ao contexto correto; sem filtro por nota nem publicação automática |
| Testes existentes | ✅ Passes finais verificados, incluindo regressões R1/R2 |

## Evidências de validação

- `oficinas-api/target/task18-evidence/all-tests.log`: **121 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS`, concluído às **14:38:59 -03**. Maven com `-Dtest.database=embedded`, PostgreSQL 17.6 real temporário; não Bun. Relatório Surefire de `ReviewIntegrationTest`: **10/10**, incluindo os dois novos cenários HTTP.
- `oficinas-api/target/task18-evidence/angular-tests.log`: **97 SUCCESS**, passe final após quatro novas regressões.
- `oficinas-api/target/task18-evidence/e2e.log`: **34 passed (23.6s)**, incluindo 12 casos de avaliação (desktop/mobile) e 22 de regressão. As duas páginas são reais no mesmo browser context; a API é simulada. A proteção backend foi validada separadamente por HTTP/PostgreSQL com o mesmo cookie entre oficinas.
- `oficinas-api/target/task18-evidence/build.log`: build Angular pós-correções aprovado às **14:39**, com dois avisos CSS preexistentes (`portal-access` 7,35 kB e `service-order-page` 7,00 kB; orçamento 6 kB). Log conferido, sem duplicar execução.
- `oficinas-api/target/task18-evidence/package.log`: Maven `-DskipTests package`, `BUILD SUCCESS`, JAR Spring Boot gerado às **14:39:29 -03** após as correções. Log conferido; não substitui o passe de testes acima.
- Capturas mobile do formulário e desktop da central sem corte/overflow: inspeção informada pelo executor principal; não duplicada nesta revisão.
- `git diff --check`: sem erros de whitespace; somente avisos locais de conversão LF/CRLF.
- Na revisão inicial, dois diagnósticos sem gravação de arquivos confirmaram R1/R2 na versão anterior. Na revisão focada, foram lidos os contratos corrigidos e os testes novos, com os passes HTTP, Angular e E2E conferidos nos logs do executor.

## Recomendações

1. Manter as regressões de contexto compartilhado e token vazio na suíte.
2. Atualizar o documento de validação e os marcadores da tarefa com o parecer aprovado; prosseguir com o fluxo de publicação autorizado pelo usuário.

## Veredito

**APROVADO.** R1 e R2 corrigidos, com regressões pertinentes aprovadas e builds finais conferidos. Nenhum bloqueio crítico ou major remanescente no escopo revisado. O parecer libera a etapa de revisão pré-merge; não afirma que Git/Trello foram publicados ou que houve implantação/SMTP externo. Essas ações permanecem com o executor principal.
