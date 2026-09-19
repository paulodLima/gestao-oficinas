# Validação da tarefa 1.0

Execução em 18/09/2026, noite, America/Sao_Paulo (logs Docker em 19/09 UTC).

## Resultado

Smoke test da base aprovado. Especificação técnica e cenários preparados. Nenhum módulo de negócio implementado nesta tarefa.

| Verificação | Resultado observado |
| --- | --- |
| docker compose -f docker/docker-compose.yml up --build -d | Exit 0; imagens construídas com cache; três serviços iniciados |
| docker compose ps | api/app Up; postgres Up (healthy) |
| pg_isready -U postgres -d oficinas | accepting connections |
| Logs da API | Spring Boot 4.1.1 em Java 21.0.12; Hikari conectado ao PostgreSQL 17.11 |
| App → http://api:8080/v3/api-docs | HTTP 200; OpenAPI 3.1.0 |
| Host → http://localhost:4200/ | HTTP 200 |
| Host → http://localhost:8080/v3/api-docs | HTTP 200 |
| git diff --check | sem erros |

A primeira tentativa de Docker no sandbox foi negada por acesso ao pipe; a execução autorizada fora do sandbox funcionou. Os contêineres permanecem em execução. Nenhum volume ou dado foi removido.

## Limites da evidência

Build utilizou cache, não foi recompilação limpa. O Dockerfile da API ignora testes; não foi executada suite de negócio porque ainda não existem esses módulos. HTTP 200 do Angular comprova entrega do scaffold, não UX funcional. Chamada interna Node → API comprova rede, não consumo pelo navegador: API_URL ainda não é usada pelo Express. Proxy da mesma origem e rotas privadas sem prerender são entregas previstas na tarefa 2.

Avisos observados: open-in-view habilitado e Swagger público. A tarefa 2 deverá desabilitar open-in-view e restringir documentação no perfil de produção, junto à configuração de autenticação. Fotos, SMTP, sessões e Flyway são decisões documentadas a implementar nas tarefas seguintes, não integrações já testadas.

## Revisão independente

Agente task_reviewer revisou techspec contra prompt e tarefa 1. Encontrou três lacunas: revogar verificação/acesso ao trocar e-mail; modelar execução autorizada dos adicionais; explicitar problema, justificativa e fotos dos adicionais. As três foram incorporadas na especificação.

## Arquivos entregues

- [techspec.md](techspec.md): arquitetura, modelo, contratos, autorização, paginação, erros, integrações e limites.
- [cenarios-validacao.md](cenarios-validacao.md): massa sintética de duas oficinas e casos a implementar.
- [tasks.md](tasks.md): progresso da tarefa 1.

A tarefa 1 é documentação e validação da base; as próximas tarefas devem implementar e testar os contratos, não tratar os cenários como testes já executados.

