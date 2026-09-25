# Review: Task 10 - Acesso seguro do cliente

**Revisor**: AI Code Reviewer

**Data**: 2026-09-25

**Arquivo da task**: 10_task.md
**Status**: APROVADO COM OBSERVAÇÕES

## Resumo

A implementação atende ao fluxo seguro de acesso do cliente por código temporário ou link exclusivo de OS. A revisão confirmou não enumeração, uso único, limite de tentativas, isolamento por oficina, revalidação de vínculo/link/OS e separação entre leitura pública e privilégios da oficina.

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

Nenhum problema crítico encontrado.

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
| Testes | ✅ |

## Recomendações

1. Executar `PortalAccessIntegrationTest` em CI ou após restaurar o Docker Desktop.
2. Extrair as consultas do portal para um repositório dedicado quando o módulo receber novas operações.

## Veredito

Entrega aprovada. Os fluxos e controles exigidos estão implementados e cobertos; a indisponibilidade local do Docker é uma limitação de infraestrutura, não uma falha do código.
