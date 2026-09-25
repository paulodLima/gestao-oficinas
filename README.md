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
| E-mails de desenvolvimento (Mailpit) | http://localhost:8025 |
| PostgreSQL | `localhost:5432` |

Credenciais padrão do banco:

```text
Banco: oficinas
Usuário: postgres
Senha: postgres
```

Esses valores podem ser alterados no arquivo `docker/.env`.

### Cadastro e acesso do proprietário

Abra o frontend e clique em **Criar uma conta**. Informe seu nome, nome da oficina, e-mail e senha (mínimo de 12 caracteres, máximo de 72 bytes). Depois entre com o e-mail e a senha cadastrados. Não há usuário ou senha inicial da aplicação.

Em **Esqueci minha senha**, solicite a recuperação e abra o e-mail no Mailpit. O link dura 30 minutos, funciona uma única vez e a troca de senha encerra as sessões anteriores. O Mailpit apenas captura e-mails locais; não envia mensagens reais.

Migrações do banco são aplicadas pelo Flyway. Swagger está desativado. A área autenticada inclui a identidade da oficina, cadastro de clientes e veículos e abertura/consulta de ordens de serviço.

## Comandos úteis do Docker

### Identidade da oficina

Depois do login, acesse **Configurar dados e logo da oficina**. Edite nome, contatos
comerciais, endereço, horário de atendimento e fuso. A logo aceita PNG/JPEG de até 2 MiB
e 4 megapixels e é salva separadamente ao selecionar o arquivo; também pode ser removida.
Sem imagem, o perfil usa a inicial do nome.

Ative **Publicar perfil da oficina** e salve para disponibilizar os dados comerciais no
link **Abrir perfil publicado**. A publicação começa desativada. Desmarcar e salvar
torna o perfil indisponível. O e-mail de login e dados de clientes não são publicados.
Se outra aba salvar primeiro, recarregue os dados antes de aplicar suas alterações.
Este perfil apresenta a oficina; acompanhamento de serviços será entregue nas tarefas futuras.

### Clientes e veículos

Depois do login, acesse **Cadastrar clientes e veículos**. É possível cadastrar, buscar e
editar clientes, vincular vários veículos ao mesmo cliente, pesquisar CPF/placa com ou sem
máscara e registrar a troca de responsável sem apagar o vínculo anterior. Placas antigas e
Mercosul são normalizadas pela API.

O e-mail do cliente pode ser confirmado por um código de seis dígitos capturado pelo Mailpit
no desenvolvimento. O código vale por 10 minutos, permite no máximo cinco tentativas e tem
limites de reenvio. Trocar o e-mail remove a verificação anterior. A transferência de
responsável fica bloqueada enquanto houver ordem de serviço ativa.

### Ordens de serviço

Depois do login, acesse **Abrir e consultar ordens de serviço**. Cada OS recebe numeração
sequencial própria da oficina e preserva o cliente responsável no momento da abertura,
veículo, relato inicial, entrada, quilometragem e previsão opcional. A busca aceita número
da OS, nome do cliente ou placa com ou sem máscara. Somente uma OS ativa pode existir por
veículo; tentativas concorrentes retornam conflito sem criar duplicidade.

Na mesma tela, a oficina pode avançar ou pular etapas não terminais, usar Funilaria e Pintura
quando necessário e retornar no fluxo com motivo obrigatório. Cada mudança registra autor,
data e hora na linha do tempo. Atualizações também podem ser publicadas sem mudar o status,
mantendo texto público e observação interna em campos e projeções separados.

A oficina também pode revisar a previsão de conclusão informando um motivo público e a próxima
ação, ou registrar que ainda não existe uma nova data. O histórico preserva previsão anterior e
nova, autor e instante. A interface destaca previsões ultrapassadas somente durante a execução e
separa os veículos prontos que apenas aguardam retirada.

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

### 1. Inicie o PostgreSQL e o e-mail local

```powershell
docker compose -f docker/docker-compose.yml up -d postgres mailpit
```

### 2. Inicie o backend

Abra outro terminal PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/oficinas"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
$env:MAIL_FROM = "oficinas@local.test"
$env:CODE_SECRET = "troque-por-um-segredo-com-24-caracteres"
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

Antes de executar API/app diretamente, pare as respectivas instâncias Docker para liberar as portas: `docker compose -f docker/docker-compose.yml stop api app`.

## Testes

Na pasta `oficinas-api`, execute `mvn test` (Docker deve estar disponível para o PostgreSQL temporário do Testcontainers).

