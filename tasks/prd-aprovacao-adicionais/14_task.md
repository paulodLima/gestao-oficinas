# Tarefa 14.0: Aprovação e recusa de adicionais pelo cliente

<critical>Ler os arquivos de prd.md e techspec.md desta pasta, se você não ler esses arquivos sua tarefa será invalidada</critical>

## Visão Geral

Entregar a decisão autenticada por código sobre itens independentes e grupos indivisíveis de uma versão enviada, com idempotência, auditoria e interface móvel no portal.

<skills>
### Conformidade com Skills Padrões

- `java-springboot`
- `frontend-design`
- `task-review`
</skills>

<requirements>

- Exibir versão, total adicional, prazo, itens e decisões existentes antes da confirmação.
- Exigir código temporário enviado ao e-mail verificado, inclusive em sessão originada por link.
- Permitir decisão por item independente ou grupo dependente indivisível.
- Registrar decisão, identidade, horário e versão exata em transação auditável e idempotente.
- Recalcular estado agregado e permitir execução somente de item aprovado na versão vigente.
- Bloquear versão substituída, OS encerrada, código inválido/reutilizado e acesso fora do vínculo.
- Não antecipar notificações gerais da Tarefa 15.
</requirements>

## Subtarefas

- [x] 14.1 Criar persistência e modelos de desafios, operações e decisões por item.
- [x] 14.2 Implementar consulta pública e blocos para itens independentes/grupos indivisíveis.
- [x] 14.3 Implementar emissão de código e confirmação idempotente/auditável.
- [x] 14.4 Recalcular estado e bloquear execução de pendentes/recusados/versões antigas.
- [x] 14.5 Criar interface móvel de revisão, seleção, código e confirmação.
- [x] 14.6 Criar testes unitários, integração e E2E para estados, concorrência e autorização.

## Detalhes de Implementação

Seguir `techspec.md`, especialmente modelos de dados, endpoints, bloqueio transacional e estratégia de testes.

## Critérios de Sucesso

- Repetição idêntica não duplica decisões e payload diferente com a mesma chave é rejeitado.
- Nenhum item de grupo é decidido isoladamente.
- Estado parcial/completo e total aprovado são consistentes.
- Link de acompanhamento sozinho nunca confirma decisão sem código.
- Conteúdo substituído e decisões anteriores permanecem no histórico.
- Interface utilizável em desktop e largura de 320 px.

## Testes da Tarefa

- [x] Testes de unidade
- [x] Testes de integração criados e compilados; execução local tentou iniciar, mas o Docker Desktop estava indisponível.
- [x] Testes E2E

<critical>SEMPRE CRIE E EXECUTE OS TESTES DA TAREFA ANTES DE CONSIDERÁ-LA FINALIZADA</critical>

## Arquivos relevantes

- `oficinas-api/src/main/java/br/com/gestao/oficinas_api/adicional/`
- `oficinas-api/src/main/java/br/com/gestao/oficinas_api/portal/`
- `oficinas-api/src/main/resources/db/migration/`
- `oficinas-app/src/app/portal/`
