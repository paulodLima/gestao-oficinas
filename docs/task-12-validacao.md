# Validação da tarefa 12 — Previsão de conclusão e atrasos

Data: 19/09/2026

## Escopo validado

- Revisão da previsão com controle otimista e timestamp com fuso.
- Histórico imutável com previsão anterior/nova, motivo público, próxima ação, autor e instante.
- Registro explícito de ausência de nova previsão.
- Classificação de atraso apenas durante a execução.
- Estado separado para veículo pronto aguardando retirada.
- Projeção pública sem identidade do autor nem conteúdo interno.
- Interface responsiva para atualização e consulta do histórico.

## Evidências automatizadas

- Backend: `./mvnw test` — 47 testes, 0 falhas, 0 erros.
- Frontend unitário: Angular/Karma — 23 testes aprovados.
- E2E Playwright: 8 testes aprovados em desktop e mobile.
- Docker Compose: API, app, PostgreSQL e Mailpit em execução; Flyway aplicou a migração V7.
- Build Angular validado na imagem Docker e localmente dentro do orçamento de CSS.

## Casos de fronteira cobertos

- Data passada e timestamp sem offset são rejeitados.
- Previsão igual ao instante de referência ainda não é atraso.
- Previsão ultrapassada em execução é atraso.
- Pronto, entregue e cancelado não são atraso de execução.
- Mudança de uma data conhecida para `null` permanece no histórico.
- Consulta de outra oficina retorna 404 e a projeção pública não expõe o autor.

## Resultado

Todos os critérios de aceite da tarefa 12 foram atendidos. A projeção pública está pronta para ser
reutilizada pelo portal do cliente, que será implementado nas tarefas posteriores.