Sem Docker, use `mvn -Dtest.database=embedded test`: executa a mesma suíte contra
PostgreSQL 17 nativo, temporário e restrito ao localhost. Os binários são dependências
de teste Maven; não substitui o banco da aplicação nem instala um serviço no sistema.
Em Linux, execute como usuário não-root. Nenhum teste integrado envia e-mails reais.

Na pasta `oficinas-app`:

```powershell
npm ci
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
npx playwright install chromium
npm run test:e2e
```

Os testes Angular usam Chrome instalado; se necessário, configure `CHROME_BIN` com o caminho de um Chromium headless. Os E2E exigem o Compose rodando nas portas padrão (4200/8025), criam contas sintéticas `@example.test` e deixam mensagens no Mailpit. Capturas ficam em `oficinas-app/test-results`, ignoradas pelo Git.

## Painel operacional e previsões

Após entrar, abra **Painel operacional e Kanban** na tela inicial ou acesse `/painel`.
O painel mostra ordens ativas, atrasadas, aguardando aprovação/peças e prontas para retirada.
Os indicadores representam toda a oficina; os filtros afetam os cartões, com 20 ordens por página.
Lista e Kanban usam a mesma seleção. No celular, a lista é a visualização inicial.

Busque por placa, cliente ou número da OS e filtre por etapa, situação, tempo sem atualização
ou tempo na etapa. Clique no número para abrir os detalhes e alterar a etapa. Não há arrastar
cartões: justificativas e histórico são registrados no detalhe. Use **Atualizar** para consultar
novamente; o painel informa o instante da consulta e não faz atualização em tempo real.

No detalhe, **Previsão de conclusão** permite informar ou remover uma estimativa, sempre com
motivo público e próxima ação. O histórico é preservado. Datas usam o fuso da oficina.
Prontas para retirada e ordens sem previsão não contam como atraso de execução.
O portal do cliente e os avisos de mudança de prazo já estão integrados.

## Central de avisos e e-mails

Acesse **Avisos** no menu ou `/notificacoes`. Abertura de OS, envio de adicionais,
decisão do cliente e mudanças de previsão geram avisos privados da oficina.
A central permite filtrar não lidos, marcar leitura e acompanhar falhas de envio.
Fotos e pequenas atualizações da linha do tempo não geram e-mails individuais.

E-mails comerciais são enfileirados na transação da operação somente para o cliente
responsável ativo com e-mail verificado. Um worker entrega após o commit e revalida
o contato antes de cada tentativa. Alteração/desativação do contato cancela o envio.
Falha SMTP não desfaz a OS: há tentativa inicial e retentativas após 1, 5, 15 e
60 minutos. Após a quinta falha, **Reenviar e-mail** permite iniciar novo ciclo,
preservando o aviso e o histórico, com intervalo mínimo de um minuto.

Configure SMTP e `MAIL_FROM` de verdade: falta de provedor nunca é tratada como
sucesso. Use Mailpit no desenvolvimento. `APP_NOTIFICATIONS_ENABLED=false` pausa o
worker sem apagar a fila; `APP_NOTIFICATIONS_POLL_MS` controla a consulta (10000 ms
por padrão, até 20 mensagens por ciclo). Ajuste a operação às regras do provedor,
sem disparos em massa ou listas externas. SMTP confirma aceitação, não leitura.
Deduplicação impede novos registros/replays; uma queda após aceitação SMTP e antes
do commit ainda pode duplicar entrega (sem garantia exactly-once).

Pronto para retirada e encerramento geram avisos transacionais desde a tarefa 17.
O gatilho de avaliação permanece na tarefa 18. Códigos de acesso/verificação e recuperação
preservam o transporte síncrono e a expiração existentes; segredos não são gravados
na fila comercial. E2E da central/portal com API simulada:
`npx playwright test e2e/notifications.spec.ts e2e/portal.spec.ts` (frontend ativo).

## Antes de publicar em produção

O Compose fornecido é de desenvolvimento. Use HTTPS, perfil Spring `prod`, banco privado com credenciais próprias e SMTP externo configurado (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `PUBLIC_URL`). Defina também um `CODE_SECRET` aleatório e exclusivo, com ao menos 24 caracteres, para o HMAC dos códigos de cliente. O perfil exige cookie Secure e URL pública HTTPS. SMTP externo e domínio remetente ainda não foram validados.

O proxy Express sobrescreve o IP encaminhado; a API só o aceita do host definido em `APP_AUTH_TRUSTED_PROXY_HOST` (`app` no Compose). Se houver outro balanceador, configure a cadeia confiável antes de publicar; não habilite confiança irrestrita em cabeçalhos. Não exponha banco nem Mailpit publicamente.

