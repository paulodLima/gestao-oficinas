# Tech Spec · Tarefa 20

Dependência: tarefas 2–19 implementadas, tarefa 19 publicada na master `7d1262b`. Stack inicial Angular 19, Spring Boot/Java 21 e PostgreSQL 17. Em 25/09/2026, o responsável autorizou a ampliação para Angular 20 corrigido e repetição da validação antes do merge. Versões finais: core 20.3.32, CLI/SSR 20.3.37, TypeScript 5.9.3; Node suportado 20.19+/22.12+/24.

## Estratégia

- Compose exclusivo `gestao-oficinas-qa`, imagens/volumes próprios, frontend loopback 14220 e Mailpit loopback 18025. Sem substituir contêineres, portas ou dados do Compose existente.
- Playwright do repositório: contextos separados proprietário/cliente, chamadas reais pela origem do app, código extraído apenas da caixa SMTP local sintética. Testes de falha interceptam somente a requisição específica e depois restauram transporte real.
- Projetos Chromium desktop, Pixel 7, WebKit/iPhone e Chromium 320px. Evidências sem tokens/CPFs/credenciais reais. Não salvar trace com segredos.
- Skills executar-task, java-springboot, executar-qa, frontend-design e ui-ux-pro-max orientam testes/correções pontuais. Manter visual industrial/utilitário existente, não redesenhar o produto.
- Playwright MCP e Context7 indisponíveis: usar runner Playwright do projeto, inspeção visual via ferramentas locais e documentação oficial. Não usar comandos Bun inexistentes.
- Corrigir problemas encontrados com regressões focadas; documentar bugs e rechecagem. Revisão task-reviewer independente antes do merge.
- Backup sintético: interromper escritas somente no ambiente QA, copiar dump lógico e volume de fotos; restaurar em banco/volume novos e conferir contagens/hash/conteúdo. Nunca sobrescrever banco/volumes existentes do usuário.

## Referências

- https://playwright.dev/docs/emulation
- https://playwright.dev/docs/browsers
- https://mailpit.axllent.org/docs/api-v1/

WebKit do Playwright é um build do motor, não o aplicativo Safari do iOS. Os resultados devem usar esse nome e explicitar limitações de permissões/câmera nativas.
