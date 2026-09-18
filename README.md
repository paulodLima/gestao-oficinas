# Gestão de Oficinas

Projeto para gestão de oficinas, composto por uma API Spring Boot, uma aplicação web Angular e um banco de dados PostgreSQL.

## Tecnologias

- Java 21 e Spring Boot
- Spring Data JPA
- PostgreSQL 17
- Angular 19 com SSR
- Docker e Docker Compose

## Estrutura do projeto

```text
gestao-oficinas/
├── docker/
│   ├── docker-compose.yml   # Orquestra banco, API e frontend
│   ├── .env.example         # Exemplo das variáveis do ambiente
│   └── README.md            # Instruções específicas do Docker
├── oficinas-api/
│   ├── src/                 # Código-fonte e testes do backend
│   ├── Dockerfile           # Imagem da API
│   └── pom.xml              # Dependências Maven
├── oficinas-app/
│   ├── public/              # Arquivos públicos do frontend
│   ├── src/                 # Código-fonte Angular
│   ├── Dockerfile           # Imagem do frontend
│   └── package.json         # Dependências e scripts Node.js
├── config/                  # Configurações do projeto
├── docs/                    # Documentação funcional e técnica
└── task/                    # Planejamento e tarefas
```

## Como subir o projeto com Docker

Esta é a forma recomendada, pois inicia o PostgreSQL, o backend e o frontend juntos.

### Pré-requisitos

- Docker Desktop instalado e em execução
- Docker Compose disponível

### 1. Configure o ambiente

No PowerShell, a partir da raiz do projeto:

```powershell
Copy-Item docker/.env.example docker/.env
```

Os valores padrão funcionam para desenvolvimento. Se necessário, altere o arquivo `docker/.env` antes de iniciar.

### 2. Inicie os serviços

```powershell
docker compose -f docker/docker-compose.yml up --build
```

Na primeira execução, o Docker baixará as imagens e instalará as dependências. As execuções seguintes serão mais rápidas devido ao cache.

Para executar em segundo plano:

```powershell
docker compose -f docker/docker-compose.yml up --build -d
```

### 3. Acesse os serviços

| Serviço | Endereço |
| --- | --- |
| Frontend | http://localhost:4200 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | `localhost:5432` |

Credenciais padrão do banco:

```text
Banco: oficinas
Usuário: postgres
Senha: postgres
```

Esses valores podem ser alterados no arquivo `docker/.env`.

## Comandos úteis do Docker

Ver os serviços em execução:

```powershell
docker compose -f docker/docker-compose.yml ps
```

Visualizar os logs:

```powershell
docker compose -f docker/docker-compose.yml logs -f
```

Visualizar apenas os logs da API:

```powershell
docker compose -f docker/docker-compose.yml logs -f api
```

Encerrar os serviços sem apagar os dados:

```powershell
docker compose -f docker/docker-compose.yml down
```

Encerrar os serviços e apagar o volume do PostgreSQL:

```powershell
docker compose -f docker/docker-compose.yml down -v
```

> O último comando apaga permanentemente os dados locais do banco.

## Desenvolvimento local

Também é possível executar o banco pelo Docker e iniciar backend e frontend diretamente na máquina, facilitando o recarregamento durante o desenvolvimento.

### Pré-requisitos adicionais

- Java 21
- Maven 3.9 ou superior
- Node.js 20 ou superior
- npm

### 1. Inicie somente o PostgreSQL

```powershell
docker compose -f docker/docker-compose.yml up -d postgres
```

### 2. Inicie o backend

Abra outro terminal PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/oficinas"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = "update"
cd oficinas-api
./mvnw.cmd spring-boot:run
```

### 3. Inicie o frontend

Abra outro terminal PowerShell:

```powershell
cd oficinas-app
npm install
npm start
```

O frontend ficará disponível em `http://localhost:4200` e a API em `http://localhost:8080`.

## Comunicação entre os serviços

Dentro da rede do Docker, os serviços utilizam os próprios nomes como endereço:

```text
Frontend -> http://api:8080
API      -> jdbc:postgresql://postgres:5432/oficinas
```

O backend só é iniciado depois que o healthcheck do PostgreSQL confirma que o banco está pronto para receber conexões.

## Solução de problemas

### Docker não está disponível

Confirme que o Docker Desktop está aberto e que o mecanismo de contêineres Linux está em execução.

### Uma porta já está sendo utilizada

Altere a porta correspondente no arquivo `docker/.env`. Por exemplo:

```dotenv
POSTGRES_PORT=5433
API_PORT=8081
APP_PORT=4201
```

### Recriar as imagens após alterações

```powershell
docker compose -f docker/docker-compose.yml up --build
```

### Reiniciar o banco do zero

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up --build
```

