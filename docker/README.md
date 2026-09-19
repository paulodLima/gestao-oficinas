# Ambiente Docker

Na pasta `docker`, copie `.env.example` para `.env` caso queira alterar portas ou credenciais. Depois execute:

```shell
docker compose up --build
```

Serviços disponíveis:

- Frontend: http://localhost:4200
- API: http://localhost:8080
- E-mails locais (Mailpit): http://localhost:8025
- PostgreSQL: `localhost:5432`

Para encerrar os contêineres:

```shell
docker compose down
```

Os dados do PostgreSQL ficam preservados no volume `postgres-data`. Para também apagar os dados, use `docker compose down -v`.

Cadastre sua conta pelo frontend; não há usuário padrão. A recuperação de senha entrega um link no Mailpit (não envia e-mail real). Flyway cria/atualiza as tabelas; não use Hibernate `update`. Swagger está desativado.

Se mudar APP_PORT, ajuste PUBLIC_URL em `.env` para que o link de recuperação use o endereço correto. Para testar pelo celular na mesma rede, PUBLIC_URL deve usar o IP local da máquina e a porta do frontend. Este Compose não é uma configuração de produção: consulte o README da raiz para HTTPS, SMTP externo e proxy confiável.
