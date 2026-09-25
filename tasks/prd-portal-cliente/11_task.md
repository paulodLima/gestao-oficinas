# Tarefa 11.0 — Portal do cliente e galeria de todas as etapas

Trello: https://trello.com/c/yjRbzLTo

## Visão geral

Mostrar serviço ativo, status, previsão, pendências, atualização e galeria cronológica abaixo do status.

## Dependências

- [x] 06 — fluxo de status e linha do tempo.
- [x] 08 — upload privado e publicação de fotos.
- [x] 10 — acesso seguro do cliente.
- [x] 12 — previsão de conclusão e atrasos.

## Subtarefas

- [x] 11.1 Criar consulta do serviço ativo autorizado com dados públicos mínimos.
- [x] 11.2 Construir cabeçalho com oficina/veículo, status, previsão, pendências e última atualização.
- [x] 11.3 Exibir galeria completa abaixo do status, filtro por etapa, zoom/deslize e linha do tempo.
- [x] 11.4 Implementar estados sem OS ativa e acesso expirado.
- [x] 11.5 Testes unitários: ordenação e filtros; integração: nenhum campo/foto interno; E2E: fotos antigas permanecem na mudança de status.

## Critérios de aceite

- Todas as fotos publicadas da OS permanecem após troca de etapa.
- Filtro, zoom e deslize disponíveis.
- Nenhum campo interno na resposta.
- Sem módulo antes/depois.
- Sem OS ativa mostrar mensagem e contato.
- Não incluir histórico completo encerrado.

## Entrega e validação

Status: concluída em 25/09/2026 na branch `codex/tarefa-11-portal-cliente-galeria`.
