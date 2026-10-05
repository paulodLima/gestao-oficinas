# Tarefa 8.0 (E08): Atualizar portal com progresso real e comunicação clara

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Mostrar ao cliente o fluxo específico da OS e sua evolução, sem progresso fictício.

Status: pendente. Prioridade: P1. ID estável: E08 (não altera IDs 01–20 do MVP).
Dependências: E05, E07.
Requisitos: RF08.
Trello: [Abrir card](https://trello.com/c/r218P6eN).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF08 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 8.1 Adaptar DTO público e linha horizontal existente ao snapshot e passagens reais.
- [ ] 8.2 Mostrar concluídas, atual, previstas e puladas; contar restantes até pronto, separando retirada/entrega.
- [ ] 8.3 Preservar retrabalho no histórico sem duplicar etapas no resumo; informar quando o legado não possui evidência histórica completa.
- [ ] 8.4 Agrupar fotos por passagem/etapa e manter galeria cronológica, legendas, zoom e carregamento paginado.
- [ ] 8.5 Exibir previsão como estimativa, motivo público de atraso e próxima ação; não converter contagem em dias ou percentual de trabalho.
- [ ] 8.6 Manter animação com movimento reduzido e sem barra vertical interna; respeitar revogação e conteúdo privado.
- [ ] 8.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e08.md.

## Detalhes de Implementação

Consultar techspec.md: Portal/projeções; Modelos de passagem; Decisões principais. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Não marcar etapas puladas como concluídas nem inferir datas inexistentes.
- Cliente vê fotos antigas liberadas e novas da etapa correta; dados internos continuam ausentes.
- Sem OS ativa, link expirado e falta de previsão têm mensagens úteis.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Contagem, retrabalho, legado, ordenação/filtros e acessibilidade da animação.
- [ ] Integração: Projeção pública sem observações privadas, funcionário/cliente/arquivo de outra oficina e revogação.
- [ ] E2E/validação operacional: Evolução, espera, salto e retorno refletidos no portal em 320px sem overflow indevido.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

oficinas-app/src/app/portal/portal-access.component.ts; API portal/ordem; fotos/eventos.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

