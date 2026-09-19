# Validação da tarefa 06 — Fluxo de status e linha do tempo

Data: 19/09/2026

## Escopo validado

- Saltos entre etapas ativas e etapas opcionais Funilaria/Pintura.
- Retornos e entrada em espera a partir da execução com motivo obrigatório.
- Eventos persistidos com autor, data, hora, status anterior/novo e motivo.
- Atualizações públicas e internas sem alteração de status.
- Projeção pública sem exposição do texto interno.
- Concorrência otimista por versão da ordem.
- Interface responsiva para alteração de etapa, comunicação e histórico.

## Evidências automatizadas

- Backend: `./mvnw test` — 44 testes, 0 falhas, 0 erros.
- Frontend unitário: `npm test -- --watch=false --browsers=ChromeHeadless` — 20 testes aprovados.
- E2E Playwright: 8 testes aprovados, incluindo a OS em desktop e mobile.
- Docker Compose: API, app, PostgreSQL e Mailpit em execução; Flyway aplicou a migração V6.
- Build Angular validado durante a reconstrução da imagem Docker, sem estouro de orçamento CSS.

## Resultado

Todos os critérios de aceite da tarefa 06 foram atendidos. Entrega e cancelamento continuam
fora deste escopo e serão implementados pela tarefa 17, conforme planejamento.
