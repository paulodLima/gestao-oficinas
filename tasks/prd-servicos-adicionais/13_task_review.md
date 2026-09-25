# Review: Task 13 - Solicitar e versionar serviços adicionais

**Revisor**: AI Code Reviewer

**Data**: 2026-09-25

**Arquivo da task**: 13_task.md
**Status**: APROVADO COM OBSERVAÇÕES

## Resumo

A implementação atende ao fluxo de preparação da oficina: rascunho com itens e fotos, cálculo decimal, grupos dependentes, envio imutável, substituição versionada, cancelamento e histórico. A decisão do cliente permanece corretamente fora do escopo, reservada para a tarefa 14.

## Arquivos Revisados

| Área | Status | Problemas |
|------|--------|-----------|
| Migração e modelo de adicionais | ✅ OK | 0 funcionais |
| Serviço, repositório e API REST | ✅ OK | 0 funcionais |
| Interface Angular da oficina | ✅ OK | 0 funcionais |
| Testes Java e Angular | ✅ OK | 0 funcionais |
| Documentação da tarefa | ✅ OK | 0 |

## Problemas Encontrados

### 🔴 Problemas Críticos

Nenhum problema crítico encontrado.

### 🟡 Problemas Major

Nenhum problema major encontrado.

### 🟢 Problemas Minor

- O teste de integração com PostgreSQL foi compilado, mas não executado porque o Docker Desktop está indisponível nesta estação.
- O build mantém dois avisos de orçamento CSS preexistentes, sem impedir a geração dos artefatos.

## ✅ Destaques Positivos

- Cálculos monetários são autoritativos no backend com `BigDecimal` e arredondamento explícito.
- Controle otimista por `expectedVersion` protege contra gravações concorrentes.
- Fotos são verificadas por oficina e OS, e precisam estar públicas no momento do envio.
- A versão enviada não é editada; substituições preservam o conteúdo anterior e o motivo.
- A interface aceita vírgula decimal, oferece prévia do total e mantém histórico de versões.
- As mutações relevantes são auditadas na mesma transação.

## Conformidade com Padrões

| Padrão | Status |
|--------|--------|
| Padrões de Código | ✅ |
| TypeScript/Angular | ✅ |
| Java/Spring Boot | ✅ |
| REST/HTTP e segurança | ✅ |
| Testes | ⚠️ |

## Recomendações

1. Executar `AdditionalRequestIntegrationTest` no CI ou quando o Docker estiver disponível.
2. Reutilizar o histórico versionado desta entrega na decisão por item/grupo da tarefa 14.

## Veredito

Entrega aprovada com observação de infraestrutura. Nenhum defeito crítico ou major foi encontrado; build, testes unitários e testes Angular passaram.
