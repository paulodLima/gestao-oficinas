# Validação · Tarefa 19

Ambiente local: Windows, Java 21, Docker Engine 29.1.3, Testcontainers com PostgreSQL 17.11. Os contêineres existentes do usuário não foram substituídos. Dados sintéticos e transporte SMTP mockado nesta etapa; SMTP real local é escopo da tarefa 20.

## Matriz de evidências

| Requisito | Evidência automatizada |
|---|---|
| Oficinas/clientes isolados; arquivo privado | `PortalAccessIntegrationTest.privateDownloadsEnforceOfficeCustomerPublicationAndRevocation`: bytes reais do arquivo, proprietário autorizado, visitante anônimo, outra oficina, outro cliente na mesma oficina e foto não publicada |
| Código expirado, reutilizado e bloqueio | `keepsCodeRequestNonEnumerableAndEnforcesExpiryAttemptsAndSingleUse`; teste transacional mantém tentativas inválidas e reverte falha inesperada |
| Limite de envio, cooldown e reenvio | `suppressesRepeatedDeliveryWithoutRevealingAccountsAndInvalidatesPreviousCodes`; chaves de limite são hashes persistidos, resposta 202 sempre genérica |
| Mudança de contato e reativação | `dormantSessionAndPendingCodeNeverReviveAfterContactChangeAndReverification`; versão de acesso impede reviver sessão/código após retornar ao mesmo e-mail ou reativar cliente |
| Decisões de adicionais | `additionalApprovalRequiresCurrentVerifiedContactAndThrottlesResends`: sessão por link, código anterior à mudança rejeitado antes/depois da verificação, retorno ao e-mail original e reativação; novo código permite decisão; cooldown preserva desafio anterior sem outro envio |
| Transferência de veículo | `transferThroughApiRevokesPreviousCustomerAccessAndOldCodes`; transferência bloqueada com OS ativa, efetiva após encerramento e sem acesso do cliente anterior à nova OS |
| Link antigo e sessão revogada | `reissueInvalidatesOldLinkAndSessionAndClosureStopsSharing`; `rotatesSessionForCodeAndLinkAndInvalidLinkClearsOnlyOperationalGrant` |
| Auditoria persistida | `cadastro_auditoria` registra emissão/revogação do link com oficina, proprietário, OS e ação; demais módulos mantêm trilhas de cadastro, status, previsão, vistoria, adicionais, decisão e encerramento nas suítes existentes |
| Concorrência | `concurrentIssuanceLeavesOnlyOneLiveLink` e suítes `ServiceOrderIntegrationTest`, `AdditionalDecisionIntegrationTest`, `OrderClosureIntegrationTest` |
| Logs e respostas | `deliveryFailureRollsBackChallengeAndNeverLogsProviderSecrets` captura a saída e verifica ausência do segredo sintético do provedor e do e-mail; erro transacional desfaz desafio |
| CPF fora das URLs | Pesquisa `/api/clientes/pesquisa` POST + CSRF; GET com q não vazio rejeitado sem refletir termo; teste Angular verifica URL/corpo/header e integração testa CPF formatado |
| HTTPS/configuração | `ProductionSecurityTest`: rejeita origem HTTP, segredo padrão/curto e override de cookie inseguro em prod; origem não aceita credenciais, query, fragmento ou caminho |

## Operação segura em produção

Ativar `SPRING_PROFILES_ACTIVE=prod`; fornecer `PUBLIC_URL=https://seu-dominio` e `CODE_SECRET` aleatório e exclusivo de no mínimo 32 caracteres por secret manager/ambiente. Não versionar valores reais. O perfil exige cookie Secure e SMTP autenticado com STARTTLS obrigatório.

Terminar TLS em proxy de confiança, redirecionar HTTP para HTTPS no ingress, manter API, PostgreSQL e armazenamento em rede privada e restringir o acesso direto às portas internas. Configurar endereço do proxy explicitamente; não confiar em `X-Forwarded-*` de clientes arbitrários. O Compose de desenvolvimento não é configuração de produção.

Não habilitar logs de corpos HTTP, cookies, parâmetros SQL, conteúdo SMTP ou query strings no proxy. Tokens novos seguem em fragmento e são trocados por POST; a interface remove também parâmetros legados ao abrir. Não registrar URL completa de links legados no ingress. Logs operacionais devem usar códigos de evento/IDs internos e retenção controlada.

Não houve deploy nem teste de TLS em um domínio de produção: os testes validam as restrições de configuração da aplicação. Validar certificado, redirecionamento, HSTS e ausência de exposição direta no ambiente real antes de liberar usuários.

## Resultados

- API: 130 testes, zero falhas/erros/pulados, PostgreSQL 17.11 via Docker (`mvn test`).
- Angular: 99 testes aprovados (`npm test -- --watch=false --browsers=ChromeHeadless`).
- Playwright: 34 testes aprovados, desktop Chromium e Pixel 7 emulado; portal/compartilhamento/encerramento/notificações/avaliações com API simulada. `PLAYWRIGHT_BASE_URL=http://localhost:14219` preserva o Compose existente.
- Build de produção Angular/SSR aprovado. Dois avisos CSS preexistentes: portal 7,35 kB e OS 7,00 kB ante orçamento de 6 kB.
- Empacotamento do JAR aprovado (`mvn package -DskipTests`, após a suíte completa). Regressão focada dos adicionais repetida após ampliar os cenários de contato: 1/1 aprovada.
- `git diff --check` sem erros de whitespace.
- Smoke de leitura do Compose existente: frontend `4200` e API `8080/api/auth/csrf` retornam HTTP 200. Não representa teste da nova versão nesse Compose; a tarefa 20 construirá imagens novas em ambiente isolado.
- Revisão independente APROVADA: dois P1 de adicionais identificados e corrigidos, sem bloqueadores remanescentes. Relatório `19_task_review.md`. Publicação registrada pelo histórico Git e pelo card 19.
