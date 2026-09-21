# Validação — tarefa 7: painel operacional e Kanban

Data: 21/09/2026. Escopo e critérios: [tasks.md](tasks.md), seção 7. Revisão: [7_task_review.md](7_task_review.md).

Implementado: painel autenticado com indicadores globais da oficina, filtros por busca, situação, etapa, atualização e tempo na etapa; lista e Kanban usam a mesma página de cartões. Cada cartão informa placa, veículo, cliente, etapa, previsão, atraso, última atualização e duração da etapa. Não há porcentagem artificial nem arrastar cartões: a alteração de etapa continua no detalhe da OS, com histórico e justificativa quando aplicável.

Segurança e consistência: consultas sempre filtram a oficina da sessão; ordenação e paginação são validadas. O instantâneo usa uma transação de leitura com relógio único. A duração deriva somente do último evento de status, portanto texto ou previsão não reiniciam a etapa. Indicadores permanecem globais e a interface o informa. Filtros efetivamente aplicados ficam na URL; alternar lista/quadro ou atualizar não usa um rascunho ainda não aplicado.

Evidências executadas:

- Suíte da API: 53 testes aprovados, incluindo 6 testes HTTP/PostgreSQL para previsão e painel, isolamento por oficina, filtros, paginação, atraso, tempo de etapa e terminais.
- Suíte Angular: 32 testes aprovados, incluindo agrupamento, durações, respostas fora de ordem, falha de atualização, filtros em rascunho e versão de previsão concorrente.
- Build Docker da API Java 21 e Angular aprovado; V8 aplicada sem remoção de volumes.
- Playwright: 12 jornadas aprovadas (desktop e móvel), incluindo painel em lista/quadro, busca, estado vazio/erro, abertura direta da OS e viewport de 320 px sem overflow horizontal.

Limites conhecidos: o painel faz atualização manual e mostra o instante da consulta; não é monitoramento em tempo real. O portal do cliente, notificações e recursos de fotos continuam nas tarefas próprias.
