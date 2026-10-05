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

## Gmail SMTP

Para enviar e-mails reais pelo Gmail, mantenha a verificação em duas etapas ativa na conta Google e gere uma **senha de aplicativo**. Não use a senha normal da conta.

No arquivo `docker/.env`, preencha as variáveis abaixo e reinicie a API com `docker compose up -d --build api`:

```shell
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-de-aplicativo
MAIL_AUTH=true
MAIL_TLS=true
MAIL_FROM=seu-email@gmail.com
```

Com essas variáveis ausentes, o ambiente continua usando o Mailpit local para testes.

## SMTP corporativo CentrUS

No ambiente de trabalho, preencha o arquivo `docker/.env` com o servidor corporativo e um endereço de remetente autorizado:

```shell
MAIL_HOST=smtp.centrus.org.br
MAIL_PORT=25
MAIL_AUTH=false
MAIL_TLS=false
MAIL_FROM=seu-email@centrus.org.br
```

Em seguida, recrie somente a API:

```shell
docker compose up -d --build api
```

Para retomar os testes locais, remova essas variáveis do `.env`; a API volta a usar o Mailpit automaticamente.
