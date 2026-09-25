# Validação — Tarefa 11

Data: 25/09/2026

## Entregas verificadas

- Consulta do serviço ativo retorna somente projeções públicas de oficina e OS.
- Cabeçalho apresenta veículo, status, previsão estimada, pendência, última atualização, motivo e próxima ação públicos.
- Galeria mantém todas as fotos publicadas da OS em ordem cronológica e filtra por etapa sem descartar as demais.
- Visualização ampliada possui original autorizado, navegação anterior/próxima, Escape e gesto horizontal.
- Linha do tempo exclui autoria e conteúdo interno.
- Estado sem OS mostra contato da oficina; resposta 401 retorna ao fluxo de acesso.
- Não há percentual artificial, histórico encerrado nem módulo antes/depois.

## Evidências automatizadas

- Backend: `mvn -q -DskipTests compile` — aprovado.
- Backend: compilação dos 13 arquivos de teste — aprovada.
- Backend unitário: 20 testes aprovados em oito suítes de política/validação.
- Frontend: `npm run build` — aprovado; permanece aviso não bloqueante de orçamento CSS no portal e em componente preexistente.
- Frontend unitário: 35 testes aprovados no ChromeHeadless.
- E2E Playwright: 2 cenários aprovados, desktop e Pixel 7, incluindo retenção de fotos antigas após mudança de status, filtro, lightbox e largura de 320 px.
- `git diff --check` — aprovado, somente avisos de normalização LF/CRLF.

## Limitação do ambiente

`PortalAccessIntegrationTest` foi compilado, mas não pôde ser executado: o serviço local `com.docker.service` está parado e esta sessão não possui permissão para iniciá-lo. O Testcontainers encerrou antes de inicializar o PostgreSQL; não houve falha de asserção da implementação.

