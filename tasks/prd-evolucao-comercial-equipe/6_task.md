# Tarefa 6.0 (E06): Concluir etapa com fotos e publicação confirmada

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Salvar evidências da etapa terminada e avançar somente após confirmação íntegra do servidor.

Status: pendente. Prioridade: P0. ID estável: E06 (não altera IDs 01–20 do MVP).
Dependências: E04, E05.
Requisitos: RF06, RF08.
Trello: [Abrir card](https://trello.com/c/l2uOae8E).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF06, RF08 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 6.1 Construir formulário com câmera/galeria, miniaturas, remoção antes do envio, observação e próxima etapa sugerida.
- [ ] 6.2 Exibir claramente etapa concluída, etapa seguinte, quantidade e destino das fotos; não publicar na seleção.
- [ ] 6.3 Preparar upload privado e validar formato/conteúdo/tamanho/pertencimento; exigir fotos novas do lote vinculadas à passagem atual quando obrigatório.
- [ ] 6.4 Finalizar com expectedVersion e chave idempotente, associando evidências à etapa anterior e avançando com auditoria/outbox na transação.
- [ ] 6.5 Sem permissão pública, salvar evidências internas para liberação posterior; sem foto exigida, não mudar a etapa.
- [ ] 6.6 Respeitar autorização de trabalho adicional, OS encerrada, espera e retorno; não permitir funcionário aprovar pelo cliente.
- [ ] 6.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e06.md.

## Detalhes de Implementação

Consultar techspec.md: Interfaces: transação de conclusão; Endpoints; Modelos. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Falha de qualquer foto obrigatória impede avanço; arquivos já preparados não ficam públicos por acidente.
- Cliente vê apenas o conteúdo explicitamente liberado na etapa correta.
- Confirmação exibe sucesso só após persistência; etapa atual/próxima são inequívocas.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Quantidade mínima, visibilidade, botão salvar, próxima etapa e lote válido.
- [ ] Integração: Commit/rollback, foto de outra OS, arquivo incompleto, chave repetida, autorização de adicional.
- [ ] E2E/validação operacional: Capturar 2 fotos → prévia → confirmar → avançar → conferir fotos na etapa concluída pelo cliente.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

ordem/ServicePhoto*; ServiceOrderService.java; equipe/ConclusaoEtapaService (novo); Angular equipe/ordem.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

