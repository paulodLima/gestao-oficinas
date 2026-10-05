# Tarefa 9.0 (E09): Consolidar orçamento autorizado e resumo de entrega

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Apresentar valor inicial e adicionais aceitos com histórico confiável, sem simular recebimento.

Status: pendente. Prioridade: P1. ID estável: E09 (não altera IDs 01–20 do MVP).
Dependências: E01, E03.
Requisitos: RF09.
Trello: [Abrir card](https://trello.com/c/EkHuZQ6w).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF09 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 9.1 Inventariar adicionais/aprovação/resumo existentes e reaproveitar versionamento/decimais e concessões restritas.
- [ ] 9.2 Adicionar orçamento inicial com itens, quantidades, valores e versões congeladas após envio; confirmação autenticada sem poderes ao técnico.
- [ ] 9.3 Calcular total autorizado pela soma do inicial aceito e adicionais efetivamente aprovados; evitar repetir versões substituídas/grupos.
- [ ] 9.4 Exibir pendente, recusado, parcialmente aprovado e autorizado na empresa e no cliente.
- [ ] 9.5 Ampliar resumo de entrega com trabalhos, orientações e fotos selecionadas, usando acesso específico revogável e validade explícita.
- [ ] 9.6 Preservar política de encerramento/retorno e manter avaliação oculta; não criar contas a receber ou emissão fiscal.
- [ ] 9.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e09.md.

## Detalhes de Implementação

Consultar techspec.md: Modelos orçamento; Valores e autorização; Integrações. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Total é rastreável a versões/decisões; arredondamento consistente; nenhum aprovado contado duas vezes.
- Técnico não consulta valores nem aprova em nome do cliente.
- Resumo não reativa acesso operacional encerrado nem mostra informações internas.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Decimal/arredondamento, aprovação parcial, substituição e total consolidado.
- [ ] Integração: Versão concorrente, dupla decisão, encerramento simultâneo e permissão do resumo/fotos.
- [ ] E2E/validação operacional: Orçamento inicial → aceite → adicional parcial → total → entrega/resumo restrito.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

adicional; portal/AdditionalDecision*; ordem/encerramento; avaliacao/resumo; Angular ordem/portal.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

