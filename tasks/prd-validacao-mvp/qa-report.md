# Relatório de QA · Validação móvel e entrega do MVP

## Resumo

- Data: 25/09/2026. Status: **validação aprovada no ambiente equivalente**, com observações operacionais; publicação registrada em `20_task.md`.
- Requisitos funcionais: 6/6 atendidos nos limites documentados. Treze problemas tratados, incluindo três achados da revisão e a atualização Angular autorizada. Nenhum bloqueador de implementação identificado na revisão incremental; cinco alertas moderados de ferramentas de desenvolvimento documentados abaixo.
- Branch: `codex/tarefa-20-validacao-mvp`, base `master` `7d1262b`.
- Docker Engine 29.1.3; PostgreSQL 17.11; Java 21; Angular core 20.3.32 e CLI/SSR 20.3.37; TypeScript 5.9.3; Playwright 1.63.0; axe 4.13.0. Ambiente principal existente preservado.

## Requisitos verificados

| ID | Resultado | Evidência |
|---|---|---|
| RF-01 | PASSOU | `e2e-qa/mvp.spec.ts`: OS, vistoria, upload público explícito, adicional pela interface, decisão com código SMTP, previsão vencida/nova previsão, entrega, avaliação privada e nova OS sem reabrir a anterior. |
| RF-02 | PASSOU | Mesma jornada em Chromium desktop, Pixel 7 emulado, WebKit/iPhone 13 emulado e Chromium 320px; nenhum overflow horizontal da página. |
| RF-03 | PASSOU | Seleção vazia seguida de galeria; oito transformações EXIF e JPEG real com orientação 6/GPS/câmera sintéticos; sanitização de foto nova/legada também no HTTP; WebP e limite 40MP. Atraso, resposta perdida/reenvio idempotente, fila compartilhada com três vagas, nomes iguais, falha CSRF e upload atrasado de outra OS. |
| RF-04 | PASSOU | Axe WCAG A/AA nas telas exercitadas, medidas de 44px em botões/inputs/selects/labels da jornada, foco e Tab entre seletores, estados vazio/carregando/erro/repetição. |
| RF-05 | PASSOU | 144 testes Java, 111 Angular 20, 44 E2E de regressão (10 integrações reais e 34 com APIs simuladas), 4 jornadas integradas + 4 matrizes públicas nas imagens finais. Build SSR, checagem TypeScript dos E2Es e Docker concluídos. |
| RF-06 | PASSOU | `docker/verify-qa-backup.ps1`: 36 tabelas com conteúdo idêntico, 22 arquivos com SHA-256 idêntico. Dump/TAR e volumes novos preservados; instruções em `docker/QA.md`. |

## Matriz e limites

| Configuração | Jornada integrada | Acessibilidade pública |
|---|---|---|
| Chromium desktop 1280×720 | PASSOU | PASSOU |
| Pixel 7 / Chromium 393×727 | PASSOU | PASSOU |
| iPhone 13 / WebKit 390×664 | PASSOU | PASSOU |
| Chromium 320×760 | PASSOU | PASSOU |

Cadastro inicial de proprietário/cliente/veículo e verificação do contato usam API para preparar a jornada; o restante percorre a interface. Os testes de regressão anteriores exercitam os cadastros pela UI e recuperação de senha SMTP. Sucesso SMTP significa recebido no Mailpit real local, não entrega em provedor externo.

WebKit não é Safari instalado em iPhone. Não foram usados aparelhos físicos, câmera real ou prompts nativos de permissão. A alternativa após câmera negada/cancelada é testada como seleção sem arquivo, seguida de galeria. HEIC não é aceito; interface orienta JPEG. Chromium verifica perda da resposta após persistência; WebKit verifica falha antes do envio, pois sua interceptação multipart omite arquivos (ver referência no guia QA). Essas distinções não são apresentadas como teste físico.

## Acessibilidade e análise visual

Zero violações automatizadas nos critérios A/AA selecionados nas telas verificadas, após correção de contraste/labels. Isso não é certificação completa WCAG nem substitui teste com tecnologias assistivas reais. Inspeção visual das capturas confirmou hierarquia, campos e avaliação em 320px sem corte. A identidade visual existente foi preservada; as skills de frontend/UI-UX orientaram apenas correções de legibilidade, foco e toque.

As medidas não abrangem todos os links nem o foco do lightbox; o teste explícito de teclado verifica Tab entre câmera/galeria. A simulação de rede usa uma imagem pequena e não mede transferência de foto grande sob banda limitada. O estado de OS encerrada foi revalidado com primeiro GET de fotos abortado, recarga habilitada e sucesso real; upload/remoção permanecem desabilitados.

