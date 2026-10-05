# Tarefa 10.0 (E10): Criar indicadores históricos e painel de gargalos

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Transformar registros operacionais em decisões com métricas definidas e dados reais.

Status: pendente. Prioridade: P1. ID estável: E10 (não altera IDs 01–20 do MVP).
Dependências: E05, E08, E09.
Requisitos: RF10.
Trello: [Abrir card](https://trello.com/c/pjFEIPpo).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF10 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 10.1 Documentar definição e período de cada métrica antes da consulta: entradas, entregas, tempo por passagem, espera de aprovação e pontualidade.
- [ ] 10.2 Separar OS sem atualização pública de última alteração interna; permitir limiar configurável.
- [ ] 10.3 Definir pontualidade contra previsão vigente e exibir quantidade de revisões; evitar indicador inflado por reprogramação silenciosa.
- [ ] 10.4 Implementar agregações por oficina/período e informar cobertura histórica, denominadores e estado sem dados.
- [ ] 10.5 Criar gráficos acessíveis com tabela alternativa e links para listas filtradas de OS.
- [ ] 10.6 Validar índices e plano de consulta com massa sintética; distinguir valor autorizado de receita recebida.
- [ ] 10.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e10.md.

## Detalhes de Implementação

Consultar techspec.md: Monitoramento; Modelos; Endpoints painel. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Totais reconciliam com massa conhecida e listagem detalhada.
- Retornos/esperas não distorcem tempo médio; registros incompletos são identificados.
- Nenhum gráfico usa dados fictícios em produção ou mistura oficinas.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Fórmulas, limites do período, offset, denominadores zero e incompletude.
- [ ] Integração: Agregados SQL vs eventos conhecidos, índices/paginação e isolamento.
- [ ] E2E/validação operacional: Selecionar período → conferir gráfico/tabela → abrir lista correspondente no desktop/mobile.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

API painel; ordem/eventos/previsão; Angular painel/dashboard*; testes de métricas.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