Há alertas de segurança nas dependências existentes do Angular 19. Atualização de versão principal e validação correspondente são pendências antes de produção, fora da tarefa 2. Consulte [a validação](docs/task-2-validacao.md).

## Comunicação entre os serviços

Dentro da rede do Docker, os serviços utilizam os próprios nomes como endereço:

```text
Frontend -> http://api:8080
API      -> jdbc:postgresql://postgres:5432/oficinas
```

O backend só é iniciado depois que o healthcheck do PostgreSQL confirma que o banco está pronto para receber conexões.

## Solução de problemas

### Compartilhar acompanhamento pelo WhatsApp

No detalhe da OS, use **Preparar link seguro**, confira a mensagem e escolha
**Abrir WhatsApp**. Selecione o destinatário e confirme o envio no WhatsApp.
O sistema não envia automaticamente nem confirma entrega. **Copiar link** e o
campo selecionável permitem compartilhar manualmente por outro canal.

O link permite leitura apenas daquela OS por até sete dias ou até seu encerramento.
Compartilhe somente com o responsável: quem possui o link pode consultar o serviço.
**Gerar outro link** invalida os anteriores; **Revogar links desta OS** encerra o
acesso mesmo após recarregar a página. Aprovar adicionais continua exigindo código.
No ambiente local, `localhost` funciona somente no próprio computador; para o
cliente acessar, use o endereço HTTPS público do sistema em uma implantação configurada.

Documentação e testes: [tarefa 16](tasks/prd-compartilhamento-whatsapp/16_task.md).

### Encerrar atendimento e registrar retorno

No detalhe da OS, **Revisar encerramento** consulta as pendências atuais. Selecione
entrega ou cancelamento (este exige motivo), resolva os adicionais ou marque seu
cancelamento explícito e confirme o encerramento definitivo. Pronto para retirada
continua sendo uma OS ativa; não representa entrega.

Depois de encerrar, alterações, fotos, vistorias e decisões ficam bloqueadas.
O histórico interno e as decisões anteriores permanecem disponíveis. Links da OS
e códigos de aprovação pendentes são revogados, inclusive para sessões abertas.
A sessão por identidade do cliente pode permanecer válida, mas não abre a OS encerrada.

**Abrir nova OS para este veículo** confere o vínculo atual e prepara um atendimento
novo, exigindo relato e quilometragem novos. O portal por identidade passa a mostrar
a nova OS quando consultado novamente, se o vínculo continuar válido. Links antigos
nunca redirecionam para a nova visita. O resumo de entrega usa o convite separado descrito abaixo.

Migração V15 e endpoints `GET/POST /api/ordens-servico/{id}/encerramento`.
Evidências: [validação da tarefa 17](tasks/prd-encerramento-retorno/validacao.md).

### Resumo de entrega e avaliação

Ao confirmar uma **entrega**, o sistema registra um único convite, com validade de
sete dias a partir do encerramento. Havendo e-mail verificado e envio transacional
configurado, ele entra na fila existente. Cancelamento não gera convite. Falha de
SMTP não desfaz a entrega; o histórico e o reenvio ficam na central de avisos.

No detalhe de uma OS entregue, **Obter link de avaliação** permite copiar o convite
para o responsável, sem enviar mensagens automaticamente. **Revogar convite** exige
confirmação e bloqueia inclusive sessões abertas. Não há reemissão após revogação
nem prorrogação dos sete dias. Não há disparo retroativo em massa para OS antigas.

Em `/avaliar`, o cliente consulta apenas modelo do veículo, entrega e atualizações
já públicas; não recebe custos internos, CPF, placa, dados de outros clientes ou
fotos. O convite não reabre a OS e não libera outra visita. Nota inteira de 1 a 5,
comentário opcional de até 2000 caracteres, uma avaliação por OS. Reenvio idêntico
é seguro; outra resposta não substitui a primeira.

A avaliação é privada por padrão. O consentimento para eventual publicação é
opcional e separado; **não existe publicação automática nem catálogo público de
depoimentos**. Em **Avaliações**, o proprietário consulta respostas paginadas e
configura opcionalmente o link de avaliação do Google. Aceitamos
`https://g.page/r/IDENTIFICADOR/review` ou
`https://search.google.com/local/writereview?placeid=IDENTIFICADOR`. Ele aparece
para todas as notas, antes e depois da resposta, sem transportar comentário ou nota.

Para clientes externos, configure `PUBLIC_URL` com o endereço HTTPS público e
o SMTP; `localhost` só funciona na própria máquina. Mantenha `CODE_SECRET` protegido
e estável: a troca do segredo impede reconstruir convites antigos para reenvio.
Migração V16. [Detalhamento e validação](tasks/prd-avaliacao-entrega/validacao.md).

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
