# Review: Task 10 - Acesso seguro do cliente

**Revisor**: AI Code Reviewer

**Data**: 2026-09-25

**Arquivo da task**: 10_task.md
**Status**: APROVADO COM OBSERVAÇÕES

## Resumo

A implementação entrega o fluxo de acesso por código temporário ou link exclusivo de OS.
Na reconferência de 25/09/2026, foi identificado e corrigido um defeito que a revisão anterior
não detectou: o erro de código inválido revertia o incremento de tentativas. O teste com
o interceptor transacional real do Spring reproduziu o rollback indevido antes da correção
e confirmou o commit da tentativa depois dela. Falhas de infraestrutura continuam provocando rollback.

## Arquivos Revisados

| Arquivo | Status | Problemas |
|---------|--------|-----------|
| `PortalAccessController.java` | ✅ OK | 0 funcionais |
| `PortalAccessPolicy.java` | ✅ OK | 0 |
| `ServiceOrderAccessController.java` | ✅ OK | 0 |
| `PortalAccessIntegrationTest.java` | ✅ OK | 0 |
| `PortalAccessPolicyTest.java` | ✅ OK | 0 |
| `portal-access.component.ts` | ✅ OK | 0 |
| `portal-access.component.spec.ts` | ✅ OK | 0 |
| `service-order-page.component.*` | ✅ OK | 0 |
| `service-order.service.ts` | ✅ OK | 0 |

## Problemas Encontrados

### 🔴 Problemas Críticos

Corrigido: `PortalAccessController.validateCode` usava o rollback padrão para `ApiException`,
desfazendo o contador de tentativas inválidas. A exceção de negócio agora preserva a tentativa.
`PortalAccessTransactionTest` cobre commit no erro de código e rollback no erro de persistência.

### 🟡 Problemas Major

Nenhum problema major encontrado.

### 🟢 Problemas Minor

- `PortalAccessController.java` concentra consultas e regras em uma classe extensa. Uma extração futura para serviço e repositório reduziria o custo de manutenção sem alterar o comportamento aprovado.
- A suíte Testcontainers foi compilada, mas não executada nesta estação porque o Docker Desktop não oferece um ambiente válido e o serviço não pôde ser iniciado.

## ✅ Destaques Positivos

- Comparação constante do HMAC do código e tokens de link armazenados somente como SHA-256.
- Resposta 202 uniforme para placa existente ou desconhecida.
- Revogação automática do link anterior na emissão de um novo.
- Revalidação do cliente, vínculo, expiração, revogação e OS ativa em cada leitura protegida.
- Sessão do portal não recebe autenticação nem privilégios de proprietário.
- Interface móvel acessível, com foco visível, estados de carregamento, CSRF e seleção restrita de veículos.

## Conformidade com Padrões

| Padrão | Status |
|--------|--------|
| Padrões de Código | ⚠️ |
| TypeScript/Node.js | ✅ |
| REST/HTTP | ✅ |
| Logging | ✅ |
| React | N/A |
| Testes | ⚠️ Integração PostgreSQL não executada; testes direcionados aprovados |

## Recomendações

1. Executar `PortalAccessIntegrationTest` em CI ou após restaurar o Docker Desktop.
2. Extrair as consultas do portal para um repositório dedicado quando o módulo receber novas operações.

## Veredito

Correção aprovada nos testes executados: Java 4/4 (política e transação), Angular 5/5
(acesso, seleção de veículo, galeria e sessão). O teste HTTP/PostgreSQL foi reforçado para
verificar cada incremento e garantir que o código incorreto nunca coincida com o sorteado.
Ele compilou, mas sua execução continua pendente porque o Docker local está parado.
Essa limitação não demonstra ausência de outros defeitos de integração.

Referência: [Spring — regras de rollback](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html).
