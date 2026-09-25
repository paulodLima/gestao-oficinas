# Especificação Técnica — Aprovação e recusa de serviços adicionais

## Resumo Executivo

A solução estende o módulo de adicionais e o portal com uma autorização transacional de duas etapas: emissão de desafio vinculado à versão e confirmação atômica das decisões. A persistência usa bloqueio pessimista da solicitação vigente, restrições únicas e chave idempotente para que concorrência ou clique repetido não dupliquem efeitos.

O código temporário segue o modelo HMAC já existente, com expiração de 10 minutos, cinco tentativas e uso único. A decisão é gravada por operação e por item, embora grupos dependentes sejam validados e confirmados como bloco indivisível.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `AdditionalDecisionController`: endpoints públicos de consulta, emissão do código e confirmação.
- `AdditionalDecisionService`: autorização, formação de blocos, validação do desafio, idempotência e transição agregada.
- `AdditionalDecisionRepository`: consultas públicas e gravação transacional de desafios, lotes e decisões.
- `AdditionalDecisionPolicy`: validade do código, grupos completos, transições e cálculo do total aprovado.
- `AdditionalDecisionComponent`: revisão móvel, seleção, resumo, código e confirmação no portal.
- `V13__decisoes_adicionais.sql`: desafios, operações idempotentes e decisões por item.

## Design de Implementação

### Interfaces Principais

```java
DecisionRequest issueChallenge(PortalGrant grant, UUID orderId, UUID requestId, long version);
DecisionResult confirm(PortalGrant grant, UUID orderId, UUID requestId, Confirmation input);
List<PublicAdditional> list(PortalGrant grant, UUID orderId);
```

`PortalGrant` deve ser extraído para um serviço reutilizável ou exposto pelo módulo de portal sem conceder privilégios de proprietário. Para sessões por link, o cliente responsável é resolvido pela OS e revalidado antes da emissão e confirmação.

### Modelos de Dados

- `adicional_desafio`: oficina, cliente, OS, solicitação, versão, hash, expiração, tentativas e uso.
- `adicional_decisao_operacao`: solicitação, versão, cliente, chave idempotente, hash do payload, comentário e horário.
- `adicional_item_decisao`: operação, versão, item, decisão `APROVADO`/`RECUSADO`, identidade e horário.
- Restrição única `(versao_id, item_id)` impede decisão duplicada; `(solicitacao_id, idempotency_key)` protege repetição da requisição.

O DTO público inclui blocos de decisão. Um bloco tem chave estável (`item:{id}` ou `grupo:{nome}`), itens, total e decisão agregada. Itens aprovados na versão vigente recebem `execucaoPermitida=true`; pendentes e recusados recebem `false`.

### Endpoints de API

- `GET /api/portal/ordens-servico/{orderId}/adicionais`: lista versões enviadas autorizadas e decisões.
- `POST /api/portal/ordens-servico/{orderId}/adicionais/{requestId}/codigo`: recebe `expectedVersion`; retorna `202` com desafio e expiração.
- `POST /api/portal/ordens-servico/{orderId}/adicionais/{requestId}/decisoes`: exige `Idempotency-Key`, desafio, código, versão, decisões por bloco e comentário opcional.

A confirmação retorna `200` com a solicitação atualizada. Código inválido retorna erro genérico; versão/estado incompatível retorna `409`; recurso fora do vínculo retorna `404`.

## Pontos de Integração

- `TransactionalEmail` envia somente o código de autorização ao e-mail verificado.
- A sessão do portal e o vínculo atual do cliente são revalidados em cada chamada.
- A decisão não depende de o e-mail de notificação geral estar implementado.

## Abordagem de Testes

### Testes Unidade

- Formação e indivisibilidade de grupos.
- Estado agregado sem decisão, parcial e completo.
- Total aprovado e bloqueio de execução.
- Código expirado, reutilizado e com tentativas excedidas.

### Testes de Integração

- Decisão independente e por grupo.
- Clique duplo com a mesma chave e conflito com payload diferente.
- Concorrência com duas chaves sobre o mesmo item.
- Sessão por código e por link, isolamento entre clientes/oficinas e versão substituída.
- Auditoria e preservação das decisões antigas.

### Testes de E2E

- Playwright em desktop e 320 px: revisar, selecionar, solicitar código, confirmar e visualizar decisão parcial/completa.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

1. Migração e modelos, base para integridade concorrente.
2. Política, repositório e serviço transacional.
3. Endpoints públicos e integração com o portal.
4. Interface de decisão e estados responsivos.
5. Testes, documentação e revisão.

### Dependências Técnicas

- Tarefas 10 e 13 concluídas.
- PostgreSQL para os testes Testcontainers.
- Serviço SMTP configurado em produção; testes usam mock do `TransactionalEmail`.

## Monitoramento e Observabilidade

- Auditoria para código solicitado, decisão confirmada e tentativa conflitante.
- Não registrar código, token, e-mail completo ou comentário em logs.
- Métricas de negócio podem ser derivadas dos estados e operações; exposição Prometheus fica para a infraestrutura futura.

## Considerações Técnicas

### Decisões Principais

- `SELECT ... FOR UPDATE` serializa decisões da mesma solicitação; restrições únicas mantêm a garantia no banco.
- HMAC e comparação constante seguem a política de acesso existente; o desafio é vinculado aos dados concretos da transação para reduzir troca de contexto.
- A chave idempotente reapresenta o resultado apenas quando o hash do payload coincide.
- A versão substituída continua consultável, porém não aceita novas decisões.

Referências: [OWASP Transaction Authorization](https://cheatsheetseries.owasp.org/cheatsheets/Transaction_Authorization_Cheat_Sheet.html), [RFC 4226](https://www.rfc-editor.org/rfc/rfc4226.html) e [PostgreSQL Explicit Locking](https://www.postgresql.org/docs/17/explicit-locking.html).

### Riscos Conhecidos

- Falha síncrona de SMTP impede a emissão do desafio; a Tarefa 15 poderá adotar outbox e retentativas.
- Grupos são identificados por texto na versão congelada; a validação exige todos os itens do grupo para evitar decisões parciais.
- A concorrência com encerramento será reforçada na Tarefa 17, mantendo já nesta entrega a verificação de OS ativa dentro da transação.

### Conformidade com Skills Padrões

- `java-springboot`: serviço transacional, injeção por construtor e testes JUnit/Mockito/Testcontainers.
- `frontend-design`: fluxo móvel coerente com o portal existente e acessível.
- `task-review`: revisão de padrões, compilação e testes antes do merge.

### Arquivos relevantes e dependentes

- `oficinas-api/.../portal/PortalAccessController.java`
- `oficinas-api/.../adicional/AdditionalRequest*.java`
- `oficinas-api/src/main/resources/db/migration/V12__solicitacoes_adicionais.sql`
- `oficinas-app/src/app/portal/portal-access.component.ts`
