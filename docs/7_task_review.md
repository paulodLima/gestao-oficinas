# Revisão: tarefa 7 — Painel operacional e Kanban

Revisor: task_reviewer · Data: 19/09/2026.
Fonte: [tasks.md](tasks.md), seção 7; [techspec.md](techspec.md).
Status: APROVADO COM OBSERVAÇÕES — correção verificada e evidências recebidas; consolidação final em andamento.

## Arquivos e escopo

Revisados DashboardController/Service/Repository, migração V8, componentes e serviço em oficinas-app/src/app/painel, rota protegida, integração de abertura direta da OS e contratos de previsão/status existentes.

## Achado P2

`dashboard.component.ts`, métodos `changeView`, `apply` e `fetch`: alternar lista/quadro chama `apply` com todos os campos atuais do formulário, inclusive uma busca ainda não aplicada, mantendo a página anterior. Em uma página posterior, digitar um cliente e apenas trocar a visualização pode aplicar a nova busca na página incorreta e exibir vazio. O botão Atualizar também lê o rascunho e conserva a página. Isso quebra a expectativa de mesma seleção entre lista e quadro e pode divergir dos filtros persistidos na URL.

Correção: manter filtros efetivamente aplicados separados do rascunho; alternância deve alterar apenas `view` e atualização deve consultar o estado aplicado. Mudanças de filtro explícitas começam na página zero. Incluir regressão com rascunho alterado sem aplicação e página posterior.

**Resolvido:** `appliedFilters` agora separa o estado consultado do formulário; `fetch` usa esse estado, `changeView` altera apenas o parâmetro de visualização e `changePage` reutiliza filtros aplicados. Teste de regressão cobre rascunho diferente da consulta e página posterior. Correção aceita após nova leitura do código.

## Pontos verificados

- Indicadores, cartões e subconsulta de etapa filtram oficina. Parâmetros de busca são vinculados, ordenação usa enum e paginação é validada.
- Snapshot usa transação read-only com repeatable read e um único instante para classificação de atraso.
- Tempo na etapa usa somente eventos STATUS; atualização geral usa updated_at. Mudanças de texto/previsão não reiniciam etapa.
- Indicadores são globais à oficina e a interface explicita que antecedem os filtros.
- Lista e quadro agrupam os mesmos cartões da página; colunas vazias e paginação são identificadas.
- Identificadores de requisição descartam respostas antigas; falha remove resultados antigos.
- Rota exige autenticação; detalhes de OS aplicam autorização no backend.
- Labels, indicação textual de atraso, foco visível, área de toque e lista móvel estão presentes no código. Verificação visual/móvel é responsabilidade do E2E em curso.

## Testes e padrões

Testes Angular inspecionados cobrem resposta fora de ordem, erro de atualização, agrupamento sem duplicação, duração e filtros não aplicados. Evidências recebidas do agente principal: Angular 31 testes aprovados; suíte API de 51 testes aprovada antes de acrescentar dois cenários de painel; classe ForecastDashboardIntegrationTest com 6 testes aprovada após esses cenários; build Docker Java 21/Angular aprovado; 2 E2E de painel aprovados com desktop e viewport móvel de 320 px, equivalência lista/quadro, abertura direta de OS, erro e vazio. Consolidação final das suítes ocorre após ajuste adicional de versão do rascunho da tarefa 12. Comandos Maven/Angular substituem exemplos Bun da skill; execução centralizada no agente principal. Nenhuma alegação de testes independentes ou prontidão de produção.

## Veredito

Correção aceita e sem bloqueadores restantes no escopo da tarefa 7. Registrar resultados consolidados finais antes do encerramento da entrega. O ajuste adicional de ForecastComponent mantém `draftVersion` ao paginar e foi verificado estaticamente; seu teste final está sendo executado pelo agente principal.
