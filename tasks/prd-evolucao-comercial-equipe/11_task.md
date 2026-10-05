# Tarefa 11.0 (E11): Confiabilizar notificações e visibilidade das falhas

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Tornar problemas de entrega visíveis sem desfazer trabalho salvo nem disparar mensagens em duplicidade.

Status: pendente. Prioridade: P0. ID estável: E11 (não altera IDs 01–20 do MVP).
Dependências: E02, E06, E09.
Requisitos: RF11.
Trello: [Abrir card](https://trello.com/c/87e9sqwj).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF11 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 11.1 Inventariar outbox/retries existentes e separar e-mails comerciais, convites e códigos de autenticação.
- [ ] 11.2 Exibir estado aguardando/enviado ao provedor/falha/cancelado e última tentativa com informação segura para dono/atendimento.
- [ ] 11.3 Integrar eventos novos de equipe/orçamento/conclusão sem enviar e-mail para cada foto isolada.
- [ ] 11.4 Reenvio com cooldown, deduplicação e auditoria; tratar estado incerto após SMTP aceitar sem prometer exactly-once.
- [ ] 11.5 Manter nome da oficina e URL contextual nos modelos; validar destinatário e escopo, sem tokens em logs.
- [ ] 11.6 Testar indisponibilidade do provedor, alertar fila antiga e preservar envio manual WhatsApp sem fingir confirmação de entrega.
- [ ] 11.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e11.md.

## Detalhes de Implementação

Consultar techspec.md: Comunicação; Integrações; Observabilidade. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Falha SMTP não reverte conclusão; UI permite diagnóstico e nova tentativa autorizada.
- Sucesso SMTP não é exibido como leitura do cliente.
- Códigos expirados não são enviados tardiamente por reutilização indiscriminada de fila.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Templates, destinatário, deduplicação, expiração e política de retry.
- [ ] Integração: Mailpit/provedor fake, falha/transiente, concorrência de workers e reenvio.
- [ ] E2E/validação operacional: Atualizar OS com provedor indisponível → ver falha → recuperar → reenviar sem repetir evento de negócio.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

notificacoes/NotificationWorker.java; NotificationService.java; TransactionalEmail.java; portal códigos; Angular notificacoes.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

