# Validação isolada do MVP

Este Compose é local e independente do ambiente principal. Não usa `docker/.env`, não publica PostgreSQL/API e não substitui os volumes ou portas 4200/8080/5432 existentes. Credenciais e segredo são **exclusivamente de teste**, inadequados para produção.

Angular foi migrado, com autorização, para core 20.3.32 e CLI/SSR 20.3.37. A auditoria npm de produção em 25/09/2026 não identificou alertas; restam cinco moderados somente na árvore de desenvolvimento. Este ambiente é exclusivamente local e não é configuração de produção. Consulte `tasks/prd-validacao-mvp/qa-report.md`.

```powershell
docker compose -f docker/docker-compose.qa.yml up -d --build --wait
```

- Sistema: http://localhost:14220
- E-mails capturados: http://localhost:18025
- Use apenas dados sintéticos e destinatários `@qa.test` / `@example.test`.
- API e banco usam rede interna. App/Mailpit também usam rede de preview para publicação em loopback. Mailpit **não possui relay**; nenhum e-mail sai para um provedor real.
- API roda como usuário `spring`. O Dockerfile prepara `/data/photos` para volumes novos. Volumes antigos com propriedade incorreta exigem diagnóstico e ajuste **apenas no volume identificado**, nunca `chmod 777` ou remoção indiscriminada.

## Testes

Java 21/Maven, Node 22.12+ (ou 20.19+/24) e navegador Chrome para Karma. Na pasta `oficinas-api`, rode `./mvnw.cmd test` (o Testcontainers usa PostgreSQL real e isolado). Na pasta `oficinas-app`:

```powershell
npm ci
npx playwright install chromium webkit
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
$env:PLAYWRIGHT_BASE_URL='http://localhost:14220'
$env:MAILPIT_URL='http://localhost:18025'
npx playwright test --config playwright.qa.config.ts
npx playwright test
```

Não execute campanhas simultâneas. As proteções reais de cadastro, códigos e login continuam ativas; muitas repetições podem responder 429. Aguarde a janela de 15 minutos antes de repetir. Durante desenvolvimento, apenas a tabela de contadores temporários `auth_limite` do **banco `oficinas_qa` no Compose `gestao-oficinas-qa`** foi reiniciada entre campanhas; não replique essa operação em produção.

O teste integrado usa API para preparar proprietário, cliente verificado e veículo; executa pela interface abertura da OS, vistoria, fotos, reenvio, adicional, aprovação com código SMTP, revisão de prazo, entrega, avaliação privada e retorno em nova OS. Os testes anteriores cobrem os cadastros pela interface. Relatórios e capturas ficam em `oficinas-app/test-results-qa` e `oficinas-app/playwright-report-qa` (ignorados no Git).

## Limites da emulação

WebKit/iPhone é emulação, **não Safari executando em iPhone físico**. Android usa perfil Pixel 7 em Chromium. O seletor nativo não é uma câmera física: câmera negada/cancelada é representada por seleção vazia, seguida de galeria. O backend testa oito transformações de orientação em matriz e um JPEG real com orientação 6, GPS e identificação sintéticos. Verifica retirada dos metadados também no download HTTP de arquivo legado. HEIC deve ser convertido em JPEG, conforme indicação da tela.

A simulação de rede usa latência e falha com uma imagem pequena; não mede transferência de uma foto grande sob banda limitada. Axe e medidas dos controles selecionados não certificam leitor de tela, todos os links, foco do lightbox ou todas as telas.

O Playwright/WebKit não preserva conteúdo de arquivos em `route.fetch` multipart (limitação documentada no [repositório oficial](https://github.com/microsoft/playwright/issues/14624)). Portanto, Chromium verifica perda da resposta **após** persistência; WebKit verifica falha **antes** do envio e repetição pelo transporte real, sem interceptação. Nenhuma resposta de sucesso é simulada na jornada integrada.

## Backup e restauração

Na raiz, sem testes/escritas em andamento:

```powershell
./docker/verify-qa-backup.ps1
```

O script Windows PowerShell confirma o projeto/volume QA, interrompe **somente sua API**, gera `pg_dump -Fc` e arquivo TAR das fotos, restaura em volumes **novos e únicos**, compara contagem+hash de conteúdo de todas as tabelas e SHA-256 de todos os arquivos. A API de origem é reiniciada no `finally`. O PostgreSQL restaurado termina parado, sem rede/portas; backups e volumes são preservados para inspeção em `artifacts/qa-backup-*` e `oficinas-qa-restore-*`. Não há exclusão automática.

Esse ensaio comprova cópia/recuperação dos dados sintéticos e arquivos, não alta disponibilidade nem RPO/RTO de produção. Antes de produção, definir destino externo criptografado, retenção, acesso e agendamento, validar HTTPS/SMTP real e executar smoke manual em dispositivos físicos. A publicação no Git não faz deploy de produção.

## Uso de fotos

Fotos permanecem privadas por padrão. Marque **Publicar novas fotos no portal do cliente** antes de selecionar se desejar disponibilizá-las ao responsável autorizado. Após falha, use **Tentar novamente** sem selecionar a mesma foto outra vez: a chave original evita duplicação, inclusive quando o servidor já concluiu o primeiro envio. A fila é mantida enquanto a página está aberta, não após recarregá-la.

Para parar sem apagar dados: `docker compose -f docker/docker-compose.qa.yml stop`. Não use `down -v` para uma simples parada.
