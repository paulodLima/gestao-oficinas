# Tarefa 3.0 (E03): Aplicar permissões e atribuir responsáveis às OS

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Permitir delegar serviços com privilégio mínimo e configurar alcance de atuação.

Status: pendente. Prioridade: P0. ID estável: E03 (não altera IDs 01–20 do MVP).
Dependências: E02.
Requisitos: RF03.
Trello: [Abrir card](https://trello.com/c/ELD6iTk8).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF03 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 3.1 Documentar matriz proprietário/atendimento/técnico por ação: consultar, atualizar, publicar, atribuir, encerrar, cancelar e acessar valores.
- [ ] 3.2 Implementar padrão ATRIBUIDAS e opção TODAS da mesma oficina por funcionário, editáveis só pelo proprietário.
- [ ] 3.3 Criar atribuição/reatribuição do responsável técnico com versão esperada; manter histórico e impedir vínculo de outra oficina/inativo.
- [ ] 3.4 Aplicar política central em busca, detalhe, foto, download, update e idempotência; não confiar no filtro do frontend.
- [ ] 3.5 Separar permissão de publicação: padrão negada para técnico até habilitação explícita; dono/atendimento libera evidências internas.
- [ ] 3.6 Revalidar bloqueio, função e atribuição em sessão ativa e na confirmação da operação.
- [ ] 3.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e03.md.

## Detalhes de Implementação

Consultar techspec.md: Política de acesso; Interfaces; Modelos. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Técnico não vê CPF, contatos completos, valores, configurações nem OS fora do alcance.
- TODAS nunca significa todas as oficinas; busca não revela existência de recurso não autorizado.
- Troca de atribuição/permissão passa a valer nas operações seguintes e não apaga autoria anterior.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Tabela de decisões de política para todos os papéis, alcances e publicação.
- [ ] Integração: IDs cruzados em todos os endpoints/fotos; reatribuição/bloqueio concorrente com escrita.
- [ ] E2E/validação operacional: Dois técnicos com OS diferentes; dono amplia/restringe alcance e UI/API refletem a alteração.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

equipe e política nova; ordem/ServiceOrderController.java; ServicePhotoController.java; identidade; tela administrativa equipe.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

