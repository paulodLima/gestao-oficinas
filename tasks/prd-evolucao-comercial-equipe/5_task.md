# Tarefa 5.0 (E05): Configurar modelos de fluxo e regras de evidência

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Adaptar as etapas ao serviço sem adulterar o histórico nem exigir fotos sem sentido.

Status: pendente. Prioridade: P0. ID estável: E05 (não altera IDs 01–20 do MVP).
Dependências: E01, E03.
Requisitos: RF05, RF06.
Trello: [Abrir card](https://trello.com/c/hJ6n7PFK).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF05, RF06 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 5.1 Criar modelos iniciais de mecânica, funilaria/pintura e rápido com execução, espera e pronto distintos.
- [ ] 5.2 Permitir ao dono ordenar etapas, marcar opcionais e exigir quantidade mínima de fotos por conclusão; começar com mínimo 1 nas etapas configuradas.
- [ ] 5.3 Instanciar snapshot do fluxo ao abrir OS e definir migração explícita para OS legada; não inferir conclusões a partir da posição atual.
- [ ] 5.4 Preservar snapshot em edições do modelo; mudança de fluxo da OS exige motivo, versão e manutenção de eventos antigos.
- [ ] 5.5 Modelar passagem por etapa para retorno/retrabalho, estado pulado com motivo e próximo passo permitido.
- [ ] 5.6 Aplicar política em todas as interfaces, inclusive atualização legada do dono; qualquer exceção administrativa exige autorização específica, motivo e auditoria.
- [ ] 5.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e05.md.

## Detalhes de Implementação

Consultar techspec.md: Modelos de fluxo/passagem; Decisões principais. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Editar modelo não altera OS em andamento silenciosamente.
- Espera exige motivo; pronto/entregue não são confundidos; retorno gera nova passagem.
- API rejeita conclusão que não atende política, mesmo por rota legada.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Próxima etapa, opcionais, retornos, espera, política de fotos e contagem até pronto.
- [ ] Integração: Snapshot/versionamento, mudança concorrente, migração de legado e tentativa de bypass.
- [ ] E2E/validação operacional: Dono configura modelo, abre OS e confirma que tela técnica/portal usam o fluxo escolhido.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

ordem/ServiceOrderStatus.java; eventos/serviço de OS; novo fluxo; migrações; Angular ordem/oficina.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