Capturas por execução em `oficinas-app/test-results-qa/` (`entrada-vazia.png`, `oficina-vistoria.png`, `portal-aprovado.png`, `historico-encerrado.png`, `avaliacao.png`, `nova-visita.png`), e relatório HTML em `oficinas-app/playwright-report-qa/`. Somente dados sintéticos. Traces desativados para não guardar códigos/tokens; logs resumidos não incluem segredos. Quatro capturas selecionadas foram atualizadas após Angular 20 em `evidence/`.

## Backup e smoke Docker

Ensaio `qa-backup-20260925-160736-2c8bfd`: API QA parada durante cópia e comparação, banco lógico restaurado em PostgreSQL novo sem rede, fotos em volume novo. Todas as tabelas e todos os arquivos comparados; a API QA reiniciada no `finally`. Banco/volumes originais não foram modificados. Destinos restaurados foram parados e preservados, sem exclusão.

Smoke: frontend QA e `/api/auth/csrf` acessíveis, 17 migrações aplicadas, escrita/leitura autenticada de fotos e SMTP funcionando nas imagens produzidas. Sistema principal permanece em 4200/8080; QA usa 14220/18025 apenas em loopback.

## Bugs e observações

Detalhes dos treze problemas em `bugs.md` e `bugfix-report.md`. Os dois avisos de orçamento CSS já existentes permanecem: portal 7,40 kB e OS 7,00 kB para aviso de 6 kB; build aprovado. Não houve deploy de produção, WhatsApp real ou publicação automática de avaliação. Antes do uso em produção, validar HTTPS/proxy/SMTP externo, backup externo com retenção e smoke em aparelhos físicos.

## Segurança e atualização autorizada

Antes da migração, `npm audit --omit=dev` apontou 10 pacotes de produção afetados (5 altos, 5 moderados), concentrados no Angular 19. O audit completo antigo contabilizava 32 alertas, incluindo ferramentas de desenvolvimento; isso não equivale a 32 exploits demonstrados. São registros históricos anteriores à correção.

Foram confirmados avisos oficiais de [SSRF/vazamento de credenciais no SSR](https://github.com/advisories/GHSA-f6mr-pjwc-34m4) e [bypass de sanitização](https://github.com/advisories/GHSA-hh8m-fm6v-7cvg). A exploração depende das condições descritas nos avisos; não foi demonstrado um exploit neste aplicativo. Não se deve confundir ausência de exploração demonstrada com correção da dependência.

O responsável autorizou explicitamente Angular 19→20 e reteste antes do merge. `ng update @angular/core@20 @angular/cli@20 --allow-dirty` aplicou as migrações oficiais de SSR, DOCUMENT e padrões de geração; não foi executado `npm audit fix --force`. Core 20.3.32, CLI/SSR 20.3.37 e TypeScript 5.9.3 ficaram registrados no lockfile, com Node compatível declarado no manifesto. Build, Docker e todas as suítes foram repetidos.

Resultado atual: **npm audit de produção com zero alertas**. O audit completo tem **cinco entradas moderadas exclusivamente na cadeia de desenvolvimento** build-angular/build-webpack → webpack-dev-server → sockjs → uuid. O [aviso uuid](https://github.com/advisories/GHSA-w5hq-g745-h8pq) refere-se aos métodos v3/v5/v6 com buffer externo; o caminho SockJS inspecionado usa `v4()` sem buffer. Não foi demonstrada exploração desse caminho, mas a dependência ainda consta como afetada: não se declara audit completo zerado. Não expor `ng serve` publicamente. A imagem final copia apenas `dist`, sem ferramentas de desenvolvimento. Uma migração adicional major ou override incompatível não foi forçada para esconder esses alertas.

A revisão independente aprovou com observações e confirmou a resolução de R20-01/R20-02/R20-03. Resultados finais posteriores ao Angular 20: `task20-api-final-security.log` (144), `task20-angular20-tests.log` (111), `task20-qa-angular20-final.log` (8), `task20-regression-angular20.log` (44), `task20-angular20-build.log`, `task20-docker-final-security.log` e auditorias `task20-npm-*-audit-after.log`. Logs locais são ignorados no Git; configuração, testes, relatório e capturas selecionadas são versionados.

Fontes primárias: [emulação Playwright](https://playwright.dev/docs/emulation), [WebKit e navegadores](https://playwright.dev/docs/browsers), [acessibilidade automatizada e seus limites](https://playwright.dev/docs/accessibility-testing), [metadata-extractor](https://github.com/drewnoakes/metadata-extractor), [redes Docker](https://docs.docker.com/engine/network/).
