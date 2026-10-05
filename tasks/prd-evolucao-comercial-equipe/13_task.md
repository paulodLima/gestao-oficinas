# Tarefa 13.0 (E13): Preparar produção, segurança e monitoramento

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Definir e validar uma operação recuperável, segura e observável antes de ampliar vendas.

Status: pendente. Prioridade: P0. ID estável: E13 (não altera IDs 01–20 do MVP).
Dependências: E03, E07, E11, E12.
Requisitos: RF13.
Trello: [Abrir card](https://trello.com/c/4DclLIDs).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF13 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 13.1 Criar configuração de produção separada do Compose local: HTTPS, cookies seguros, proxy confiável, portas mínimas, sem senhas default/Mailpit exposto.
- [ ] 13.2 Inventariar segredos e documentar rotação; não copiar credenciais em documentos, logs ou cards.
- [ ] 13.3 Executar matriz de autorização em endpoints/arquivos/exportações, dependências vulneráveis, CSRF, limites e sessão revogada.
- [ ] 13.4 Instrumentar latência, erros, fila, uploads, disco/cota e backups; alertas com responsável e runbook.
- [ ] 13.5 Testar carga de referência proposta 50 oficinas/100 mil OS/25 usuários; registrar hardware, resultados e gargalos, meta inicial p95 busca <1s.
- [ ] 13.6 Documentar retenção, acesso de suporte, exportação, incidentes, restauração e checklist de liberação; não contratar infraestrutura nesta task sem autorização.
- [ ] 13.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e13.md.

## Detalhes de Implementação

Consultar techspec.md: Monitoramento; Testes; Dependências técnicas. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Sem falhas críticas de isolamento ou perda de dados; pendências de produção explicitadas.
- Alertas são exercitados; logs não contêm CPF/token/senha ou payload privado.
- Capacidade e disponibilidade não são prometidas além do que foi medido.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Redação de logs, validação de configuração segura e limites.
- [ ] Integração: Matriz de duas oficinas e sessões, headers/proxy, alertas e migração em staging.
- [ ] E2E/validação operacional: Smoke HTTPS e fluxo principal em staging; teste de carga e recuperação documentados.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

identidade/SecurityConfig.java; application*.yaml; Docker/proxy; testes segurança; runbooks produção.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

