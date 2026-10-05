# Resumo de Tarefas de Implementação — Evolução comercial e equipe

## Tarefas

- [x] 1.0 **E01 — Estabilizar jornadas essenciais e consolidar baseline** — P0 — [Detalhes](1_task.md) — [Validação](validacao-e01.md) — [Trello](https://trello.com/c/KDeVBHBM)
- [ ] 2.0 **E02 — Cadastrar equipe e implementar acesso individual** — P0 — [Detalhes](2_task.md) — [Trello](https://trello.com/c/WBnIIlS1)
- [ ] 3.0 **E03 — Aplicar permissões e atribuir responsáveis às OS** — P0 — [Detalhes](3_task.md) — [Trello](https://trello.com/c/ELD6iTk8)
- [ ] 4.0 **E04 — Criar área mobile da equipe com busca por placa** — P0 — [Detalhes](4_task.md) — [Trello](https://trello.com/c/OVx4UWtv)
- [ ] 5.0 **E05 — Configurar modelos de fluxo e regras de evidência** — P0 — [Detalhes](5_task.md) — [Trello](https://trello.com/c/hJ6n7PFK)
- [ ] 6.0 **E06 — Concluir etapa com fotos e publicação confirmada** — P0 — [Detalhes](6_task.md) — [Trello](https://trello.com/c/l2uOae8E)
- [ ] 7.0 **E07 — Tratar rede instável, reenvio e conflitos de atualização** — P0 — [Detalhes](7_task.md) — [Trello](https://trello.com/c/xqsH7wey)
- [ ] 8.0 **E08 — Atualizar portal com progresso real e comunicação clara** — P1 — [Detalhes](8_task.md) — [Trello](https://trello.com/c/r218P6eN)
- [ ] 9.0 **E09 — Consolidar orçamento autorizado e resumo de entrega** — P1 — [Detalhes](9_task.md) — [Trello](https://trello.com/c/EkHuZQ6w)
- [ ] 10.0 **E10 — Criar indicadores históricos e painel de gargalos** — P1 — [Detalhes](10_task.md) — [Trello](https://trello.com/c/pjFEIPpo)
- [ ] 11.0 **E11 — Confiabilizar notificações e visibilidade das falhas** — P0 — [Detalhes](11_task.md) — [Trello](https://trello.com/c/87e9sqwj)
- [ ] 12.0 **E12 — Preparar armazenamento privado, cotas e recuperação** — P0 — [Detalhes](12_task.md) — [Trello](https://trello.com/c/JNeNEuLq)
- [ ] 13.0 **E13 — Preparar produção, segurança e monitoramento** — P0 — [Detalhes](13_task.md) — [Trello](https://trello.com/c/4DclLIDs)
- [ ] 14.0 **E14 — Entregar onboarding, importação e operação de assinaturas** — P1 — [Detalhes](14_task.md) — [Trello](https://trello.com/c/0rBkMTQH)
- [ ] 15.0 **E15 — Executar piloto comercial e validar lançamento** — P1 — [Detalhes](15_task.md) — [Trello](https://trello.com/c/Tv8O1INQ)

## Contexto e sequência

Escopo e lista aprovados pelo usuário. Fonte: [PRD](prd.md) e [Tech Spec](techspec.md).
IDs E01–E15 identificam esta fase, sem renumerar o MVP. E01 está concluída; E02–E15 permanecem pendentes.
Sequência linear válida: E01 → E02 → E03 → E04 → E05 → E06 → E07 → E08 → E09 → E10 → E11 → E12 → E13 → E14 → E15.
Prioridade não elimina dependências. E09 pode iniciar após E03; E12 após E07. E14 possui subentregas verificáveis.
A área mobile utilizável está em E04–E07, mas produção/piloto depende dos gates posteriores de segurança.

## Dependências e rastreabilidade

| ID | Prioridade | Dependências | Requisitos |
| --- | --- | --- | --- |
| E01 | P0 | Base MVP | RF01 |
| E02 | P0 | E01 | RF02 |
| E03 | P0 | E02 | RF03 |
| E04 | P0 | E03 | RF04 |
| E05 | P0 | E01, E03 | RF05, RF06 |
| E06 | P0 | E04, E05 | RF06, RF08 |
| E07 | P0 | E06 | RF07 |
| E08 | P1 | E05, E07 | RF08 |
| E09 | P1 | E01, E03 | RF09 |
| E10 | P1 | E05, E08, E09 | RF10 |
| E11 | P0 | E02, E06, E09 | RF11 |
| E12 | P0 | E01, E07 | RF12 |
| E13 | P0 | E03, E07, E11, E12 | RF13 |
| E14 | P1 | E03, E12, E13 | RF14 |
| E15 | P1 | E08, E09, E10, E11, E12, E13, E14 | RF15 |

## Definição de concluído

- Critérios atendidos, testes unitários/integração/E2E pertinentes executados e evidências registradas.
- Isolamento, privacidade, concorrência e revogação verificados nos módulos afetados.
- Documentos e card sincronizados; sem marcar conclusão apenas por existir interface.
- Sem credenciais/dados pessoais em evidências.
- Serviços externos e validações manuais não disponíveis permanecem como pendência explícita.

## Decisões e limites

- Funcionário: alcance configurável pelo dono, padrão somente OS atribuídas.
- Publicação é permissão separada e confirmação explícita; foto vinculada à etapa concluída.
- Fotos obrigatórias conforme tipo de etapa; esperas exigem motivo.
- Login individual com convite e senha; sessão inicial de 12h, sem promessa de offline.
- Manter MVP e avaliações ocultas; não recriar módulos existentes.
- Plano comercial/preço/provedores definitivos e contato com oficinas exigem decisão antes da ativação.
- Não provisionar recursos pagos, executar piloto, commitar ou fazer push nesta entrega.

Documento consolidado para leitura: [docs/tasks-evolucao-comercial-equipe.md](../../docs/tasks-evolucao-comercial-equipe.md).
