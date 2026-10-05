# Tarefa 15.0 (E15): Executar piloto comercial e validar lançamento

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Validar uso, valor percebido, custo e disposição de pagamento antes de expandir.

Status: pendente. Prioridade: P1. ID estável: E15 (não altera IDs 01–20 do MVP).
Dependências: E08, E09, E10, E11, E12, E13, E14.
Requisitos: RF15.
Trello: [Abrir card](https://trello.com/c/Tv8O1INQ).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF15 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 15.1 Preparar demonstração com dados sintéticos, roteiro de instalação/onboarding e FAQ de suporte; posicionar acompanhamento sem substituir ERP completo.
- [ ] 15.2 Definir cinco oficinas candidatas e obter adesão/autorização separada antes de contato, convite, importação ou cobrança real.
- [ ] 15.3 Instrumentar métricas mínimas agregadas: primeira OS compartilhada, atualização semanal, uso do portal, tempo de aprovação, suporte e custo por oficina.
- [ ] 15.4 Conduzir quatro semanas de piloto autorizado; medir atualização simples com meta de 30s excluindo upload e dificuldades no celular.
- [ ] 15.5 Testar disposição a pagar R$99/R$149 como hipóteses, sem anunciar preço definitivo; contabilizar armazenamento, e-mail, suporte e aquisição.
- [ ] 15.6 Documentar decisão seguir/ajustar/interromper com dados: meta proposta 4/5 ativadas e 3/5 ativas ao final; falhas de segurança ou recuperação impedem lançamento.
- [ ] 15.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e15.md.

## Detalhes de Implementação

Consultar techspec.md: Observabilidade; Sequenciamento; Dependências. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Relatório diferencia entrevistas, resultados medidos e hipóteses; cadastro/receita fictícia não conta como venda.
- Plano de suporte e custo por oficina documentados; feedback vira backlog priorizado sem customização ilimitada.
- Fora de escopo (fiscal/estoque/app nativo/WhatsApp automático) não é prometido na venda.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Eventos/métricas de ativação, denominadores e cálculos de custo sem PII.
- [ ] Integração: Coleta deduplicada de métricas reconciliada com OS, sem mistura de oficinas.
- [ ] E2E/validação operacional: Ensaio de onboarding/demonstração; piloto real somente após autorização e gates de segurança.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

novos docs/piloto-comercial.md e relatório; telemetria agregada; onboarding; materiais comerciais.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

