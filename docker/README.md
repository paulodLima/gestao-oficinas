# Ambiente Docker

Na pasta `docker`, copie `.env.example` para `.env` caso queira alterar portas ou credenciais. Depois execute:

```shell
docker compose up --build
```

Serviços disponíveis:

- Frontend: http://localhost:4200
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: `localhost:5432`

Para encerrar os contêineres:

```shell
docker compose down
```

Os dados do PostgreSQL ficam preservados no volume `postgres-data`. Para também apagar os dados, use `docker compose down -v`.

