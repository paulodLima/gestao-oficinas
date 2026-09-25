# Validação da Tarefa 13 — Serviços adicionais

Data: 2026-09-25

## Entrega

- API autenticada para listar, criar, editar, enviar, substituir e cancelar solicitações adicionais.
- Persistência separada de solicitação, versões, itens e fotos, com isolamento por oficina e ordem de serviço.
- Valores em `numeric(15,2)`, quantidades em `numeric(12,3)` e arredondamento financeiro `HALF_UP`.
- Grupos dependentes validados como conjuntos de pelo menos dois itens.
- Envio congela a versão; substituição marca a anterior como `SUBSTITUIDA` e cria novo rascunho.
- Estados futuros de decisão (`PARCIALMENTE_DECIDIDA` e `DECIDIDA`) reservados para a tarefa 14.
- Auditoria transacional de criação, edição, envio, substituição e cancelamento.
- Tela responsiva na ordem de serviço para preparação, revisão, envio, histórico e cancelamento.

## Evidências automatizadas

- `npm run build`: aprovado. Permanecem apenas os avisos de orçamento CSS preexistentes do portal e da página de OS.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 39 testes aprovados.
- Suíte unitária Java: aprovada, incluindo precisão decimal, grupos, estados, OS encerrada e substituição.
- Compilação integral dos testes Java (`mvn test -DskipTests`): aprovada.
- `AdditionalRequestIntegrationTest`: criado e compilado para validar imutabilidade, substituição, histórico, auditoria e isolamento entre oficinas.

## Limitação do ambiente

O teste Testcontainers não foi executado localmente porque o serviço Docker desta estação está parado e não pôde ser iniciado sem privilégios administrativos. Ele está pronto para execução em CI ou em uma estação com Docker disponível.
