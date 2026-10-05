# Tarefa 14.0 (E14): Entregar onboarding, importação e operação de assinaturas

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Permitir ativação assistida e administração comercial sem depender de alterações manuais no banco.

Status: pendente. Prioridade: P1. ID estável: E14 (não altera IDs 01–20 do MVP).
Dependências: E03, E12, E13.
Requisitos: RF14.
Trello: [Abrir card](https://trello.com/c/0rBkMTQH).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF14 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 14.1 E14.1a Entregar onboarding: identidade da oficina, equipe, fluxo, primeira OS e compartilhamento; checklist persistente e retomável.
- [ ] 14.2 E14.1b Entregar importação CSV com template, prévia, normalização, duplicidade, erros por linha, limite e rollback/reprocessamento idempotente.
- [ ] 14.3 E14.1c Entregar planos/cotas e estados de teste/ativo/inadimplente/cancelado com regras explícitas; downgrade não apaga dados.
- [ ] 14.4 E14.1d Integrar adaptador de cobrança em sandbox: webhooks autenticados, idempotência, eventos fora de ordem, conciliação e portal de cancelamento.
- [ ] 14.5 E14.1e Criar console SaaS segregado com mínimo privilégio, MFA para operador de produção, auditoria e sem impersonação invisível.
- [ ] 14.6 E14.1f Exportar dados/fotos autorizados e definir retenção pós-cancelamento antes de cobrar; fornecedor/preços/contratos finais requerem decisão explícita de ativação.
- [ ] 14.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e14.md.

## Detalhes de Implementação

Consultar techspec.md: Componentes SaaS; Modelos; Endpoints; Integrações. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Cada subentrega tem demonstração e testes próprios; nenhuma cobrança real ocorre em teste.
- Importação não mistura oficinas nem duplica lote ao repetir; registros rejeitados são explicados.
- Assinatura/cota vale no backend; inadimplência não apaga histórico automaticamente; admin não ganha acesso irrestrito.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Validação de CSV/duplicidade, cotas, máquina de estados e idempotência de webhook.
- [ ] Integração: Sandbox/fake de cobrança, replay/ordem de eventos, jobs de importação e exportação isolada.
- [ ] E2E/validação operacional: Nova oficina → onboarding/importação → primeira OS → teste/plano → cancelamento/exportação.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

novos comercial/importacao/admin; Angular onboarding/assinatura; cadastro; políticas de cota; documentação comercial.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

